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

;; Returns a cursor for the current player, or for player one.
(defn- get-current-player [app]
  (or (first (filter :current (:players app)))
      (first (:players app))))

;; Returns a cursor for the non-current player, or for player two.
(defn- get-opponent [app]
  (or (first (remove :current (:players app)))
      (second (:players app))))

(defn- damage [app n]
  ; TODO: I wouldn't give this code to a dog to compile.
  (let [apply-damage (fn [p]
                       ; is there an assoc-if or something? This is still mad ugly.
                       (if (= p (get-opponent app))
                         (assoc p :health (- (:health p) n))
                         p))]
    ; useful to use (om/transact app :players (fn [players] ...? I actually
    ; just want to update the health of a specific player anyway, but I have
    ; to rewrite the players map to get that to happen to the app... no mutable
    ; state. This kills mainly because life-counter-view only gets an app cursor.
    ; Lightbulb moment: maybe I can re-render it based on the current player?
    ; Or render just the damage buttons based on the current player? It could be
    ; really handy if the damage buttons were a view of their own.
    (om/transact! app (fn [app]
                        (assoc app :players (mapv apply-damage (:players app)))))))

(defn- heal [app n]
  (om/transact! app #(assoc % :health (+ (:health %) n))))

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
      (let [hits (chan)
            combo (running-total hits combo-timeout)]
        {:hits hits, :combo combo}))
    om/IWillMount
    (will-mount [_]
      (let [hits (om/get-state owner :hits)
            combo (om/get-state owner :combo)
            damage (partial damage app)
            heal (partial heal app)]
        ; display running total from combo channel on screen;
        ; add grand total to opponent damage or self healing
        (go-loop []
          (let [[k v] (<! combo)]
            (condp = k
              :running-total
              (.log js/console "running total: %d" v)
              :grand-total
              (if (> v 0)
                (do
                  (.log js/console "dealing %d damage" v)
                  (damage v))
                (heal v))))
          (recur))))
    om/IRenderState
    (render-state [this {:keys [hits combo] :as state}]
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
