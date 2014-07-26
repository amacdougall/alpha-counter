(ns alpha-counter.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan >! <! alts! put! timeout close!]]
            [alpha-counter.lib.async :as async]
            [alpha-counter.channels :refer [trickle running-total]]
            [alpha-counter.views.character-select :refer [character-select-view]]
            [alpha-counter.views.life-counter :refer [life-counter-view]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(def app-state
  (atom
    {:ready false ; when true, displays the main life counter
     :players [{} {}]}))


(defmulti main-view (fn [app _] (:ready app)))

(defmethod main-view false [app owner]
  (character-select-view app owner))

(defmethod main-view true [app owner]
  (life-counter-view app owner))

(om/root main-view app-state
  {:target (. js/document (getElementById "main"))})

; scratch

; (def hp-in (chan))
; (def hp (trickle hp-in 50))
; (def combo (running-total hp 5000))
; 
; (def output-test
;   (go (loop []
;         (when-let [v (<! combo)]
;           (do
;             (.log js/console v)
;             (recur))))))
; 
; (doseq [n [2 3 5 8 13]]
;   (put! hp-in n))
