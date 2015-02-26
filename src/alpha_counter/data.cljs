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

(defonce app-state
  (atom
    {:characters-selected false ; when true, displays the main life counter
     :players [{:id :player-one} {:id :player-two}]
     :current-player-id :player-one
     :history []}))

;; Contains a map {:player-id {:hits ch, :running-total ch, :damage ch}, ...},
;; for each player. The :hits channel is an input channel which should be given
;; damage amounts from buttons. The :running-total channel receives values
;; representing the running total of the current combo. The :damage channel
;; receives actual damage values to be applied to the player.
(defonce channels
  (atom nil))

;; Returns a reference cursor for the damage history.
(defn history []
  (om/ref-cursor (:history (om/root-cursor app-state))))

;; Returns the {:hits :running-total :damage} channel map for player with the
;; supplied id.
(defn channels-for [player-id]
  (player-id @channels))

;; Sets :current-player-id to the id of the supplied player.
(defn select-player [app player]
  (om/update! app [:current-player-id] (:id player)))

(defn register-hit [n]
  (let [hits (:hits (channels-for (:current-player-id @app-state)))]
    (put! hits n)))

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

    ; apply damage to player as it comes off the delay->trickle chain
    (go-loop []
      (when-let [v (<! damage)]
        (apply-damage player v)
        (recur)))

    {:hits hits
     :running-total running-total
     :damage damage}))

;; Returns a channel hash appropriate for the channels atom.
(defn- build-channels [{:keys [players]}]
  (apply assoc {} (interleave (map :id players) (map player->channels players))))

;; Closes all channels in the channels atom. Useful when resetting the
;; application state after character change or a Figwheel live reload.
(defn- close-all-channels! []
  (doseq [[_ v] @channels, 
          [_ ch] v]
    (close! ch)))

;; Sets the app ready.
(defn ready [app]
  (close-all-channels!)
  (reset! channels (build-channels app))
  (om/update! app [:characters-selected] true))

; Character Select
;; Initializes the player by selecting the character. Sets player health to the
;; character's max health.
(defn select-character [player character]
  (om/transact! player #(assoc % :character character
                                 :health (:health character))))
