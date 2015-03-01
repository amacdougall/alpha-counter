(ns alpha-counter.views.util
  (:require [clojure.string :as string]))

;; Returns the supplied string class names, without nils, as a space-separated
;; string suitable for use as a #js className. Class names may already include
;; spaces, naturally; (classes "inner button" "selected") will return "inner
;; button selected".
(defn classes [& cs]
  (string/join " " (remove nil? cs)))
