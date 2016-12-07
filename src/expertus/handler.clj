(ns expertus.handler
  (:require [compojure.core :refer [defroutes routes wrap-routes GET]]
            [compojure.route :as route]
            [expertus.layout :refer [error-page]]
            [expertus.auth :refer [auth-routes]]
            
            ;; Модули
            [expertus.api.core :refer [api-routes]]

            [expertus.middleware :as middleware]
            [expertus.layout :as layout]
            [ring.util.http-response :as resp]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []

  (timbre/merge-config!
    {:level     (if (env :dev) :trace :info)
     :appenders {:rotor (rotor/rotor-appender
                          {:path "expertus.log"
                           :max-size (* 512 1024)
                           :backlog 10})}})

  (if (env :dev) (parser/cache-off!))
  (timbre/info (str
                 "\n-=[expertus started successfully"
                 (when (env :dev) " using the development profile")
                 "]=-")))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "expertus is shutting down...")
  (timbre/info "shutdown complete!"))

(defroutes base-routes
  (GET "/" [] (layout/render "index.html"))
  (route/resources "/")
  (route/not-found (resp/not-found 
                    {:status 404
                     :message "page not found"})))

(def app-routes
  (routes
    (var api-routes)
    base-routes))

(def app (middleware/wrap-base #'app-routes))
