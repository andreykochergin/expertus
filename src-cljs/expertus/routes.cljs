(ns expertus.routes
  (:require [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

;; Изменение текущего хэша
(defn reset-hash [hash]
  (set! js/window.location.hash hash)
  (secretary/dispatch! hash)
  [:div])

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! 
  []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(secretary/set-config! :prefix "#")
;; UI домашний раздел
(secretary/defroute "/" []
  (session/put! :page :home))

;; UI авторизации
(secretary/defroute "/login" []
  (session/put! :page :login))

;; UI регистрации
(secretary/defroute "/registration" []
  (session/put! :page :registration)
  (session/put! :subpage :registration/action))

;; -----------------
;; НАСТРОЙКИ
;; UI настройки - основная информация
(secretary/defroute "/settings" []
  (session/put! :page :settings)
  (session/put! :subpage :settings/home))

;; UI настройки - пополнение баланса
(secretary/defroute "/settings/balance" []
  (session/put! :page :settings)
  (session/put! :subpage :settings/balance))

;; UI настройки - продление подписки
(secretary/defroute "/settings/subscription" []
  (session/put! :page :settings)
  (session/put! :subpage :settings/subscription))

;; UI настройки - изменение пароля
(secretary/defroute "/settings/password" []
  (session/put! :page :settings)
  (session/put! :subpage :settings/password))

;; UI настройки - изменение почтового адреса
(secretary/defroute "/settings/email" []
  (session/put! :page :settings)
  (session/put! :subpage :settings/email))
;; --------------

;; --------------
;; UI редактирование и просмотр профиля

(secretary/defroute "/profile/user/:login" [login]
  (session/put! :page :profile/view)
  (session/put! :profile-login login))

(secretary/defroute "/profile/edit" []
  (session/put! :page :profile/edit)
  (session/put! :subpage :profile/edit-base))

;; Изменение локаций
(secretary/defroute "/profile/edit/locations" []
  (session/put! :page :profile/edit)
  (session/put! :subpage :profile/edit-locations))

;; Изменение категорий
(secretary/defroute "/profile/edit/categories" []
  (session/put! :page :profile/edit)
  (session/put! :subpage :profile/edit-categories))

;; Изменение услуг
(secretary/defroute "/profile/edit/services" []
  (session/put! :page :profile/edit)
  (session/put! :subpage :profile/edit-services))

;; Изменение контактов
(secretary/defroute "/profile/edit/contacts" []
  (session/put! :page :profile/edit)
  (session/put! :subpage :profile/edit-contacts))

;; Изменение описания
(secretary/defroute "/profile/edit/about" []
  (session/put! :page :profile/edit)
  (session/put! :subpage :profile/edit-about))

;; Изменение аватара
(secretary/defroute "/profile/edit/avatar" []
  (session/put! :page :profile/edit)
  (session/put! :subpage :profile/edit-avatar))

;; Изменение файлов
(secretary/defroute "/profile/edit/files" []
  (session/put! :page :profile/edit)
  (session/put! :subpage :profile/edit-files))

;; ---------------

;; UI поиска специалистов
(secretary/defroute "/experts" []
  (session/put! :page :experts)
  (session/put! :subpage :experts/search))

;; UI поиска компаний
(secretary/defroute "/companies" []
  (session/put! :page :companies)
  (session/put! :subpage :companies/search))

;; UI поиска магазинов
(secretary/defroute "/stores" []
  (session/put! :page :stores)
  (session/put! :subpage :stores/search))

;; UI поиска вакансий и работодателей
(secretary/defroute "/jobs" []
  (session/put! :page :jobs)
  (session/put! :subpage :jobs/search))

;; Работодатели
(secretary/defroute "/jobs/headhunters" []
  (session/put! :page :jobs)
  (session/put! :subpage :jobs/search-headhunters))

;; ----------------------------
;; ПАНЕЛЬ АДМИНИСТРАТОРА
(secretary/defroute "/admin" []
  (session/put! :page :admin))
