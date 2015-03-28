(ns alpha-counter.views.character-select
  (:require [alpha-counter.data :as data]
            [sablono.core :as html :refer-macros [html]]
            [alpha-counter.views.util :refer [classes html-container]]
            [om.core :as om :include-macros true]))

;; Given a player and a character, returns an element which selects that
;; character on click.
(defn- character->icon [player character]
  (let [selected? (= (:name character) (-> player :character :name))
        button-classes (classes "small button" (when selected? "selected"))]
    (html [:li
           [:button {:class button-classes
                     :on-click #(data/choose-character! player character)}
            (str (when (and selected? (:ex player)) "EX ") (:name character))]])))

;; Given a player, returns a vec of character icon elements which select that
;; character for that player on click.
(defn- player->icons [player]
  (mapv (partial character->icon player) data/characters))

;; Top-level component which displays a character icon grid for each player,
;; followed by a Ready button. The Ready button is only enabled when both
;; players have selected a character.
(defn main [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "CharacterSelect")
    om/IRender
    (render [_]
      ; TODO: enable for two-player teams; right now it assumes that each
      ; team has exactly one player.
      (let [p1 (-> app :teams first :players first)
            p2 (-> app :teams second :players first)]
        (html [:div {:class "character-select"}
               [:h1 nil "Character Select"]
               [:h2 nil "Player One"]
               (html-container [:ul {:class "list"}] (player->icons p1))
               [:h2 nil "Player Two"]
               (html-container [:ul {:class "list"}] (player->icons p2))
               [:button {:class "button"
                         :on-click data/ready!
                         :disabled (some #(nil? (:character %)) [p1 p2])}
                "Start!"]])))))
