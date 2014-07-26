(ns alpha-counter.views.character-select
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

; TODO: move this to its own file?
(def characters
  [{:name "Grave" :health 90}
   {:name "Jaina" :health 85}
   {:name "Rook" :health 100}
   {:name "Midori" :health 95}
   {:name "Setsuki" :health 70}
   {:name "Valerie" :health 85}])


;; Sets the app ready.
(defn ready [app]
  (om/transact! app :ready (constantly true)))

;; Initializes the player by selecting the character. Sets player health
;; to the character's max health, and empties player history.
(defn- select-character [player character]
  (om/transact! player #(assoc % :character character
                                 :current false
                                 :health (:health character)
                                 :history [])))

(defn character-select-view [app owner]
  (reify
    om/IRender
    (render [_]
      ; TODO: format this better? Split it up? I'm sure there's something
      ; helpful in the colossal core library.
      (let [p1 (-> app :players first)
            p2 (-> app :players second)
            icons (fn [player]
                    (mapv (fn [c]
                            (dom/button
                              #js {:onClick #(select-character player c)}
                              (:name c)))
                          characters))]
        (dom/div nil
          (dom/h1 nil "Character Select")
          (dom/h2 nil "Player One")
          (apply dom/div nil (icons p1))
          (dom/h2 nil "Player Two")
          (apply dom/div nil (icons p2))
          (dom/button #js {:onClick #(ready app)
                           :disabled (some empty? [p1 p2])}
            "Start!"))))))
