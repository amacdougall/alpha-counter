(ns alpha-counter.dev
    (:require
     [alpha-counter.core]
     [figwheel.client :as fw]))

(enable-console-print!)

(fw/start {
  :on-jsload (fn []
               ;; (stop-and-start-my app)
               )})
