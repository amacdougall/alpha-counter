(ns alpha-counter.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [alpha-counter.channels :refer [running-total delayed-total trickle]]
            [cljs.core.async :refer [>! <! chan put! mult tap]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

; Configuration Constants
;; Vec of {:name :health} hashes representing characters in the game.
(def characters
  [{:name "Grave" :health 90}
   {:name "Midori" :health 95}
   {:name "Rook" :health 100}
   {:name "Valerie" :health 85}
   {:name "Lum" :health 90}
   {:name "Jaina" :health 85}
   {:name "Setsuki" :health 70}
   {:name "DeGrey" :health 90}
   {:name "Geiger" :health 90}
   {:name "Argagarg" :health 85}
   {:name "Quince" :health 90}
   {:name "Troq" :health 95}
   {:name "Menelker" :health 70}
   {:name "Gloria" :health 70}
   {:name "Vendetta" :health 85}
   {:name "Onimaru" :health 90}
   {:name "Bal-Bas-Beta" :health 80}
   {:name "Persephone" :health 80}
   {:name "Gwen" :health 85}
   {:name "Zane" :health 85}])


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

(def app-state
  (atom
    {:ready false ; when true, displays the main life counter
     :players [{:id :player-one} {:id :player-two}]}))

; Utility
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

;; Returns a vec of hashes {:player :opponent :channel :mult}, where the
;; :channel value is the input channel of incoming hits, and the :mult value is
;; a mult which interested channels may tap to receive hit data. For
;; life-counter-view state.
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
;; be set as the running total of the current combo. For life-counter-view state.
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
;; opponent or healing to the player. For life-counter-view state.
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

;; Sets the app ready.
(defn ready [app]
  (om/update! app [:ready] true))


; Character Select
;; Initializes the player by selecting the character. Sets player health
;; to the character's max health, and empties player history.
(defn- select-character [player character]
  (om/transact! player #(assoc % :character character
                                 :current false
                                 :health (:health character)
                                 :history [])))

(defn character-select-view [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "CharacterSelectView")
    om/IRender
    (render [_]
      ; TODO: format this better? Split it up? I'm sure there's something
      ; helpful in the colossal core library.
      (let [p1 (-> app :players first)
            p2 (-> app :players second)
            icons (fn [player]
                    (mapv (fn [c]
                            (dom/li nil
                              (dom/button
                                #js {:className "small button"
                                     :onClick #(select-character player c)}
                                (:name c))))
                          characters))]
        (dom/div #js {:className "character-select"}
          (dom/h1 nil "Character Select")
          (dom/h2 nil "Player One")
          (apply dom/ul #js {:className "list"} (icons p1))
          (dom/h2 nil "Player Two")
          (apply dom/ul #js {:className "list"} (icons p2))
          (dom/button #js {:className "button"
                           :onClick #(ready app)
                           :disabled (some #(nil? (:character %)) [p1 p2])}
            "Start!"))))))

; Life Counter: health bars, combo damage display, damage buttons
; TODO: subdivide further
;; Health bar view. Expects props {:player p, :select-player fn}.
(defn- health-view [props owner]
  (reify
    om/IDisplayName
    (display-name [_] "HealthView")
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

(defn life-counter-view [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "LifeCounterView")
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
                     #js {:className "button"
                          :onClick #(put! hits n)}
                     ; negative damage should be displayed as "+n". Maybe this
                     ; is weird? I'll think about it later.
                     (if (pos? n) n (str "+" (Math/abs n)))))
                 (sort (concat [-4] (range 1 21))))))))))


; Base
(defn main-view [app owner]
  (dom/div #js {:className "content"}
    (if-not (:ready app)
      (om/build character-select-view app)
      (om/build life-counter-view app))))
; NOTE: gotta om/build each thing individually if we want to run init-state; we
; can't just call (desired-view app owner). This is what we want, anyway, because
; we want to apply a different classname to each component.

(om/root main-view app-state
  {:target (. js/document (getElementById "main"))})
