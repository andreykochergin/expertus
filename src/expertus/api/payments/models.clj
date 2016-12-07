(ns expertus.api.payments.models
  (:require [monger.util :refer [object-id random-uuid]]
            [expertus.util :refer [datetime]]))

(defn payment
  "Модель платежа"
  [{:keys [login coins]}]
  {:login login
   :coins coins
   :uuid (random-uuid)
   :datetime (datetime)
   :_id (object-id)})
