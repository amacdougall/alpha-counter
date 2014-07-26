(ns alpha-counter.views.life-counter
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

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
