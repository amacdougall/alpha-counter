(ns alpha-counter.abilities
  (:require [alpha-counter.data :as data]))

(defn- player-ids []
  (map :id (:players @data/app-state)))

(defn- opponent [player-id]
  (first (remove #{player-id} (player-ids))))

; gwen
(defn shadow-plague []
  (data/register-hit 2 (data/player-of "Gwen")))

; gloria
(defn overdose []
  ; TODO: handle 2v1 and 2v2, where only one opponent is on the front line
  (let [self (data/player-of "Gloria")
        target (opponent self)]
    (data/register-hit 10 self)
    (js/setTimeout #(data/register-hit 10 target) 1000)))

(defn healing-touch []
  (data/register-hit -4 (data/player-of "Gloria")))

(defn bathed-in-moonlight []
  ; TODO: handle 2v1 and 2v2, where only one opponent is on the front line
  (let [self (data/player-of "Gloria")
        target (opponent self)]
    (data/register-hit -4 self)
    (data/register-hit -4 target)))

; argagarg
(defn hex-of-murkwood []
  (data/register-hit 2 (opponent (data/player-of "Argagarg"))))

; jaina
(defn burning-vigor []
  (data/register-hit 3 (data/player-of "Jaina")))

(defn burning-desperation []
  (data/register-hit 4 (data/player-of "Jaina")))
