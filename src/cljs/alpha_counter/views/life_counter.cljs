(ns alpha-counter.views.life-counter
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [alpha-counter.channels :refer [running-total delayed-total trickle]]
            [cljs.core.async :refer [>! <! chan put! mult tap]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;; Time between damage running total upticks; that is, the running total of
;; combo damage will increase by 1 after this many ms. To go from
;; 5 damage to 15 damage would take (* damage-change-speed 10) ms.
(def damage-change-speed 25)

;; As with damage-trickle-speed, but for health alteration. If a player
;; goes from 50 health to 45, it will take (* health-change-speed 5) ms.
(def health-change-speed 50)

;; Time before combo is considered complete, in ms. After the timeout, damage
;; will be applied to the player.
(def combo-timeout 3000)

;; Sets :current to true on the supplied player, false on all others. Can be
;; used as a handler as (partial app player).
(defn- select-player [app player]
  (let [set-current #(assoc % :current (= % @player))]
    (om/transact! app :players #(mapv set-current %))))

;; Returns a cursor for the current player, or for player one.
(defn- get-current-player [app]
  (or (first (filter :current (:players app)))
      (first (:players app))))

;; Returns a cursor for the non-current player, or for player two.
(defn- get-opponent [app]
  (or (first (remove :current (:players app)))
      (second (:players app))))

;; Subtracts n health from the player. If n is negative, this will heal the
;; player, but only up to the maximum health of the player's character.
; NOTE: We implement healing... with a damage function. Food for thought.
(defn- damage [player n]
  (om/transact! player (fn [player]
                         (let [health (:health player)
                               max-health (-> player :character :health)]
                           (assoc player :health (min (- health n)
                                                      max-health))))))

;; Health bar view. Expects props {:player p, :select-player fn}.
(defn- health-view [props owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [player select-player]} props]
        (dom/div (when (:current player) #js {:className "selected"})
          (dom/button #js {:onClick #(select-player player)}
            (-> player :character :name))
          (dom/div #js {:className "health-display"}
            (dom/div #js {:className "damage"} "")
            (dom/div #js {:className "bar"} "")
            (dom/div #js {:className "number"} (:health player))))))))

;; Returns a vec of hashes {:player :opponent :channel :mult}, where the
;; :channel value is the input channel of incoming hits, and the :mult value is
;; a mult which interested channels may tap to receive hit data.
(defn- init-hit-channels [app]
  (let [p1-hits (chan)
        p2-hits (chan)]
    [{:player (-> app :players first)
      :opponent (-> app :players second)
      :channel p1-hits
      :mult (mult p1-hits)}
     {:player (-> app :players second)
      :opponent (-> app :players first)
      :channel p2-hits
      :mult (mult p2-hits)}]))

;; Given a vec of hit channel hashes, returns a vec of running damage total
;; channel hashes {:player :opponent :channel}. Values from these channels will
;; be set as the running total of the current combo.
(defn- init-damage-channels [hit-channels]
  (mapv (fn [{:keys [mult] :as m}]
          (-> m
            (dissoc :mult)
            (assoc :channel (-> (tap mult (chan))
                              (trickle damage-change-speed)
                              (running-total combo-timeout)))))
        hit-channels))

;; Given a vec of hit channel hashes, returns a vec of combo damage total
;; channels. Values from these channels will be applied as damage to the
;; opponent or healing to the player.
(defn- init-total-channels [hit-channels]
  (mapv (fn [{:keys [mult] :as m}]
          (-> m
            (dissoc :mult)
            ; input to this channel will be held until combo-timeout completes;
            ; then the grand total will be trickled as a stream of 1s.
            (assoc :channel (-> (tap mult (chan))
                              (delayed-total combo-timeout)
                              (trickle health-change-speed)))))
        hit-channels))

(defn life-counter-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      (let [hit-channels (init-hit-channels app)
            damage-channels (init-damage-channels hit-channels)
            total-channels (init-total-channels hit-channels)]
        {:hit-channels hit-channels
         :damage-channels damage-channels
         :total-channels total-channels}))
    om/IWillMount
    (will-mount [_]
      (let [damage-channels (om/get-state owner :damage-channels)
            total-channels (om/get-state owner :total-channels)]
        (doseq [{:keys [player opponent channel]} damage-channels]
          ; display running total from damage channel on screen
          (go-loop []
            ; TODO: actually display running combo damage
            (.log js/console "combo running total: %d" (<! channel))
            (recur)))
        (doseq [{:keys [player opponent channel]} total-channels]
          ; apply totals as damage/healing
          (go-loop []
            (let [v (<! channel)]
              (cond
                (pos? v) (damage opponent v)
                (neg? v) (damage player v))) ; negative damage is healing
            (recur)))))
    om/IRenderState
    (render-state [this {:keys [hit-channels _] :as state}]
      (dom/div nil
        ; player health bars
        (apply dom/div #js {:className "players"}
          (om/build-all health-view
            (mapv (fn [p] ; build health-view arguments per player
                    {:player p
                     :select-player (partial select-player app p)})
                  (:players app))))
        ; combo damage buttons
        ; NOTE: checking player ids instead of doing straight equality on
        ; players, because the cursor values are different -- even though it
        ; totally works later when we call the damage function on a player
        ; cursor stored in a channel hash. Weird stuff. I assume it's related
        ; to the render lifecycle? Since this render is prompted by player
        ; select, you'd think the :current value of both players would be up to
        ; date in all cursors, though.
        (let [for-player (fn [{:keys [player]}]
                           (= (:id player) (:id (get-current-player app))))
              hits (-> (filter for-player hit-channels) first :channel)]
          (apply dom/div nil
            (map (fn [n]
                   (dom/button
                     #js {:onClick #(put! hits n)}
                     ; negative damage should be displayed as "+n". Maybe this
                     ; is weird? I'll think about it later.
                     (if (pos? n) n (str "+" (Math/abs n)))))
                 (sort (concat [-4] (range 1 21))))))))))
