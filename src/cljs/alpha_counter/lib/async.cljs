;; Heavily based on David Nolen's blog.util.reactive;
;; https://github.com/swannodette/swannodette.github.com/blob/master/code/blog/src/blog/utils/reactive.cljs
(ns alpha-counter.lib.async
  (:refer-clojure :exclude [map filter remove distinct concat take-while])
  (:require [cljs.core.async :refer [>! <! chan put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

;; Pass each value through to the output channel, logging it out along the way.
(defn log [in]
  (let [out (chan)]
    (go-loop []
      (let [e (<! in)]
        (.log js/console e)
        (>! out e)))
    out))

;; Given a function f and an input channel... sets up a go block consisting of
;; a loop which takes elements from the input channel, applies f to them, and
;; puts the result in the output channel. Returns the output channel. When the
;; input channel is empty, the output channel is closed.
(defn map [f in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (>! out (f x))
              (recur))
            (close! out))))
    out))

;; Given a predicate function and an input channel, sets up a go block which
;; sends input values to the output channel only if they pass the predicate.
;; Returns the output channel, of course. I am noticing that the output
;; channel of each of these methods can serve as the input channel for
;; another one, which is a serious lightbulb moment.
(defn filter [pred in]
  (let [out (chan)]
    (go (loop []
          (if-let [x (<! in)]
            (do (when (pred x) (>! out x))
              (recur))
            (close! out))))
    out))

;; The inverse of filter. filter:select::remove:reject. But don't forget,
;; this and filter can both take sets, since sets are callable; this makes it
;; possible to just say (filter #{:a :b :c} in) to get an output channel
;; ignoring all other values.
(defn remove [f in]
  (let [out (chan)]
    (go (loop []
          (if-let [v (<! in)]
            (do (when-not (f v) (>! out v))
              (recur))
            (close! out))))
    out))

;; Returns an output channel which will contain each item in the xs seq, in
;; order, closing when none remain.
(defn spool [xs]
  (let [out (chan)]
    (go (loop [xs (seq xs)]
          (if xs
            (do (>! out (first xs))
              (recur (next xs)))
            (close! out))))
    out))

;; Given a predicate function, an input channel, and optionally a vector of two
;; output channels, sends results from the input to the output channels based
;; on the predicate. Returns a vector of the two output channels.
(defn split
  ([pred in] (split pred in [(chan) (chan)]))
  ([pred in [out1 out2]]
    (go (loop []
          (if-let [v (<! in)]
            (if (pred v)
              (do (>! out1 v)
                (recur))
              (do (>! out2 v)
                (recur))))))
    [out1 out2]))

;; Given a seq of xs and an input channel, places all the xs in the output
;; channel, then reads from the input channel to the output channel.
(defn concat [xs in]
  (let [out (chan)]
    (go (loop [xs (seq xs)]
          (if xs
            (do (>! out (first xs))
              (recur (next xs)))
            (if-let [x (<! in)]
              (do (>! out x)
                (recur xs))
              (close! out)))))
    out))

;; Reads from the input channel to the output channel, discarding sequential
;; identical items (but not enforcing overall uniqueness).
(defn distinct [in]
  (let [out (chan)]
    (go (loop [last nil]
          (if-let [x (<! in)]
            (do (when (not= x last) (>! out x))
              (recur x))
            (close! out))))
    out))

;; Given a seq of input channels and optionally an output channel, returns an
;; output channel which is given all items from all input channels. When an
;; input channel has a nil value, it is removed from the set of input channels.
;; The origin of the name was not obvious to me:
;; http://en.wikipedia.org/wiki/Fan-in
(defn fan-in
  ([ins] (fan-in ins (chan)))
  ([ins out]
    (go (loop [ins (vec ins)]
          (when (> (count ins) 0)
            (let [[x in] (alts! ins)]
              (when x
                (>! out x)
                (recur ins))
              (recur (vec (disj (set ins) in))))))
        (close! out))
    out))

;; Takes values from the input channel until the predicate function returns
;; true. The function is checked after a value is taken.
(defn take-until
  ([pred-sentinel in] (take-until pred-sentinel in (chan)))
  ([pred-sentinel in out]
    (go (loop []
          (if-let [v (<! in)]
            (do
              (>! out v)
              (if-not (pred-sentinel v)
                (recur)
                (close! out)))
            (close! out))))
    out))

;; Takes all values from the input channel, conj-ing them onto the collection.
(defn siphon
  ([in] (siphon in []))
  ([in coll]
    (go (loop [coll coll]
          (if-let [v (<! in)]
            (recur (conj coll v))
            coll)))))

;; Given a value and an input channel, takes values from the input channel, but
;; puts the value in the output channel. A channel version of constantly.
(defn always [v c]
  (let [out (chan)]
    (go (loop []
          (if-let [e (<! c)]
            (do (>! out v)
              (recur))
            (close! out))))
    out))

;; Given an input channel, returns a hash with :chan and :control channels. The
;; most recent value taken from the control channel determines whether values
;; taken from the input channel are put in the output channel. In other words,
;; put true in the control channel to start getting output; put false in the
;; control channel to turn it off.
(defn toggle [in]
  (let [out (chan)
        control (chan)]
    (go (loop [on true]
          (recur
            (alt!
              in ([x] (when on (>! out x)) on)
              control ([x] x)))))
    {:chan out
     :control control}))

;; Given a seq of channels, waits until each channel has received a value, and
;; then returns a vec of each value. Good for synchronizing events.
(defn barrier [cs]
  (go (loop [cs (seq cs) result []]
        (if cs
          (recur (next cs) (conj result (<! (first cs))))
          result))))

;; Same thing, but keeps going.
(defn cyclic-barrier [cs]
  (let [out (chan)]
    (go (loop []
          (>! out (<! (barrier cs)))
          (recur)))
    out))
