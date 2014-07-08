(ns alpha-counter.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def characters
  [{:name "Grave" :health 90}
   {:name "Jaina" :health 85}
   {:name "Rook" :health 100}
   {:name "Midori" :health 95}
   {:name "Setsuki" :health 70}
   {:name "Valerie" :health 85}])


(def app-state
  (atom
    {:ready false ; when true, displays the main life counter
     :current-player nil
     :players [{} {}]}))

; helpers
(defn select-character [player character]
  (om/transact! player #(assoc % :character character
                                 :health (:health character)
                                 :history [])))

(defn ready [app]
  (om/transact! app #(assoc % :ready true)))

; views
(defn character-select-view [app owner]
  (reify
    om/IRender
    (render [_]
      ; TODO: format this better? Split it up? I'm sure there's something
      ; helpful in the colossal core library.
      (let [p1 (-> app :players first)
            p2 (-> app :players second)
            icons (fn [player]
                    (map (fn [c]
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

(defn life-counter-view [player owner]
  (dom/h1 nil "Coming soon!"))

(defmulti main-view (fn [app _] (:ready app)))

(defmethod main-view false [app owner]
  (character-select-view app owner))

(defmethod main-view true [app owner]
  (life-counter-view (:current-player app) owner))

(om/root main-view app-state
  {:target (. js/document (getElementById "main"))})

;; scratch below this line

; (:players @app-state)
