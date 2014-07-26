(ns alpha-counter.views.life-counter
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [alpha-counter.channels :refer [running-total trickle]]
            [cljs.core.async :refer [>! <! chan put!]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;; Time before combo is considered complete, in ms.
(def combo-timeout 3000)

;; Sets :current to true on the supplied player, false on all others. Can be
;; used as a handler as (partial app player).
(defn- select-player [app player]
  (let [set-current #(assoc % :current (= % @player))]
    (om/transact! app :players #(mapv set-current %))))

;; Returns the current player, or player one.
(defn- get-current-player [app]
  (or (first (filter :current (:players app)))
      (first (:players app))))

; views

;; Health bar view. Expects props {:player p, :select-player fn}.
(defn- health-view [props owner]
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
    om/IInitState
    (init-state [_]
      (.log js/console "lcv/init-state")
      (let [hits (chan)
            combo (running-total hits combo-timeout)]
        {:hits hits, :combo combo}))
    om/IWillMount
    (will-mount [_]
      (let [hits (om/get-state owner :hits)
            combo (om/get-state owner :combo)]
        ; display running total from combo channel on screen;
        ; add grand total to opponent damage or self healing
        (go-loop []
          (let [v (<! combo)]
            (.log js/console "combo loop got %o" v (pr-str v))
            (if (= (first v) :running-total)
              (.log js/console "running total: %d" (second v))
              (.log js/console "grand total: %d" (second v))))
          (recur))))
    om/IRenderState
    ; TODO: figure out why there is no state when this first runs; You'd think
    ; that init-state would happen before render-state! That's just obvious.
    ; ...but that's totally what's happening. I might not understand the
    ; lifecycle as well as I thought. What's going on?
    (render-state [this {:keys [hits combo] :as state}]
      (.log js/console "lcv/render-state: state %o %s" state (pr-str state))
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
                 (dom/button
                   #js {:onClick #(put! hits n)}
                   n))
               (range 1 21)))))))
