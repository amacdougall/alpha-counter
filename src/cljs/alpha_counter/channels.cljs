(ns alpha-counter.channels
  (:require [cljs.core.async :refer [>! <! chan put! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)


;; Returns a channel which takes values from the input channel, and decomposes
;; them into a steady stream of 1s or -1s. Useful for making a damage counter go up
;; steadily instead of in chunks.
(defn trickle [in ms]
  (let [out (chan)]
    (go-loop [total 0]
      (if (zero? total)
        ; await new value; set it as total
        (recur (<! in))
        ; await new value, or tick toward 0 by emitting a 1 or -1
        (let [[v ch] (alts! [in (timeout ms)])]
          (if (= ch in)
            ; new value: add to total and restart timeout
            (recur (+ total v))
            ; timeout: tick toward 0 by incrementing or decrementing
            (let [n (if (pos? total) 1 -1)]
              (>! out n)
              (recur (- total n)))))))
    out))

;; Returns a channel which takes values from the input channel, and receives
;; the running total after each input. For instance, given the values 10, 20,
;; 30, receives the values 10, 30, 50. After the timeout, receives :reset, and
;; the total is reset to 0.
(defn running-total [in ms]
  (let [out (chan)]
    (go-loop [total 0]
      (if (zero? total)
        ; await new input and make it the start of a running total
        (let [new-total (+ total (<! in))]
          (>! out new-total)
          (recur new-total))
        ; accumulate incoming value and output running total, or time out
        (let [[n ch] (alts! [in (timeout ms)])]
          (if (= ch in)
            ; new value: output running total and await next input
            (let [new-total (+ total n)]
              (>! out new-total)
              (recur new-total))
            ; timeout: reset
            (do
              (>! out :reset)
              (recur 0))))))
    out))

;; Returns a channel which, after an ms delay, receives the total of all input
;; values. After the timeout, the total is reset to 0.
(defn delayed-total [in ms]
  (let [out (chan)]
    (go-loop [total 0]
      (if (zero? total)
        ; await new value
        (recur (<! in))
        ; accumulate incoming value, or deliver total after timeout
        (let [[n ch] (alts! [in (timeout ms)])]
          (if (= ch in)
            (recur (+ total n))
            (do
              (>! out total)
              (recur 0))))))
    out))
