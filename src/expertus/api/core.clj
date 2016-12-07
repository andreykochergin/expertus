(ns expertus.api.core
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :as resp]
            [expertus.auth :refer [auth-routes]]
            [expertus.api.accounts.handler :refer [accounts-routes]]
            [expertus.api.profiles.handler :refer [profiles-routes]]
            [expertus.api.categories.handler :refer [categories-routes]]
            [expertus.api.locations.handler :refer [locations-routes]]
            [expertus.api.payments.handler :refer [payments-routes]]))

(defapi api-routes
  (ring.swagger.ui/swagger-ui "/swagger")
  ;JSON docs available at the /swagger.json route
  (swagger-docs
   {:info
    {:title "Expertus REST API"
     :description "Расширенная документация по системе Expertus"
     :version "0.3.6"}})
  ;; Expertus core api-routes
  auth-routes
  accounts-routes
  profiles-routes
  categories-routes
  locations-routes
  payments-routes)
