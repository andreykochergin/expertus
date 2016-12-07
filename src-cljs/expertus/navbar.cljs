(ns expertus.navbar
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [expertus.state :refer [app-state]]
            [expertus.auth :refer [auth? logout]]
            [expertus.routes :refer [reset-hash]]
            [expertus.alert :refer [add-alert-message]]))

(defn navbar-ui-component 
  "Основной компонент навигации"
  []
  [:div.container
   [:div.row
    ;; Если пользователь не аутентифицирован
    ;; отобразить верхние кнопки
    (if-not (auth?) 
      [:div.col-xs-offset-8.col-xs-4.text-right
       (str (@app-state :profile))
       ;; Кнопка регистриции
       [:ul.nav.nav-pills
        [:li
         {:class (when (= :registration (session/get :page)) "active")}
         [:a {:href "#/registration"} "Регистрация"]]
        [:li
         {:class (when (= :login (session/get :page)) "active")}
         [:a {:href "#/login"} "Вход"]]]]
      ;; Иначе отобразить меню аккаунта
      [:div.col-xs-offset-8.col-xs-4
       [:ul.nav.nav-pills
        [:li
         {:class 
          (when (or
                 (= :settings (session/get :page))
                 (= :settings/balance (session/get :page))
                 (= :settings/subscription (session/get :page))
                 (= :settings/password (session/get :page))
                 (= :settings/email (session/get :page))) 
            "active")}
         [:a {:href "#/settings"} "Настройки"]]
        (if-not (= :admin (keyword (-> @app-state :identity :role)))
          ;; Если тип аккаунта не администратор или модератор
          [:li
           {:class
            (when (or
                 (= :profile/edit (session/get :page))
                 (= :profile/edit-locations (session/get :page))
                 (= :profile/edit-categories (session/get :page))
                 (= :profile/edit-services (session/get :page))
                 (= :profile/edit-contacts (session/get :page))
                 (= :profile/edit-about (session/get :page))
                 (= :profile/edit-avatar (session/get :page))
                 (= :profile/edit-files (session/get :page))) 
            "active")}
           [:a {:href "#/profile/edit"} "Профиль"]]
          ;; Панель администратора
          [:li
           {:class (when (= :admin (session/get :page)) "active")}
           [:a {:href "#/admin"} "Панель управления"]])
        [:li [:a {:href "#/" :on-click #(logout)} "Выйти"]]]])]
   ;; Список меню
   ;; Логотип или название сайта
   [:h2 
    "Мой Тренер " [:small "спортивный поисковик"]]
   [:hr]
   [:ul.nav.nav-pills
    [:li
     ;; Выделение активного элемента
     {:class (when (= :home (session/get :page)) "active")}
     [:a {:href "#/"} "Поиск"]]
    [:li
     ;; Выделение активного элемента
     {:class (when (= :experts (session/get :page)) "active")}
     [:a {:href "#/experts"} "Тренеры"]]
    [:li
     ;; Выделение активного элемента
     {:class (when (= :companies (session/get :page)) "active")}
     [:a {:href "#/companies"} "Клубы"]]
    [:li
     ;; Выделение активного элемента
     {:class (when (= :stores (session/get :page)) "active")}
     [:a {:href "#/stores"} "Магазины"]]
    [:li
     ;; Выделение активного элемента
     {:class (when (= :events (session/get :page)) "active")}
     [:a {:href "#/events"} "События"]]
    [:li
     ;; Выделение активного элемента
     {:class (when (= :jobs (session/get :page)) "active")}
     [:a {:href "#/jobs"} "Работа"]]]
   [:hr]])
