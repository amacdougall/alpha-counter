(ns alpha-counter.channels
  (:require [cljs.core.async :refer [>! <! chan put! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)


;; Returns a channel which takes values from the input channel, and decomposes
;; them into a steady stream of 1s. Useful for making a damage counter go up
;; steadily instead of in chunks.
(defn trickle [in ms]
  (let [out (chan)]
    (go (loop [total 0]
          (if (= 0 total)
            ; await new value; set it as total
            (recur (<! in))
            ; await new value, or emit 1 and decrement total every tick
            (let [[v ch] (alts! [in (timeout ms)])]
              (if (= ch in)
                (recur (+ total v))
                (do
                  (>! out 1)
                  (recur (dec total))))))))
    out))

;; Returns a channel which takes values from the input channel, and receives
;; either running total vecs in the form [:running-total n] or, if the timeout
;; expires without an input value, a vec in the form [:total n].
(defn running-total [in ms]
  (let [out (chan)]
    ; TODO: only wait for timeout if (> total 0)
    (go (loop [total 0]
          (let [[v ch] (alts! (if (zero? total) [in] [in (timeout ms)]))]
            (if (= ch in)
              (do
                (>! out [:running-total (+ total v)])
                (recur (+ total v)))
              (do
                (>! out [:total total])
                (recur 0))))))
    out))
