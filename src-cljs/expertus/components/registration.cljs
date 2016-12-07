(ns expertus.components.registration
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [reagent-forms.core :refer [bind-fields]]
            [ajax.core :refer [GET POST]]
            [cljs-time.core :as time]
            [cljs-time.format :as timef]
            [cljs-time.coerce :as timec]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [expertus.state :refer [app-state]]
            [expertus.auth :refer [auth?]]
            [expertus.forms :refer [input row]]
            [expertus.routes :refer [reset-hash]]
            [expertus.alert :refer [add-alert-message]]
            [expertus.load :refer [load-on load-off]]))

;; --------------------------
;; Обработчики обращений к API

(defn $post->reg 
  "Запрос к API /registration POST"
  [reg-data]
  (load-on)
  (POST 
   "/accounts"
   {:keywords? true
    :response-format :json
    :params @reg-data
    :handler
    (fn [res]
      (reset-hash "/login")
      (load-off)
      (add-alert-message 
       res
       :title "Ваш аккаунт создан"
       :message "Войдите на сайт используя логин и
пароль и добавьте данные о себе в свой профиль."
       :page :login))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message 
       res
       :title "Логин или почтовый адрес уже используются"
       :message "Аккаунт с таким логином или почтовым 
адресом уже существует, укажите другие данные"))}))




;; -------------------------
;; UI элементы компонента Registration

(defn registration-header
  "Шаблон заголовка"
  []
  [:div.page-header
   [:h3 "Регистрация"]])

(defn registration-wrap
  "Шаблон структуры UI компонента"
  [body]
  (if-not (auth?)
    [:div.container
     [registration-header]
     [:div.row
      [:div.col-xs-12
       (body)]]]
    (reset-hash "/")))

(def registration-form-template
  [:div
   [:div.row
    [:div.col-xs-4
     (input "Логин" :text :login)]]
   [:div.row
    [:div.col-xs-4
     (input "Email" :text :email)]]
   [:div.row
    [:div.col-xs-4
     (input "Пароль" :password :password)]]
   [:div.col-xs-12
    [:br] 
    [:br]]
   [:h5 "Выберите тип аккаунта"]
   [:div.btn-group {:field :single-select :id :role}
    [:button.btn.btn-default {:key "expert"} "Тренер"]
    [:button.btn.btn-default {:key "company"} "Клуб"]
    [:button.btn.btn-default {:key "store"} "Магазин"]
    [:button.btn.btn-default {:key "headhunter"} "Работодатель"]]])

(defn registration-action-subpage
  "UI регистрации пользователя"
  []
  (let [reg-data (atom nil)] 
    (fn [] 
      [:div.row
       ;; Форма регистрации
       [:div.col-xs-12
        [bind-fields
         registration-form-template
         reg-data]]
       ;; Кнопка отправки данных
       [:div.col-xs-12
        [:br]
        [:button.btn.btn-success
         ;; Валидация данных
         (if (b/valid?
              @reg-data
              :login v/required
              :email v/required
              :role v/required
              :password v/required)
           ;; Если данные корректны
           ;; Отправить POST запрос на получение
           ;; токена авторизации
           {:type "button"
            :on-click #($post->reg reg-data)}
           {:type "button"
            :class "disabled"})
         "Перейти к заполнению профлия"]]])))

(defn registration-ui-component 
  "Компонент регистрации пользователя"
  []
  [registration-wrap
   
   (fn [] 
     
     ;; Определить подстраницу
     (condp = (session/get :subpage)
       
       ;; Форма регистрации
       :registration/action
       [registration-action-subpage]))])
