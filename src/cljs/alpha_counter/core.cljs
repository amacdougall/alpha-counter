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
     :players [{} {}]}))

; helpers
;; Initializes the player by selecting the character. Sets player health
;; to the character's max health, and empties player history.
(defn select-character [player character]
  (om/transact! player #(assoc % :character character
                                 :current false
                                 :health (:health character)
                                 :history [])))

(defn ready [app]
  (om/transact! app :ready (constantly true)))

;; Sets :current to true on the supplied player, false on all others. Can be
;; used as a handler as (partial app player).
(defn select-player [app player]
  (let [set-current #(assoc % :current (= % @player))]
    (om/transact! app :players #(mapv set-current %))))

;; Returns the current player, or player one.
(defn get-current-player [app]
  (or (first (filter :current (:players app)))
      (first (:players app))))

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

(defn health-view [props owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [player select-player]} props]
        (dom/div (when (:current player) #js {:className "selected"})
          (dom/button #js {:onClick #(select-player player)}
            (-> player :character :name))
          (dom/div #js {:className "health-display"}
            (dom/div #js {:className "damage"} "")
            (dom/div #js {:className "bar"} "")
            (dom/div #js {:className "number"} (:health player))))))))


(defn life-counter-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        ; player health bars
        (apply dom/div #js {:className "players"}
          (om/build-all health-view
            (mapv (fn [p] ; build health-view arguments per player
                    {:player p
                     :select-player (partial select-player app p)})
                  (:players app))))
        ; combo damage buttons
        (apply dom/div nil
          (map (fn [n]
                 ; TODO: feed these button clicks into a channel system instead
                 (dom/button
                   #js {:onClick #(.log js/console "life click: %d" n)}
                   n))
               (range 1 21)))))))

(defmulti main-view (fn [app _] (:ready app)))

(defmethod main-view false [app owner]
  (character-select-view app owner))

(defmethod main-view true [app owner]
  (life-counter-view app owner))

(om/root main-view app-state
  {:target (. js/document (getElementById "main"))})
