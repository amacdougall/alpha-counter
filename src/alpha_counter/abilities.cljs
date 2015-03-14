(ns alpha-counter.abilities
  (:require [alpha-counter.data :as data]))

(defn- player-ids []
  (map :id (:players @data/app-state)))

(defn- opponent [player-id]
  (first (remove #{player-id} (player-ids))))

; gwen
(defn shadow-plague! []
  (data/register-hit 2 (data/player-id-of "Gwen")))

; gloria
(defn overdose! []
  ; TODO: handle 2v1 and 2v2, where only one opponent is on the front line
  (let [self (data/player-id-of "Gloria")
        target (opponent self)]
    (data/register-hit 10 self)
    (js/setTimeout #(data/register-hit 10 target) 1000)))

(defn healing-touch! []
  (data/register-hit -4 (data/player-id-of "Gloria")))

(defn bathed-in-moonlight! []
  ; TODO: handle 2v1 and 2v2, where only one opponent is on the front line
  (let [self (data/player-id-of "Gloria")
        target (opponent self)]
    (data/register-hit -4 self)
    (data/register-hit -4 target)))

; argagarg
(defn hex-of-murkwood! []
  (data/register-hit 2 (opponent (data/player-id-of "Argagarg"))))

;; The EX version of Hex of Murkwood, which deals 5 damage. EX Argagarg also
;; gets the original version, because with Bubble Shield up, his hex does an
;; extra 2.
(defn ex-of-murkwood! []
  (data/register-hit 5 (opponent (data/player-id-of "Argagarg"))))

; jaina
;; True if Jaina's health is low enough to use Burning Desperation.
(defn jaina-desperation? []
  (<= (:health (data/player-of "Jaina")) 35))

(defn burning-vigor! []
  (data/register-hit 3 (data/player-id-of "Jaina")))

(defn burning-desperation! []
  (data/register-hit 4 (data/player-id-of "Jaina")))

(defn active []
  (let [abilities
        [(when (data/chosen? "Gwen")
           [["Shadow Plague" shadow-plague!]])
         (when (data/chosen? "Gloria")
           [["Overdose" overdose!]
            ["Healing Touch" healing-touch!]
            ["Bathed in Moonlight" bathed-in-moonlight!]])
         (when (data/chosen? "Argagarg")
           (if (:ex (data/player-of "Argagarg"))
             [["EX of Murkwood" ex-of-murkwood!]
              ["Hex of Murkwood" hex-of-murkwood!]]
             [["Hex of Murkwood" hex-of-murkwood!]]))
         (when (data/chosen? "Jaina")
           (if (:ex (data/player-of "Jaina"))
             [["Overeager Vigor" burning-vigor!]]
             [["Burning Vigor" burning-vigor!]
              (when (jaina-desperation?)
                ["Burning Desperation" burning-desperation!])]))]]
    (->> abilities
      flatten
      (remove nil?)
      (partition 2))))
