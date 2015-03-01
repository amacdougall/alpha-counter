(ns alpha-counter.views.main
  (:require [alpha-counter.data :as data]
            [alpha-counter.abilities :as abilities]
            [alpha-counter.views.character-select :refer [character-select-view]]
            [alpha-counter.views.life-counter :refer [life-counter-view]]
            [cljs.core.async :refer [>! <! put! timeout]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

; Base
(defn main-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "content"}
        (if-not (:characters-selected app)
          (om/build character-select-view app)
          (om/build life-counter-view app))))))
