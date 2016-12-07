(ns expertus.components.login
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
            [expertus.load :refer [load-on load-off]]
            [expertus.alert :refer [add-alert-message]]))

;; Компонент аутентификации
(defn $get->profile 
  ;; Обработчик получения данных профиля 
  []
  (GET 
   (str "/profiles/expanded/" (-> @app-state :identity :login))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :handler
    (fn [res]
      (swap! app-state assoc :profile res))
    :error-handler
    (fn [res]
      (add-alert-message 
       res
       :title "Ваш профиль не найден"
       :message "Возможно вы аутентифицированы как администратор.
Если это не так свяжитесь с технической поддержкой!"
       :page :home))}))

(defn $get->account 
  ;; Обработчик получения данных аккаунта
  []
  (GET 
   (str "/accounts/" (-> @app-state :identity :login))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :handler
    (fn [res]
      (swap! app-state assoc :account res))
    :error-handler
    (fn [res]
      (add-alert-message res))}))

(defn $post->login 
  ;; Обработчик аутентификации и выдачи токена
  [login-data]
  (load-on)
  (POST 
   "/auth"
   {:params @login-data
    :keywords? true
    :response-format :json
    ;; Обработчик при успешном запросе
    :handler
    (fn [res]
      ;; Просмотреть токен авторизации и
      ;; добавить auth-token, login, role в app-state
      (do
        (let [token (:token res)]
          ;; Получить данные о токене
          (GET 
           (str "/auth/session/" token)
           {:keywords? true
            :response-format :json
            :handler
            (fn [res2] 
              ;; Обновить app-state
              (swap! app-state assoc
                     :identity 
                     (assoc (:result res2)
                       :auth-token token))
              ($get->account)
              ($get->profile))
            ;; Если произошла ошибка
            :error-handler
            (fn [res2]
              (add-alert-message res2))}))
        (load-off)))

    ;; Обработчик при неудачном запросе
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message 
       res 
       :title "Неверные имя пользователя или пароль"
       :message "Проверьте правильность введенных данных"))}))

(def login-form-template
  ;; Шаблон формы аутентификации
  [:div
   [:div.row
    [:div.col-xs-4
     (input "Логин" :text :login)]]
   [:div.row
    [:div.col-xs-4
     (input "Пароль" :password :password)]]])

(defn login-form
  "Форма аутентификации"
  []
  (let [login-data (atom nil)]
    (fn [] 
      [:div.row
       [:div.col-xs-12

        ;; Шаблон формы
        [bind-fields
         login-form-template
         login-data]]
       [:div.col-xs-12
        [:br]

        ;; Кнопка отправки данных
        [:button.btn.btn-primary
         (if (b/valid?
              @login-data
              :login v/required
              :password v/required)

           ;; Если данные корректны
           ;; Отправить POST запрос на получение
           ;; токена авторизации
           {:type "button"
            :on-click #($post->login login-data)}
           {:type "button"
            :class "disabled"})
         "Войти на сайт"]
        [:a.btn.btn-default
         {:role "button"
          :href "#/registration"}
         "Зарегистрироваться"]]])))

(defn login-ui-component 

  "Компонент аутентификации"

  []
  (if-not (auth?)
    [:div.container
     [:div.page-header
      [:h3 "Войти"]]
     [login-form]]
    (reset-hash "/")))
