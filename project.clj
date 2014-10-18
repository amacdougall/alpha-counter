;; project.clj and profiles.clj based on github.com/magomimmo/cljs-start.
(defproject alpha-counter "0.0.1-SNAPSHOT"
  :description "An Om life counter webapp for Yomi."
  :url "http://www.alanmacdougall.com/alphacounter"

  :min-lein-version "2.3.4"

  ; We need to add src/cljs too, because cljsbuild does not add its
  ; source-paths to the project source-paths
  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.8.0-alpha1"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :hooks [leiningen.cljsbuild]

  :cljsbuild
  {:builds {; This build is only used for including any cljs source
            ; in the packaged jar when you issue lein jar command and
            ; any other command that depends on it
            :alpha-counter
            {:source-paths ["src/cljs"]
             ; The :jar true option is not needed to include the CLJS sources
             ; in the packaged jar. This is because we added the CLJS source
             ; codebase to the Leiningen :source-paths

             ; :jar true

             ; Compilation Options
             :compiler
             {:output-to "dev-resources/public/js/alpha_counter.js"
              :optimizations :advanced
              :pretty-print false}}}})
; All other builds are in profiles.clj
