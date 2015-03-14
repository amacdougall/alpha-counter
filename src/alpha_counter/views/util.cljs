(ns alpha-counter.views.util
  (:require [sablono.core :as html :refer-macros [html]]
            [clojure.string :as string]))

;; Returns the supplied string class names, without nils, as a space-separated
;; string suitable for use as a #js className. Class names may already include
;; spaces, naturally; (classes "inner button" "selected") will return "inner
;; button selected".
(defn classes [& cs]
  (string/join " " (remove nil? cs)))


;; A helper which produces a Sablono-like vector. Provide a container in
;; Sablono format, and a vector of children produced by om/build-all.
;;
;; Example:
;; (html-container
;;   [:div {:class 'parent'}] (om/build-all component data))
(defn html-container [container children]
  (html (vec (concat container children))))
