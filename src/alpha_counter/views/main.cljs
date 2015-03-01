(ns alpha-counter.views.main
  (:require [alpha-counter.views.character-select :as character-select]
            [alpha-counter.views.life-counter :as life-counter]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

; Base
(defn main [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "content"}
        (if-not (:characters-selected app)
          (om/build character-select/main app)
          (om/build life-counter/main app))))))
