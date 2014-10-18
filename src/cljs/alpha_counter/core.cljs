(ns alpha-counter.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [alpha-counter.channels :refer [running-total delayed-total trickle]]
            [clojure.string :as string]
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
(def health-change-speed 40)

;; Time before combo is considered complete, in ms. After the timeout, damage
;; will be applied to the player.
(def combo-timeout 3000)

(def app-state
  (atom
    {:ready false ; when true, displays the main life counter
     :players [{:id :player-one, :current true} {:id :player-two}]}))

; Utility
;; Returns the supplied string class names, without nils, as a space-separated
;; string suitable for use as a #js className. Class names may already include
;; spaces, naturally; (classes "inner button" "selected") will return "inner
;; button selected".
(defn- classes [& cs]
  (string/join " " (remove nil? cs)))

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
                                 :health (:health character)
                                 :history [])))

;; Given a player and a character, builds an element which selects that
;; character on click.
(defn- character->icon [player character]
  (let [selected? (= (:name character) (-> player :character :name))
        button-classes (classes "small button" (when selected? "selected"))]
    (dom/li nil
      (dom/button
        #js {:className button-classes
             :onClick #(select-character player character)}
        (:name character)))))

(defn- player->icons [player]
  (mapv (partial character->icon player) characters))

(defn character-select-view [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "CharacterSelectView")
    om/IRender
    (render [_]
      (let [p1 (-> app :players first)
            p2 (-> app :players second)]
        (dom/div #js {:className "character-select"}
          (dom/h1 nil "Character Select")
          (dom/h2 nil "Player One")
          (apply dom/ul #js {:className "list"} (player->icons p1))
          (dom/h2 nil "Player Two")
          (apply dom/ul #js {:className "list"} (player->icons p2))
          (dom/button #js {:className "button"
                           :onClick #(ready app)
                           :disabled (some #(nil? (:character %)) [p1 p2])}
            "Start!"))))))

; Life Counter: health bars, combo damage display, damage buttons
;; Returns the player's health as a float ratio, between 1.0 and 0.0.
(defn- health-ratio [player]
  (let [max-health (:health (:character player))
        health (:health player)]
    (/ health max-health)))

;; Returns the player's health as a percentage string ending in "%", rounded
;; up; e.g. a player at 50/80 health would return "63%". Useful when
;; calculating damage bar width.
(defn- health-percent [player]
  (-> (health-ratio player) (* 100) Math/ceil (str "%")))

;; Returns the damage the player has taken as a percentage string ending in
;; "%", rounded down; e.g. a player at 50/80 health would return "37%". Useful
;; when calculating damage bar margin for right alignment.
(defn- damage-percent [player]
  (-> (- 1 (health-ratio player)) (* 100) Math/floor (str "%")))

;; Health bar view. Expects props {:player p, :select-player fn}. Includes
;; character name, current life as a number, and health and damage bars. The
;; health bar shrinks as the player takes damage, progressively revealing the
;; damage bar.
(defn- health-view [props owner]
  (reify
    om/IDisplayName
    (display-name [_] "HealthView")
    om/IRender
    (render [_]
      (let [{:keys [player select-player]} props
            ; if this is the left-hand player, the health bar should be aligned
            ; right as it shrinks; I'd prefer to do this in CSS, I just don't
            ; know how. TODO: learn how. Maybe dwab or Rachel can help.
            left (= (:id player) :player-one)
            health-width ["width" (health-percent player)]
            health-offset (when left ["marginLeft" (damage-percent player)])
            health-style (apply js-obj (concat health-width health-offset))]
        (dom/li #js {:className (classes "health-view"
                                         (when (:current player) "current"))
                      :onClick #(select-player player)}
          (dom/div #js {:className "health-view__name"} (-> player :character :name))
          (dom/div #js {:className "health-view__health-bar"}
            (dom/div #js {:className "health-view__damage"} "")
            (dom/div #js {:className "health-view__health" :style health-style} "")
            (dom/div #js {:className "health-view__number"} (:health player))))))))

;; List of damage buttons. Expects props {:app :hit-channels}. Automatically
;; sets up buttons to send hits to the appropriate channels.
(defn- damage-buttons-view [props owner]
  (reify
    om/IDisplayName
    (display-name [_] "DamageButtonsView")
    om/IRender
    ; combo damage buttons
    ; NOTE: checking player ids instead of doing straight equality on
    ; players, because the cursor values are different -- even though it
    ; totally works later when we call the damage function on a player
    ; cursor stored in a channel hash. Weird stuff. I assume it's related
    ; to the render lifecycle? Since this render is prompted by player
    ; select, you'd think the :current value of both players would be up to
    ; date in all cursors, though.
    (render [_]
      (let [{:keys [app hit-channels]} props
            for-player (fn [{:keys [player]}]
                         (= (:id player) (:id (get-current-player app))))
            hits (-> (filter for-player hit-channels) first :channel)]
        (apply dom/ul #js {:className "damage-buttons"}
          (map (fn [n]
                 (dom/li nil
                   (dom/button
                    #js {:className "small button"
                         :onClick #(put! hits n)}
                    ; negative damage should be displayed as "+n". Maybe this
                    ; is weird? I'll think about it later.
                    (if (pos? n) n (str "+" (Math/abs n))))))
               (sort (concat [-4] (range 1 21)))))))))

;; When a combo is in progress, display the running total; otherwise, displays
;; the "VS" symbol between the two health-view panes.
(defn combo-view [running-total owner]
  (reify
    om/IDisplayName
    (display-name [_] "ComboDamageView")
    om/IRender
    (render [_]
      (dom/div nil (str "Running total: "
                        (if (= running-total :reset) "none" running-total))))))

;; Life counter view. Includes the combo damage readout and a health-view for
;; each player. Its local state includes channels which manage the combo damage
;; running total and the total damage.
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
         :total-channels total-channels
         :running-total 0}))
    om/IWillMount
    (will-mount [_]
      (let [damage-channels (om/get-state owner :damage-channels)
            total-channels (om/get-state owner :total-channels)]
        (doseq [{:keys [player opponent channel]} damage-channels]
          ; update combo running total in component state
          (go-loop []
            (om/set-state! owner :running-total (<! channel))
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
    (render-state [_ {:keys [hit-channels running-total] :as state}]
      (dom/div #js {:className "life-counter"}
        ; combo total
        (om/build combo-view running-total)
        ; player health bars
        (apply dom/ul #js {:className "players"}
          (om/build-all health-view
            (mapv (fn [p] ; build health-view arguments per player
                    {:player p
                     :select-player (partial select-player app p)})
                  (:players app))))
        ; damage buttons
        (om/build damage-buttons-view {:app app :hit-channels hit-channels})))))


; Base
(defn main-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "content"}
        (if-not (:ready app)
          (om/build character-select-view app)
          (om/build life-counter-view app))))))

(om/root main-view app-state
  {:target (. js/document (getElementById "main"))})
