;; Модель локации и функции для работы с ней
;; Задачи:
;; 1. Объявить структуру модели location
;; 2. Предоставить функции для работы с моделью
(ns expertus.api.locations.models
  (:require [monger.util :refer [object-id random-uuid]]
            [expertus.util :refer [datetime]]))

(defn location
  "Модель категории"
  [{:keys [alias title type]}]
  {:alias alias
   :title title
   :type type
   :_id (object-id)})
