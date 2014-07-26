(ns alpha-counter.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [chan >! <! alts! put! timeout close!]]
            [alpha-counter.lib.async :as async]
            [alpha-counter.channels :refer [trickle running-total]]
            [alpha-counter.views.character-select :refer [character-select-view]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(def app-state
  (atom
    {:ready false ; when true, displays the main life counter
     :players [{} {}]}))

; helpers

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

;; Health bar view. Expects props {:player p, :select-player fn}.
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
