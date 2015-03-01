(ns alpha-counter.core
  (:require [alpha-counter.data :as data]
            [alpha-counter.views.main :as views]
            [om.core :as om :include-macros true]
            [FastClick]))

(enable-console-print!)

(defn main []
  (.attach js/FastClick (.-body js/document))
  (om/root views/main
    data/app-state
    {:target (. js/document (getElementById "app"))}))

(main)
