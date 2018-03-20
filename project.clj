(defproject alpha-counter "0.9.2"
  :description "An Om life counter webapp for Yomi."
  :url "http://www.yomicounter.com"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [sablono "0.3.4"]
                 [org.omcljs/om "0.8.8"]
                 [org.clojure/core.async "0.4.474"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.15"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"]
  
  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src" "dev_src"]
              :compiler {:output-to "resources/public/js/compiled/alpha_counter.js"
                         :output-dir "resources/public/js/compiled/out"
                         :optimizations :none
                         :main alpha-counter.dev
                         :asset-path "js/compiled/out"
                         :source-map true
                         :source-map-timestamp true
                         :foreign-libs [{:file "resources/vendor/fastclick/fastclick.min.js" :provides ["FastClick"]}]
                         :cache-analysis true }}
             {:id "prod"
              :source-paths ["src"]
              :compiler {:output-to "resources/public/js/compiled/alpha_counter.js"
                         :optimizations :advanced
                         :main alpha-counter.core
                         :foreign-libs [{:file "resources/vendor/fastclick/fastclick.min.js" :provides ["FastClick"]}]
                         :externs ["resources/vendor/fastclick/fastclick.js"]
                         :pretty-print false
                         :closure-warnings {:externs-validation :off
                                            :non-standard-jsdoc :off}}}]}

  :figwheel {:http-server-root "public" ;; default and assumes "resources"
             :server-port 3449 ;; default
             :css-dirs ["resources/public/css"]}) ;; watch and update CSS
