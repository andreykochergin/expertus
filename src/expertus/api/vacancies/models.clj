(ns expertus.api.vacancies.models
  (:require [monger.util :refer [object-id random-uuid]]
            [expertus.util :refer 
             [unparse-datetime nowf datetime+months]]
            [clj-time.core :as time]))

(defn vacancy
  "Модель вакансии"
  [{:keys 
    [owner title content categories
     locations keywords above sum
     employment]}]
  {:owner owner ;; логин автора вакансии
   :title title ;; заголовок
   :content content ;; описание
   :categories categories ;; специализации
   :locations locations ;; места
   :keywords keywords ;; хэштеги
   :salary ;; зарплата
   {:above above
    :sum sum}
   :employment employment ;; тип занятости  
   :created (nowf)
   :updated nil
   :_id (object-id)})
