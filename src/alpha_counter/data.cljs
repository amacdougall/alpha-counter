(ns alpha-counter.data
  (:require [alpha-counter.channels :as channels]
            [om.core :as om :include-macros true]
            [cljs.core.async :refer [chan mult tap]]))

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
     :current-player-id :player-one}))

;; Sets :current-player-id to the id of the supplied player.
(defn select-player [app player]
  (om/update! app [:current-player-id] (:id player)))

;; Subtracts n health from the player. If n is negative, this will heal the
;; player, but only up to the maximum health of the player's character.
(defn apply-damage [player n]
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
(defn init-hit-channels [app]
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
(defn init-damage-channels [hit-channels]
  (mapv (fn [{:keys [mult] :as m}]
          (-> m
            (dissoc :mult)
            (assoc :channel (-> (tap mult (chan))
                              (channels/trickle damage-change-speed)
                              (channels/running-total combo-timeout)))))
        hit-channels))

;; Given a vec of hit channel hashes, returns a vec of combo damage total
;; channels. Each value on these channels should be applied directly as damage.
;;
;; In a two-hit combo dealing 2 and then 3 damage, the values would be 1, 1, 1,
;; 1, 1, beginning only after the combo timeout.
(defn init-total-channels [hit-channels]
  (mapv (fn [{:keys [mult] :as m}]
          (-> m
            (dissoc :mult)
            ; input to this channel will be held until combo-timeout completes;
            ; then the grand total will be trickled as a stream of 1s.
            (assoc :channel (-> (tap mult (chan))
                              (channels/delayed-total combo-timeout)
                              (channels/trickle health-change-speed)))))
        hit-channels))

;; Sets the app ready.
(defn ready [app]
  (om/update! app [:characters-selected] true))

; Character Select
;; Initializes the player by selecting the character. Sets player health to the
;; character's max health.
(defn select-character [player character]
  (om/transact! player #(assoc % :character character
                                 :health (:health character))))
