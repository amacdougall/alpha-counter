(defproject alpha-counter "0.9.1"
  :description "An Om life counter webapp for Yomi."
  :url "http://www.yomicounter.com"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [org.clojure/clojurescript "0.0-2740" :scope "provided"]
                 [ring "1.3.1"]
                 [compojure "1.2.0"]
                 [enlive "1.1.5"]
                 [om "0.7.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [figwheel "0.1.4-SNAPSHOT"]
                 [environ "1.0.0"]
                 [com.cemerick/piggieback "0.1.5"]
                 [weasel "0.5.0"]
                 [leiningen "2.5.0"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-environ "1.0.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "alpha-counter.jar"

  ; In the preamble/externs, note that the React files come from the Om jar,
  ; which is automatically added to the classpath. Libraries not provided in a
  ; CLJS-friendly format are in resources/vendor.
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src/cljs"]
     :compiler {:output-to     "resources/public/js/app.js"
                :output-dir    "resources/public/js/out"
                :source-map    "resources/public/js/out.js.map"
                :preamble      ["react/react.min.js"]
                :externs       ["react/externs/react.js"]
                :foreign-libs  [{:file "resources/vendor/fastclick/fastclick.min.js"
                                 :provides ["FastClick"]}]
                :optimizations :none
                :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:init-ns alpha-counter.server
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   ; TODO: upgrade to 2.3, which might not need piggieback/weasel
                   :plugins [[lein-figwheel "0.1.4-SNAPSHOT"]]

                   :figwheel {:http-server-root "public"
                              :port 3449
                              :css-dirs ["resources/public/css"]}

                   :env {:is-dev true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]}}}}

             :uberjar {:hooks [leiningen.cljsbuild]
                       :env {:production true}
                       :omit-source true
                       :aot :all
                       :cljsbuild {:builds {:app
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {:optimizations :advanced
                                              :pretty-print false}}}}}})
