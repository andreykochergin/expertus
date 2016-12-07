(ns expertus.api.profiles.models
  (:require [monger.util :refer [object-id random-uuid]]
            [expertus.util :refer 
             [unparse-datetime nowf datetime+months]]
            [clj-time.core :as time]))

(defn base-profile-fields
  "Модель профиля"
  [login role]
  {:login login ;; логин аккаунта
   :role role ;; тип профиля
   :about ""
   :avatar ""
   :categories []
   :locations []
   :services []
   :keywords []
   :contacts []
   :subscription 
   (unparse-datetime 
    (datetime+months 1)
    :date-time) ;; подписка (месяц бесплатно)
   :updated (nowf :date-time) ;; последнее обновление
   :files [] ;; ссылки на медиа-файлы
   :uuid (random-uuid) ;; уникальный идентификатор
   :_id (object-id)})

(defn headhunter
  [login]
  (merge
   {:first-name ""
    :last-name ""
    :address ""
    :company {:name ""}}
   (base-profile-fields login :headhunter)))

(defn store
  [login]
  (merge
   {:name ""
    :address ""}
   (base-profile-fields login :store)))

(defn company
  [login]
  (merge
   {:name ""
    :address ""}
   (base-profile-fields login :company)))

(defn expert
  [login]
  (merge 
   {:first-name ""
    :last-name ""
    :age ""
    :experience ""
    :education ""
    :job ""
    :price []}
   (base-profile-fields login :expert)))
