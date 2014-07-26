(ns alpha-counter.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [alpha-counter.views.character-select :refer [character-select-view]]
            [alpha-counter.views.life-counter :refer [life-counter-view]]))

(enable-console-print!)

(def app-state
  (atom
    {:ready false ; when true, displays the main life counter
     :players [{} {}]}))

(defn main-view [app owner]
  (dom/div nil
    (if-not (:ready app)
      (om/build character-select-view app)
      (om/build life-counter-view app))))
; NOTE: gotta om/build each thing individually if we want to run init-state; we
; can't just call (desired-view app owner).

(om/root main-view app-state
  {:target (. js/document (getElementById "main"))})
