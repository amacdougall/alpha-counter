(ns alpha-counter.views.character-select
  (:require [alpha-counter.data :as data]
            [alpha-counter.views.util :refer [classes]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;; Given a player and a character, returns an element which selects that
;; character on click.
(defn- character->icon [player character]
  (let [selected? (= (:name character) (-> player :character :name))
        button-classes (classes "small button" (when selected? "selected"))]
    (dom/li nil
      (dom/button
        #js {:className button-classes
             :onClick #(data/choose-character player character)}
        (str (when (and selected? (:ex player)) "EX ") (:name character))))))

;; Given a player, returns a vec of character icon elements which select that
;; character for that player on click.
(defn- player->icons [player]
  (mapv (partial character->icon player) data/characters))

;; Top-level view which displays a character icon grid for each player,
;; followed by a Ready button. The Ready button is only enabled when both
;; players have selected a character.
(defn main [app owner]
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
                           :onClick #(data/ready! app)
                           :disabled (some #(nil? (:character %)) [p1 p2])}
            "Start!"))))))
