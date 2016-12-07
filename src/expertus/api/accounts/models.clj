;; Модель пользователя и функции для работы с ней
;; Задачи:
;; 1. Объявить структуру модели account
;; 2. Предоставить функции для работы с моделью
(ns expertus.api.accounts.models
  (:require [monger.util :refer [object-id random-uuid]]
            [expertus.util :refer [nowf]]))

(defn account
  "Модель пользователя"
  [{:keys [login email role password profile]}]
  {:login login ;; псевдоним
   :email email ;; почтовый адрес
   :role role ;; тип аккаунта
   :password password ;; пароль
   :mailing false ;; подписка на новости
   :coins 0 ;; денежный счет
   :registered (nowf :date-time) ;; дата регистрации
   :updated (nowf :date-time) ;; последнее обновление
   :visited (nowf :date-time) ;; последнее посещение
   :uuid (random-uuid) ;; уникальный идентификатор
   :profile (or profile false)  ;; профиль
   :transactions [] ;; транзакции пользователя
   :_id (object-id)})
