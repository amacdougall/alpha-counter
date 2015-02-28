(ns alpha-counter.data
  (:require [alpha-counter.channels :as channels]
            [om.core :as om :include-macros true]
            [cljs.core.async :refer [chan mult tap <! >! put! close!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

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

(declare app-state channels)

(defonce app-state
  (atom
    {:characters-selected false ; when true, displays the main life counter
     :players [{:id :player-one} {:id :player-two}]
     :current-player-id :player-one
     :running-total 0
     :history []}))

;; Contains a map {:player-id {:hits ch, :running-total ch, :damage ch}, ...},
;; for each player. The :hits channel is an input channel which should be given
;; damage amounts from buttons. The :running-total channel receives values
;; representing the running total of the current combo. The :damage channel
;; receives actual damage values to be applied to the player.
(defonce channels
  (atom nil))

;; Returns a reference cursor for the app state. To the best of my knowledgely,
;; if you aren't planning to transact on the cursor, you may as well just deref
;; it. om/value vs @app-state is probably not going to be a big deal.
(defn app-cursor []
  (om/ref-cursor (om/root-cursor app-state)))

;; Returns a reference cursor for the damage history.
(defn history-cursor []
  (om/ref-cursor (:history (om/root-cursor app-state))))

;; Returns the {:hits :running-total :damage} channel map for player with the
;; supplied id.
(defn channels-for [player-id]
  (player-id @channels))

;; Sets :current-player-id to the id of the supplied player.
(defn select-player [app player]
  (om/update! app [:current-player-id] (:id player)))

;; Given a damage amount and optionally a player id, registers a hit on that
;; player for that amount. If id is omitted, applies the hit to the current
;; player.
(defn register-hit
  ([n]
   (register-hit n (:current-player-id @app-state)))
  ([n id]
   (let [hits (:hits (channels-for id))]
     (put! hits n)
     (om/transact! (history-cursor) #(conj % [id n])))))

(defn undo []
  (if-not (empty? (om/value (history-cursor)))
    (om/transact! (om/root-cursor app-state)
      (fn [{:keys [history] :as app}]
        (let [[id n] (peek history)
              hits (:hits (channels-for id))]
          (put! hits (- n))
          (assoc app
                 :current-player-id id
                 :history (pop history)))))))

;; Subtracts n health from the player. If n is negative, this will heal the
;; player, but only up to the maximum health of the player's character.
(defn apply-damage [player n]
  (om/transact! player (fn [player]
                         (let [health (:health player)
                               max-health (-> player :character :health)]
                           (assoc player :health (min (- health n)
                                                      max-health))))))

(defn- player->channels [player]
  (let [hits (chan)
        mult (mult hits)
        running-total (-> (tap mult (chan))
                        (channels/trickle damage-change-speed)
                        (channels/running-total combo-timeout))
        damage (-> (tap mult (chan))
                 (channels/delayed-total combo-timeout)
                 (channels/trickle health-change-speed))]

    ; update running total as numbers come off any running-total channel
    (go-loop []
      (when-let [n (<! running-total)]
        (om/update! (app-cursor) [:running-total] n)
        (recur)))

    ; apply damage to player as it comes off the delay->trickle chain
    (go-loop []
      (when-let [v (<! damage)]
        (apply-damage player v)
        (recur)))

    {:hits hits
     :running-total running-total
     :damage damage}))

;; Resets character selection and returns to the character screen.
(defn return-to-character-select []
  (om/transact! (app-cursor)
    (fn [app]
      (assoc app
             :characters-selected false
             :running-total 0))))

;; Returns a channel hash appropriate for the channels atom.
(defn- build-channels [{:keys [players]}]
  (apply assoc {} (interleave (map :id players) (map player->channels players))))

;; Sets the app ready.
(defn ready [app]
  (reset! channels (build-channels app)) ; old channels can just be GCed
  (om/update! app [:characters-selected] true))

; Character Select
;; Initializes the player by selecting the character. Sets player health to the
;; character's max health.
(defn choose-character [player character]
  (om/transact! player #(assoc % :character character
                                 :health (:health character))))

(defn chosen? [character-name]
  (let [players (:players @app-state)
        ->name #(:name (:character %))
        names (map ->name players)]
    (some (partial = character-name) names)))

(defn player-of [character-name]
  (let [players (:players @app-state)
              has-character #(= (:name (:character %)) character-name)]
      (:id (first (filter has-character players)))))
