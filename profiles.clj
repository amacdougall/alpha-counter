;; ****************************** NOTES ******************************
;; Defines four profiles:
;;
;; - :shared
;; - :dev
;; - :simple
;; - :advanced
;;
;; the :dev, :simple and :advanced profiles are composite profiles,
;; meaning that they share the content of :shared profile.
;; *******************************************************************

{:shared {:clean-targets ["out" :target-path]
          :test-paths ["test/clj" "test/cljs"]
          :resources-paths ["dev-resources"]
          :plugins [[com.cemerick/clojurescript.test "0.2.1"]]
          :cljsbuild
          {:builds {:alpha-counter
                    {:source-paths ["test/cljs"]
                     :compiler
                     {:output-dir "dev-resources/public/js/main"
                      :source-map "dev-resources/public/js/main/alpha_counter.js.map"}}}
           :test-commands {"phantomjs"
                           ["phantomjs" :runner "dev-resources/public/js/main/alpha_counter.js"]}}}
 :dev [:shared
       {:source-paths ["dev-resources/tools/http" "dev-resources/tools/repl"]
        :dependencies [[ring "1.3.1"]
                       [compojure "1.2.0"]
                       [enlive "1.1.4"]]
        :plugins [[com.cemerick/austin "0.1.5"]]
        :cljsbuild
        {:builds {:alpha-counter
                  {:source-paths ["dev-resources/tools/repl"]
                   :compiler
                   {:optimizations :whitespace
                    :pretty-print true}}}}

        :injections [(require '[ring.server :as http :refer [run]]
                              'cemerick.austin.repls)
                     (defn browser-repl []
                       (cemerick.austin.repls/cljs-repl (reset! cemerick.austin.repls/browser-repl-env
                                                                (cemerick.austin/repl-env))))]}]
 ;; simple profile.
 :simple [:shared
          {:cljsbuild
           {:builds {:alpha-counter
                     {:compiler {:optimizations :simple
                                 :pretty-print false}}}}}]
 ;; advanced profile
 ; TODO: get advanced optimizations to work with Om; this build is provided by
 ; cljs-start, which is library-agnostic. Om might require extra juice.
 :advanced [:shared
            {:cljsbuild
             {:builds {:alpha-counter
                       {:compiler
                        {:optimizations :advanced
                         :pretty-print false
                         :preamble ["react/react.min.js"]}}}}}]}
                         ; contrary to the Om docs, no :externs was needed for me

