;; Namespace containing data lookup and manipulation functions. Note that as a
;; rule, any function which returns something from the app state returns a
;; cursor to it; this include functions such as players and current-team.
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
   {:name "Bal-Bas-Beta" :health 80 :ex-health 100}
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

;; App state consists of the following keys:
;;
;; * characters-selected: true if character select is complete.
;; * teams: a vector of team objects; see below.
;; * current-team-id: the keyword id of the currently active team.
;; * running-total: the running total of the current combo.
;; * history: a vector of `[player-id n]` pairs representing every hit which
;;   has occurred. Recorded by register-hit!.
;;
;; Each team is a map in this form:
;;
;; ```
;; {:id :team-one
;;  :players [{:id :player-one, :health 90, :ex false
;;             :character {:name "Grave", :health 90}}]}
;; ```
(defonce app-state
  (atom
    {:characters-selected false
     ; TODO: remove assumption that teams start with players
     :teams [{:id :team-one
              :current-player-id :player-one
              :players [{:id :player-one}]}
             {:id :team-two
              :current-player-id :player-two
              :players [{:id :player-two}]}]
     :current-team-id :team-one
     :running-total 0
     :history []}))

;; Contains a map {:player-id {:hits ch, :running-total ch, :damage ch}, ...},
;; for each player. The :hits channel is an input channel which should be given
;; damage amounts from buttons. The :running-total channel receives values
;; representing the running total of the current combo. The :damage channel
;; receives actual damage values to be applied to the player.
(defonce channels
  (atom nil))

;; Returns a cursor for the app state as a whole.
(defn app-cursor []
  (om/ref-cursor (om/root-cursor app-state)))

;; Returns a cursor for the damage history.
(defn history-cursor []
  (om/ref-cursor (:history (om/root-cursor app-state))))

;; Returns the {:hits :running-total :damage} channel map for player with the
;; supplied id.
(defn channels-for [player-id]
  (player-id @channels))

;; Sets :current-team-id to the id of the supplied team.
(defn select-team! [team]
  (om/update! (app-cursor) [:current-team-id] (:id team)))

;; If the supplied team has two players, swaps which one is selected.
(defn tag! [team]
  (when-not (zero? (count team))
    (let [current (:current-player-id team)
          bench (first (remove #{current} (map :id team)))]
      (om/update! team [:current-player-id] bench))))

;; Returns the maximum health of the player, based on the chosen character.
(defn max-health [player]
  (let [character (:character player)]
    (if (:ex player)
      (or (:ex-health character) (:health character))
      (:health character))))

;; Given an id and a map, returns true if the map has an :id key whose value is
;; equal to the id. Use as a filter method with `(partial id-match :k)`.
(defn- id-match [id m]
  (= (:id m) id))

;; Returns the current team.
(defn current-team []
  (let [app (app-cursor)
        id (:current-team-id app)]
    (om/ref-cursor (first (filter (partial id-match id) (:teams app))))))

;; Returns the current player of the current team; in other words, the player
;; who will receive the next hit.
(defn current-player []
  (let [team (current-team)
        id (:current-player-id team)]
    (first (filter (partial id-match id) (:players team)))))

;; Returns a list of all players in the game. Note that the list is not a
;; cursor in itself, since there is no such list embodied in the app-state.
(defn players []
  (let [app (app-cursor)
        teams (get app :teams)
        players (flatten (map #(get % :players) teams))]
    players))

;; Returns a vec of all teams in the game.
(defn teams []
  (get (app-cursor) :teams))

;; Returns the team of the supplied player or player id.
(defmulti team-of keyword?)

(defmethod team-of false [player]
  (team-of (:id player)))

(defmethod team-of true [id]
  (let [has-player #(some (partial id-match id) (:players %))]
    (first (filter has-player (teams)))))

;; Convenience method which returns the id of the team of the supplied player.
(defn team-id-of [player]
  (:id (team-of player)))

;; Given a damage amount and optionally a player id, registers a hit on that
;; player for that amount, by putting it on the player's hits channel. If id is
;; omitted, applies the hit to the current player.
(defn register-hit!
  ([n]
   (register-hit! n (:id (current-player))))
  ([n id]
   (let [hits (:hits (channels-for id))]
     (put! hits n)
     (om/transact! (history-cursor) #(conj % [id n])))))

;; Reverses the most recently registered hit, incidentally selecting the target
;; team and player.
(defn undo! []
  (if-not (empty? (om/value (history-cursor)))
    (om/transact! (om/root-cursor app-state)
      (fn [{:keys [history] :as app}]
        (let [[player-id n] (peek history)
              hits (:hits (channels-for player-id))
              target-team-id (team-id-of player-id)
              select-player (fn [{:keys [team-id] :as team}]
                              (if (= team-id target-team-id)
                                (assoc team :current-player-id player-id)
                                team))]
          ; reverse the hit
          (put! hits (- n))
          ; set current team, and current player within the relevant team
          (assoc app
                 :current-team-id target-team-id
                 :teams (mapv select-player (:teams app))
                 :history (pop history)))))))

;; Subtracts n health from the player. If n is negative, this will heal the
;; player, but only up to the maximum health of the player's character.
(defn apply-damage! [player n]
  (om/transact! player
    (fn [player]
      (let [health (:health player)]
        (assoc player :health (min (- health n) (max-health player)))))))

;; Builds hits, running-total, and damage channels for the supplied player
;; cursor, returning a map `{:hits ch, :running-total ch, :damage ch}`. Has the
;; side effect of starting loops over the running-total and damage channels, so
;; invoke cautiously.
;;
;; TODO: move the loop setup to another method. I'm not even giving this a bang
;; name, because it's so self-evident that it needs to be refactored instead.
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
        (apply-damage! player v)
        (recur)))

    {:hits hits
     :running-total running-total
     :damage damage}))

;; Resets character selection and returns to the character screen.
; TODO: get this to work with team system; for now, can only select once
(defn return-to-character-select! []
  (om/transact! (app-cursor)
    #(assoc % :characters-selected false
              :teams []
              :running-total 0)))

;; Returns a channel hash appropriate for the channels atom. Starts go loops
;; over each channel. See player->channels.
(defn- build-channels! [players]
  (apply assoc {} (interleave (map :id players) (map player->channels players))))

;; Clears all undo history.
(defn- clear-history! []
  (om/update! (history-cursor) []))

;; Returns the player, at full health. Returns the player TO full health.
(defn- reset-health [player]
  (assoc player :health (max-health player)))

;; Returns the team, with all players set to full health.
(defn- reset-team-health [team]
  (assoc team :players (mapv reset-health (:players team))))

;; Sets the app ready. Rebuilds channels, sets players to full health.
; TODO: rename; this is a bit vague.
(defn ready! []
  (let [app (app-cursor)]
    (reset! channels (build-channels! (players))) ; old channels can just be GCed
    (clear-history!)
    (om/transact! app #(assoc % :characters-selected true
                                :teams (mapv reset-team-health (:teams %))))))

;; Returns all players to full health.
(defn rematch! []
  (clear-history!)
  (om/transact! (app-cursor) #(assoc % :teams (mapv reset-team-health (:teams %)))))

; Character Select
;; Initializes the player by selecting the character. If that character is
;; already selected, toggles EX status.
(defn choose-character! [player character]
  (if (= (:character player) character)
    (om/update! player [:ex] (not (:ex player)))
    (om/transact! player #(assoc % :character character, :ex false))))

;; Returns true if the supplied character has been chosen by any player.
(defn chosen? [character-name]
  (let [chosen-characters (map #(-> % :character :name) (players))]
    (some (partial = character-name) chosen-characters)))

;; Returns the player who is using the supplied character. Note that mirror
;; matches are fundamentally unsupported at the moment.
(defn player-of [character-name]
  (let [has-character #(= (-> % :character :name) character-name)]
      (first (filter has-character (players)))))

;; Returns the player id of the player who is using the supplied character. A
;; convenience method to avoid typing (:id (data/player-of x)) over and over.
(defn player-id-of [character-name]
  (:id (player-of character-name)))
