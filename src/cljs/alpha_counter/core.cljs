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
  (om/transact! app :ready (constantly true)))

(defn select-player [app player]
  ; Since player is already a cursor, we need to set its value. Is this going
  ; to be an ongoing pain point?
  (om/transact! app #(assoc % :current-player (om/value player))))

(defn is-current-player [app player]
  (let [current-player (or (:current-player app) (first (:players app)))]
    (= player current-player)))

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

(defn life-counter-view [app owner]
  (let [p1 (-> app :players first)
        p2 (-> app :players second)]
    (dom/div nil
      (dom/div #js {:className "players"}
        ; TODO: a single character health component?
        (dom/div (when (is-current-player app p1) #js {:className "selected"})
          (dom/button #js {:onClick #(select-player app p1)}
            (-> p1 :character :name))
          (dom/div #js {:className "health-display"}
            (dom/div #js {:className "damage"} "")
            (dom/div #js {:className "bar"} "")
            (dom/div #js {:className "number"} (:health p1))))
        (dom/div (when (is-current-player app p2) #js {:className "selected"})
          (dom/button #js {:onClick #(select-player app p2)}
            (-> p2 :character :name))
          (dom/div #js {:className "health-display"}
            (dom/div #js {:className "damage"} "")
            (dom/div #js {:className "bar"} "")
            (dom/div #js {:className "number"} (:health p2))))))))


(defmulti main-view (fn [app _] (:ready app)))

(defmethod main-view false [app owner]
  (character-select-view app owner))

(defmethod main-view true [app owner]
  (life-counter-view app owner))

(om/root main-view app-state
  {:target (. js/document (getElementById "main"))})

;; scratch below this line

; (:current-player @app-state)
