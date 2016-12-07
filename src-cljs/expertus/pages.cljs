(ns expertus.pages
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]

            ;; Компоненты аккаунтов
            [expertus.components.login
             :refer [login-ui-component]]
            [expertus.components.registration
             :refer [registration-ui-component]]
            [expertus.components.settings
             :refer [settings-ui-component]]

            ;; Компоненты профилей
            [expertus.components.profile-edit
             :refer [profile-edit-ui-component]]
            [expertus.components.profile-view
             :refer [profile-view-ui-component]]
            [expertus.components.experts
             :refer [experts-ui-component]]
            [expertus.components.companies
             :refer [companies-ui-component]]
            [expertus.components.stores
             :refer [stores-ui-component]]
            [expertus.components.jobs
             :refer [jobs-ui-component]]

            ;; Прочие компоненты
            [expertus.components.home 
             :refer [home-ui-component]]
            [expertus.components.admin
             :refer [admin-ui-component]]))


;; Все страницы сайта
(def pages
  {;; Домашняя страница (поиск)
   :home #'home-ui-component

   ;; Аутентификация
   :login #'login-ui-component

   ;; Регистрация
   :registration #'registration-ui-component

   ;; Настройки аккаунта
   :settings #'settings-ui-component
   
   ;; Редактирование профиля  
   :profile/edit #'profile-edit-ui-component

   ;; Просмотр профиля
   :profile/view #'profile-view-ui-component

   ;; Поиск и список специалистов
   :experts #'experts-ui-component

   ;; Поиск и список компаний
   :companies #'companies-ui-component

   ;; Поиск и список магазинов
   :stores #'stores-ui-component

   ;; Поиск и список вакансий и работодателей
   :jobs #'jobs-ui-component

   ;; Раздел управления
   :admin #'admin-ui-component})

;; Текущаяя страница
(defn page
  []
  [(pages (session/get :page [:div]))])
