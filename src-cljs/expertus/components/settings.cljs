(ns expertus.components.settings
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [reagent-forms.core :refer [bind-fields]]
            [ajax.core :refer [GET POST PUT DELETE]]
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
            [expertus.load :refer [load-on load-off]]
            [expertus.util :as util]))

(defn refresh-account!
  "Обновить данные аккаунта"
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

;; Обработчики запросов к API
(defn $put->change-email
  "Обработчик изменения email аккаунта"
  [email-data]
  (load-on)
  (PUT
   (str "/accounts/" (-> @app-state :identity :login))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :params @email-data
    :handler
    (fn [res]
      (refresh-account!)
      (load-off)
      (add-alert-message
       res
       :title "Почтовый адрес изменен"
       :message "Почтовый адрес вашего аккаунта изменен"))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message 
       res
       :title "Не удалось изменить почтовый адрес"
       :message "Проверьте валидность введенных данных"))}))

(defn $put->change-password 
  "Обработчик изменения пароля аккаунта"
  [password-data]
  (load-on)
  (PUT
   (str "/accounts/change-password/" (-> @app-state :identity :login))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :params @password-data
    :handler
    (fn [res]
      (reset! password-data nil)
      (load-off)
      (add-alert-message
       res
       :title "Пароль изменен"
       :message "Пароль вашего аккаунта изменен"))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message 
       res
       :title "Не удалось изменить пароль"
       :message "Проверьте правильность старого пароля и корректность нового"))}))

(defn $post->payment [balance-data]
  ;; Обработчик запроса на пополнение баланса
  (load-on)
  (POST
   "/payments"
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :params 
    (let [coins (long (:coins @balance-data))] 
      {:coins coins
       :login (-> @app-state :identity :login)})
    :handler
    (fn [res]
      (reset! balance-data nil)
      (refresh-account!)
      (load-off)
      (add-alert-message 
       res
       :title "Обработка запроса"
       :message "Вам необходимо оплатить сумму платежа через 
платежную систему, после средства будут зачислены на ваш баланс"))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message 
       res))}))

;; ---------------------
;; UI элементы компонента Settings

(defn settings-navbar 
  "Меню навигации"
  []
  [:div.row
   [:div.col-xs-12
    [:ul.nav.nav-pills
     [:li
      {:class (when (= :settings/home (session/get :subpage)) "active")}
      [:a 
       {:href "#/settings"} 
       "Основная информация"]]
     (if-not (= :admin (keyword (-> @app-state :identity :role)))
       ;; Скрыть если пользователь админ
       [:li 
        {:class (when (= :settings/balance (session/get :subpage)) "active")}
        [:a 
         {:href "#/settings/balance"} 
         "Баланс"]])
     (if-not (= :admin (keyword (-> @app-state :identity :role)))
       [:li 
        {:class (when (= :settings/subscription (session/get :subpage)) "active")}
        [:a 
         {:href "#/settings/subscription"} 
         "Подписка"]])
     [:li 
      {:class (when (= :settings/email (session/get :subpage)) "active")}
      [:a 
       {:href "#/settings/email"} 
       "Почтовый адрес"]]
     [:li 
      {:class (when (= :settings/password (session/get :subpage)) "active")}
      [:a 
       {:href "#/settings/password"} 
       "Пароль"]]]]])

(defn settings-header
  "Заголовок компонента настроек"
  []
  [:div.page-header
   [:h3 "Настройки"]])

(defn settings-wrap
  "Шаблон структуры UI компонента"
  [body]
  (if (auth?)
    [:div.container
     [settings-header]
     [:div.row
      [:div.col-xs-2
       [settings-navbar]]
      [:div.col-xs-10
       (body)]]]
    (reset-hash "/login")))



;; -------------------------------
;; UI пополнения баланса

(def balance-form-template
  [:div.row
   [:div.col-xs-12
    [:div.row
     [:div.col-xs-2
      (input "Количество монет" :text :coins)]]]])

(defn settings-balance-subpage
  "UI пополнения баланса"
  []
  (let [balance-data (atom nil)] 
    (fn [] 
      [:div.row
       [:div.col-xs-12
        ;; Форма пополнения баланса
        [:h4 "Пополнить баланс"]
        [:hr]
        [:div.row
         [:div.col-xs-3
          [:strong "Текущий баланс: "]
          [:span (get-in @app-state [:account :coins])]
          [:del "М"]
          [:br]
          [:br]]]
        [bind-fields
         balance-form-template
         balance-data]
        ;; Конвертация в монеты если указана сумма
        (if (b/valid? @balance-data :coins v/required)
          [:div
           [:br]
           [:strong "Сумма платежа: "]
           ;; Курс монет к рублю
           (* (int (:coins @balance-data)) 100)
           [:del "Р"]])
        [:br]
        ;; Отправика данных
        [:button.btn.btn-success
         (if (b/valid? @balance-data :coins v/required)
           {:type "button"
            ;; временное решение
            :on-click #($post->payment balance-data)} 
           {:type "button"
            :class "disabled"})
         "Перейти к оплате"]
        [:br]
        [:br]]])))



;; ------------------------
;; UI продления подписки

(def subscription-form-template
  [:div.row
   [:div.col-xs-12
    [:h5 "Выберите пакет продления подписки"]
    [:div.btn-group {:field :single-select :id :mounths}
     [:button.btn.btn-default {:key 1} "1 месяц"]
     [:button.btn.btn-default {:key 3} "3 месяца"]
     [:button.btn.btn-default {:key 6} "6 месяцев"]
     [:button.btn.btn-default {:key 12} "12 месяцев"]]]])

(defn settings-subscription-subpage
  "UI продления подписки"
  []
  (let [subscription-data (atom nil)] 
    (fn []
      [:div.row
       [:div.col-xs-10
        [:h4 "Продлить подписку"]
        [:hr]
        [:div.row
         [:div.col-xs-12
          [:strong "Текущая подписка до: "]
          [:span (-> @app-state :profile :subscription)]
          [:br]
          [:br]]]
        [bind-fields
         subscription-form-template
         subscription-data]
        ;; Отобразить цену подписки если она выбрана
        (if (b/valid? @subscription-data :mounths v/required)
          [:div
           [:br]
           [:strong "Стоимость: "]
           (str (* (:mounths @subscription-data) 5))
           [:del "М"]])
        [:br]
        ;; Отправка данных
        [:button.btn.btn-success
         (if (b/valid? @subscription-data :mounths v/required)
           {:type "button"
            :on-click #()}
           {:type "button"
            :class "disabled"})
         "Продлить подписку"]
        [:br]
        [:br]]])))



;; ----------------------------
;; UI изменения пароля

(def password-form-template
  [:div.row
   [:div.col-xs-12
    [:div.row
     [:div.col-xs-3
      (input "Новый пароль" :password :new-password)]]
    [:div.row
     [:div.col-xs-3
      (input "Старый пароль" :password :old-password)]]]])

(defn settings-password-subpage
  "UI изменения пароля"
  []
  (let [password-data (atom nil)] 
    (fn [] 
      [:div.row
       [:div.col-xs-10
        [:h4 "Изменить пароль"]
        [:hr]
        [bind-fields
         password-form-template
         password-data]
        [:br]
        ;; Кнопка отправки данных
        [:button.btn.btn-default
         (if (b/valid? 
              @password-data 
              :new-password v/required
              :old-password v/required)
           {:type "button"
            :on-click #($put->change-password password-data)}
           {:type "button"
            :class "disabled"})
         "Сохранить"]
        [:br]
        [:br]]])))



;; -------------------------
;; UI изменения почтового адреса

(def email-form-template
  [:div.row
   [:div.col-xs-12
    [:div.row
     [:div.col-xs-3
      (input "Почтовый адрес" :text :email)]]
    [:div.row
     [:div.col-xs-4
      [:br]
      (row "Получать новости?"
           [:input {:field :checkbox :id :mailing}]
           :inline? true
           :label-size 6
           :input-size 6)]]]])

(defn settings-email-subpage
  "UI изменения почтового адреса"
  []
  (let [email-data (atom 
                    {:mailing (-> @app-state :account :mailing)
                     :email (-> @app-state :account :email)})] 
    (fn [] 
      [:div.row
       [:div.col-xs-10
        [:h4 "Почтовый адрес"]
        [:hr]
        [bind-fields
         email-form-template
         email-data]
        [:br]
        ;; Кнопка отправки данных
        [:button.btn.btn-default
         (if (b/valid? 
              @email-data 
              :email v/required
              :mailing v/required)
           {:type "button"
            :on-click #($put->change-email email-data)}
           {:type "button"
            :class "disabled"})
         "Сохранить"]
        [:br]
        [:br]]])))



;; ---------------------
;; UI отображения данных профиля

(defn settings-home-subpage
  "UI отображения данных аккаунта"
  [account]
  [:div.row
   [:div.col-xs-12
    [:h4 "Основная информация"]
    [:hr]
    [:table.table
     [:thead
      [:th "Логин"]
      [:th "Email"]
      [:th "Роль"]
      (if-not (= :admin (keyword (-> @app-state :identity :role)))
        [:th "Баланс"])
      ;; Скрыть если пользователь админ
      (if-not (= :admin (keyword (-> @app-state :identity :role)))
        [:th "Подписка"])
      [:th "Зарегистрирован"]
      [:th "Последнее обновление"]]
     [:tbody
      [:td (:login account)]
      [:td (:email account)]
      [:td 
       (str (util/translate-role (:role account)))]
      (if-not (= :admin (keyword (-> @app-state :identity :role)))
        [:td 
         (:coins account)
         [:del "М"]])
      (if-not (= :admin (keyword (-> @app-state :identity :role)))
        [:td 
         "до "
         (-> @app-state :profile :subscription)])
      [:td (:registered account)]
      [:td (:updated account)]]]
    [:hr]

    ;; Список последних траназкций
    ;; скрыть если пользователь админ
    (if-not (= :admin (keyword (-> @app-state :identity :role))) 
      [:div.col-xs-6
       [:h4 "Транзакции"]
       [:small "Доступны все транзакции за последние 6 месяцев"]
       [:hr]
       [:table.table
        [:thead
         [:tr
          [:th "Действие"]
          [:th "Дата и время"]
          [:th "Сумма"]]]
        [:tbody
         (doall 
          ;; Свернуть все результаты
          (for [t (reverse (:transactions account))]
            [:tr {:key (rand-int 9999)}
             [:td (:description t)]
             [:td (:datetime t)]
             [:td 
              (:coins t)
              [:del "М"]]]))]]])
    
    ;; Список последних аутентификаций
    [:div.col-xs-6
     [:h4 "Последние аутентификации"]
     [:small "Последние 15 аутентификаций"]
     [:hr]
     [:table.table
      [:thead
       [:tr
        [:th "IP адрес"]
        [:th "Браузер"]
        [:th "Дата и время"]]]
      [:tbody
       [:tr {:key (rand-int 9999)}
        [:td ""]
        [:td ""]
        [:td ""]]]]]]])

;; --------------------------
;; UI компонент настроек аккаунта

(defn settings-ui-component 
  "UI компонент настроек аккаунта"
  []
  [settings-wrap
   
   ;; Копия аккаунта из app-state
   (let [account-data (:account @app-state)]
     
     (fn []
       
       ;; Определить подстраницу
       (condp = (session/get :subpage)
          
          ;; Основные настройки
          :settings/home
          [settings-home-subpage account-data]
          
          ;; Пополнение баланса
          :settings/balance
          [settings-balance-subpage]

          ;; Продление подписки
          :settings/subscription
          [settings-subscription-subpage]
          
          ;; Изменение почтового адреса
          :settings/email
          [settings-email-subpage]

          ;; Изменение пароля
          :settings/password
          [settings-password-subpage])))])
