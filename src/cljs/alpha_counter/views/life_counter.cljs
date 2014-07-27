(ns alpha-counter.views.life-counter
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [alpha-counter.channels :refer [running-total trickle]]
            [cljs.core.async :refer [>! <! chan put!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;; Time before combo is considered complete, in ms.
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

(defn- init-hit-channels [app]
  [{:player (-> app :players first)
    :opponent (-> app :players second)
    :channel (chan)}
   {:player (-> app :players second)
    :opponent (-> app :players first)
    :channel (chan)}])

(defn- init-combo-channels [hit-channels]
  (mapv (fn [{:keys [channel] :as m}]
          (assoc m :channel (running-total channel combo-timeout)))
        hit-channels))

(defn life-counter-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      (let [hit-channels (init-hit-channels app)
            combo-channels (init-combo-channels hit-channels)]
        {:hit-channels hit-channels, :combo-channels combo-channels}))
    om/IWillMount
    (will-mount [_]
      (let [combo-channels (om/get-state owner :combo-channels)]
        (doseq [{:keys [player opponent channel]} combo-channels]
          ; display running total from combo channel on screen;
          ; add grand total to opponent damage or self healing
          (go-loop []
            (let [[k v] (<! channel)]
              (condp = k
                :running-total
                (.log js/console "running total: %d" v)
                :grand-total
                (cond
                  (pos? v) (damage opponent v)
                  (neg? v) (damage player v)))) ; negative damage is healing
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
        (let [for-player (fn [{:keys [player channel]}]
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
