(ns alpha-counter.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [alpha-counter.channels :refer [running-total delayed-total trickle]]
            [clojure.string :as string]
            [goog.events :as events]
            [FastClick] ; foreign lib, appears as js/FastClick
            [cljs.core.async :refer [>! <! chan put! mult tap]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:import [goog.events EventType]))

(enable-console-print!)

; Configuration Constants
;; Vec of {:name :health} hashes representing characters in the game.
(def characters
  [{:name "Grave" :health 90}
   {:name "Midori" :health 90}
   {:name "Rook" :health 100}
   {:name "Valerie" :health 80}
   {:name "Lum" :health 90}
   {:name "Jaina" :health 85}
   {:name "Setsuki" :health 70}
   {:name "DeGrey" :health 90}
   {:name "Geiger" :health 90}
   {:name "Argagarg" :health 85}
   {:name "Quince" :health 90}
   {:name "Bal-Bas-Beta" :health 80}
   {:name "Menelker" :health 70}
   {:name "Gloria" :health 70}
   {:name "Vendetta" :health 75}
   {:name "Onimaru" :health 90}
   {:name "Troq" :health 95}
   {:name "Persephone" :health 75}
   {:name "Gwen" :health 85}
   {:name "Zane" :health 85}])

;; Every amount of damage which can be dealt in the game. Populates the buttons list.
(def damage-amounts (sort (concat [-12 -6 -4] (range 1 21) [21 22 29 36 45 50 55])))

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

(defonce app-state
  (atom
    {:characters-selected false ; when true, displays the main life counter
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

;; Subtracts n health from the player. If n is negative, this will heal the
;; player, but only up to the maximum health of the player's character.
(defn- damage [player n]
  (om/transact! player (fn [player]
                         (let [health (:health player)
                               max-health (-> player :character :health)]
                           (assoc player :health (min (- health n)
                                                      max-health))))))

;; Returns a vec of hashes {:player :channel :mult}, for each player. When a
;; player is hit, push the damage amount onto :channel. Tap the :mult to
;; receive hit notifications.
;;
;; Clojure Newbie Note™: Providing hit notifications as a mult instead of a
;; single channel allows us to do unrelated things with the notification
;; streams, with independent timing. With a single-channel workflow, we would
;; have to take a hit from the channel, and do multiple actions with it at the
;; same time. By tapping a mult to generate independent channels, we can read
;; from different taps at different speeds.
(defn- init-hit-channels [app]
  (let [p1-hits (chan)
        p2-hits (chan)]
    [{:player (-> app :players first)
      :channel p1-hits
      :mult (mult p1-hits)}
     {:player (-> app :players second)
      :channel p2-hits
      :mult (mult p2-hits)}]))

;; Given a vec of hit channel hashes, returns a vec of running damage total
;; channel hashes {:player :channel}. Each value on these channels represents
;; the new running total of the current combo.
;;
;; In a two-hit combo dealing 2 and then 3 damage, the values would be 2, 5,
;; and then, after the combo timeout, :reset.
(defn- init-damage-channels [hit-channels]
  (mapv (fn [{:keys [mult] :as m}]
          (-> m
            (dissoc :mult)
            (assoc :channel (-> (tap mult (chan))
                              (trickle damage-change-speed)
                              (running-total combo-timeout)))))
        hit-channels))

;; Given a vec of hit channel hashes, returns a vec of combo damage total
;; channels. Each value on these channels should be applied directly as damage.
;;
;; In a two-hit combo dealing 2 and then 3 damage, the values would be 1, 1, 1,
;; 1, 1, beginning only after the combo timeout.
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
  (om/update! app [:characters-selected] true))

; Character Select
;; Initializes the player by selecting the character. Sets player health to the
;; character's max health.
(defn- select-character [player character]
  (om/transact! player #(assoc % :character character
                                 :health (:health character))))

;; Given a player and a character, returns an element which selects that
;; character on click.
(defn- character->icon [player character]
  (let [selected? (= (:name character) (-> player :character :name))
        button-classes (classes "small button" (when selected? "selected"))]
    (dom/li nil
      (dom/button
        #js {:className button-classes
             :onClick #(select-character player character)}
        (:name character)))))

;; Given a player, returns a vec of character icon elements which select that
;; character for that player on click.
(defn- player->icons [player]
  (mapv (partial character->icon player) characters))

;; Top-level view which displays a character icon grid for each player,
;; followed by a Ready button. The Ready button is only enabled when both
;; players have selected a character.
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
;; up; e.g. a player at 50/80 health would return "63%". Useful when applying
;; damage bar width in CSS.
(defn- health-percent [player]
  (-> (health-ratio player) (* 100) Math/ceil (str "%")))

;; Returns the damage the player has taken as a percentage string ending in
;; "%", rounded down; e.g. a player at 50/80 health would return "37%". Useful
;; when applying damage bar margin for right alignment in CSS.
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
            ; NOTE: (js-obj "a" 1 "b" 2) => {a: 1, b: 2} in JavaScript
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
    ;
    ; TODO: There may yet be a way around this. Try again!
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
               damage-amounts))))))

;; When a combo is in progress, display the running total; otherwise, displays
;; the "VS" symbol between the two health-view panes.
(defn combo-view [running-total owner]
  (reify
    om/IDisplayName
    (display-name [_] "ComboView")
    om/IRender
    (render [_]
      (let [idle (#{0 :reset} running-total) ; false if combo is in progress
            is-healing (neg? running-total) ; yes, negative damage restores health
            damage-text (str (when is-healing "+") (Math/abs running-total))
            text (if idle "VS" damage-text)
            active-state-name (cond is-healing "healing", (not idle) "damage")]
            ; active-state-name is nil if idle
        (dom/div #js {:className (classes "combo-view" active-state-name)}
          (dom/div #js {:className "combo-view__text"} text))))))

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
        (doseq [{:keys [player channel]} damage-channels]
          ; update combo running total in component state
          (go-loop []
            (om/set-state! owner :running-total (<! channel))
            (recur)))
        (doseq [{:keys [player channel]} total-channels]
          ; apply totals as damage/healing
          (go-loop []
            (let [v (<! channel)]
              (damage player v))
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
        (if-not (:characters-selected app)
          (om/build character-select-view app)
          (om/build life-counter-view app))))))

(defn main []
  (.attach js/FastClick (.-body js/document))
  (om/root main-view
    app-state
    {:target (. js/document (getElementById "app"))}))

(main)
