(ns expertus.state
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET]]))

;; Состояние приложения
(defonce app-state 
  (atom 
   {;; :identity {:login "" :role "" :auth-token "" :addr "" :exp ""}
    :identity nil ;; Данные пользователя для авторизации
    :account nil ;; Данные аккаунта
    :profile nil ;; Данные профиля
    
    ;; Данные приложения
    :app-data 
    {:categories "" ;; Список категорий
     :locations ""  ;; Список локаций
     
     ;; Данные для раздела управления
     :admin

     ;; Пользовательский профиль
     ;; редактируемый администратором
     ;; используется в обрабочтках profile
     ;; для подмены логина админа
     ;; на тот в котором необходимо
     ;; изменить данные
     {:edited-profile-login nil}}
    
    ;; Статус сообщения от обработчика
    :alert nil}))

;; Получить список категорий и локаций
(defn set-categories! []
  (GET 
   "/categories"
   {:keywords? true
    :response-format :json
    :handler
    (fn [res]
      (swap! app-state assoc-in
             [:app-data :categories]
             (:result res)))
    :error-handler
    (fn [res]
      (js/alert res))}))

(defn set-locations! []
  (GET 
   "/locations"
   {:keywords? true
    :response-format :json
    :handler
    (fn [res]
      (swap! app-state assoc-in
             [:app-data :locations]
             (:result res)))
    :error-handler
    (fn [res]
      (js/alert res))}))
