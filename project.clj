(defproject expertus "0.4.5-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [selmer "0.9.1"]
                 [com.taoensso/timbre "4.1.1"]
                 [com.taoensso/tower "3.0.2"]
                 [markdown-clj "0.9.71"]
                 [environ "1.0.0"]
                 [compojure "1.4.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.1.5"]
                 [ring-ttl-session "0.1.1"]
                 [ring "1.4.0"
                  :exclusions [ring/ring-jetty-adapter]]
                 [metosin/ring-middleware-format "0.6.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [bouncer "0.3.3"]
                 [prone "0.8.2"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [org.webjars/bootstrap "3.3.5"]
                 [org.webjars/jquery "2.1.4"]
                 [buddy "0.6.2"]
                 [clj-time "0.11.0"]
                 [com.novemberain/monger "2.1.0"]
                 [org.clojure/clojurescript "1.7.122" :scope "provided"]
                 [org.clojure/tools.reader "0.9.2"]
                 [reagent "0.5.1" ; :exclusions [cljsjs/react]
                  ]
                 ; [cljsjs/react-with-addons "0.13.3-0"]
                 ; [cljsjs/react "0.13.3-1"]
                 [reagent-forms "0.5.11"]
                 [reagent-utils "0.1.5"]
                 [org.omcljs/om "0.9.0"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.andrewmcveigh/cljs-time "0.3.13"]
                 [cljs-ajax "0.5.0"]
                 [metosin/compojure-api "0.23.1"]
                 [metosin/ring-swagger-ui "2.1.2"]
                 [org.immutant/web "2.1.0"]]

  :min-lein-version "2.0.0"
  :uberjar-name "expertus.jar"
  :jvm-opts ["-server"]

  :main expertus.core

  :plugins [[lein-environ "1.0.1"]
            [lein-cljsbuild "1.1.0"]]
  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src-cljs"]
     :compiler
     {:output-to "resources/public/js/app.js"
      :externs ["react/externs/react.js"]
      :pretty-print true}}}}
  
  :profiles
  {:uberjar {:omit-source true
             :env {:production true}
             :hooks [leiningen.cljsbuild]
             :prep-tasks ["compile" ["cljsbuild" "once"]]
             :aot :all
             :cljsbuild
             {:builds
              {:app
               {:source-paths ["env/prod/cljs"]
                :compiler {:optimizations :advanced :pretty-print false}}}}}
   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :project/dev  {:dependencies [[ring/ring-mock "0.2.0"]
                                 [ring/ring-devel "1.4.0"]
                                 [pjstadig/humane-test-output "0.7.0"]
                                 [lein-figwheel "0.3.9"]
                                 [org.clojure/tools.nrepl "0.2.10"]]
                  :plugins [[lein-figwheel "0.3.9"]]
                   :cljsbuild
                   {:builds
                    {:app
                     {:compiler 
                      {:source-map true} 
                      :source-paths ["env/dev/cljs"]}}} 
                  
                  :figwheel
                  {:http-server-root "public"
                   :server-port 3449
                   :nrepl-port 7002
                   :css-dirs ["resources/public/css"]
                   :ring-handler expertus.handler/app}
                  
                  :repl-options {:init-ns expertus.core}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]
                  ;;when :nrepl-port is set the application starts the nREPL server on load
                  :env {:dev        true
                        :port       3000
                        :nrepl-port 7000}}
   :project/test {:env {:test       true
                        :port       3001
                        :nrepl-port 7001}}
   :profiles/dev {}
   :profiles/test {}})
