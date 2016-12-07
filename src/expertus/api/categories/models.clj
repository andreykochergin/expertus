;; Модель категории и функции для работы с ней
;; Задачи:
;; 1. Объявить структуру модели category
;; 2. Предоставить функции для работы с моделью
(ns expertus.api.categories.models
  (:require [monger.util :refer [object-id random-uuid]]
            [expertus.util :refer [datetime]]))

(defn category
  "Модель категории"
  [{:keys [alias title description m-img p-img]}]
  {:alias alias
   :title title
   :description description
   :mimg m-img
   :pimg p-img
   :_id (object-id)})
