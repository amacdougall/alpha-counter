(ns alpha-counter.abilities
  (:require [alpha-counter.data :as data]))

;; Returns the current player id of the opposing team.
(defn- opponent [player-id]
  (let [team-id (data/team-id-of player-id)
        own-team? #(= (:id %) team-id)
        rival-team (first (remove own-team? (data/teams)))]
    (:current-player-id rival-team)))

; gwen
(defn shadow-plague! []
  (data/register-hit! 2 (data/player-id-of "Gwen")))

;; The self-damage effect of her Desperate Strike destiny card.
(defn desperation! []
  (data/register-hit! 4 (data/player-id-of "Gwen")))

; gloria
(defn overdose! []
  ; TODO: handle 2v1 and 2v2, where only one opponent is on the front line
  (let [self (data/player-id-of "Gloria")
        target (opponent self)]
    (data/register-hit! 10 self)
    (js/setTimeout #(data/register-hit! 10 target) 1000)))

(defn healing-touch! []
  (data/register-hit! -4 (data/player-id-of "Gloria")))

(defn bathed-in-moonlight! []
  ; TODO: handle 2v1 and 2v2, where only one opponent is on the front line
  (let [self (data/player-id-of "Gloria")
        target (opponent self)]
    (data/register-hit! -4 self)
    (data/register-hit! -4 target)))

(defn pulsing-globes! []
  (data/register-hit! -3 (data/player-id-of "Gloria")))

; argagarg
(defn hex-of-murkwood! []
  (data/register-hit! 2 (opponent (data/player-id-of "Argagarg"))))

;; The EX version of Hex of Murkwood, which deals 5 damage. EX Argagarg also
;; gets the original version, because with Bubble Shield up, his hex does an
;; extra 2.
(defn ex-of-murkwood! []
  (data/register-hit! 5 (opponent (data/player-id-of "Argagarg"))))

; jaina
;; True if Jaina's health is low enough to use Burning Desperation.
(defn jaina-desperation? []
  (<= (:health (data/player-of "Jaina")) 35))

(defn burning-vigor! []
  (data/register-hit! 3 (data/player-id-of "Jaina")))

(defn burning-desperation! []
  (data/register-hit! 4 (data/player-id-of "Jaina")))

; persephone
(defn loyal-pets! []
  (data/register-hit! 6 (opponent (data/player-id-of "Persephone"))))

; vendetta
;; Applies a single point of poison damage, so you'll have to tap it once for
;; each stack. I was thinking of using a debuff/tick mechanism to handle
;; stacking, but that raises issues of its own: you can't undo a poison debuff
;; with the undo button, for instance, and "Add Poison" and "Remove Poison"
;; would be an awkward pair of buttons, and there's no indication of how many
;; debuffs the opponent has, and so on. Just tapping four times is a small
;; price.
(defn poison! []
  (data/register-hit! 1 (opponent (data/player-id-of "Vendetta"))))

(defn active []
  ; TODO: refactor this if we touch it again; but as long as it works, it's
  ; okay as it is.
  (let [abilities
        [(when (data/chosen? "Gwen")
           (let [ex? (:ex (data/player-of "Gwen"))]
           [["Shadow Plague" shadow-plague!]
            (when ex? ["Desperation" desperation!])]))
         (when (data/chosen? "Gloria")
           (let [ex? (:ex (data/player-of "Gloria"))]
             [["Overdose" overdose!]
              ["Healing Touch" healing-touch!]
              ["Bathed in Moonlight" bathed-in-moonlight!]
              (when ex? ["Pulsing Globes" pulsing-globes!])]))
         (when (data/chosen? "Argagarg")
           (let [ex? (:ex (data/player-of "Argagarg"))]
             [(when ex? ["EX of Murkwood" ex-of-murkwood!])
              ["Hex of Murkwood" hex-of-murkwood!]]))
         (when (data/chosen? "Jaina")
           (if (:ex (data/player-of "Jaina"))
             [["Overeager Vigor" burning-vigor!]]
             [["Burning Vigor" burning-vigor!]
              (when (jaina-desperation?)
                ["Burning Desperation" burning-desperation!])]))
         (when (and (data/chosen? "Persephone") (:ex (data/player-of "Persephone")))
           [["Loyal Pets" loyal-pets!]])
         (when (and (data/chosen? "Vendetta") (:ex (data/player-of "Vendetta")))
           [["Poison" poison!]])]]
    (->> abilities
      flatten
      (remove nil?)
      (partition 2))))
