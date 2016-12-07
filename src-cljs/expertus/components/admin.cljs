(ns expertus.components.admin
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
            [expertus.alert :refer [add-alert-message]]))

(defn admin-navbar 
  "Навигация панели управления"
  []
  [:ul.nav.nav-pills
   [:li 
    {:class (when (= :admin (session/get :page)) "active")}
    [:a
     {:href "#/"}
     "Статистика"]]
   [:li 
    {:class (when (= :admin/users (session/get :page)) "active")}
    [:a
     {:href "#/"}
     "Пользователи"]]
   [:li 
    {:class (when (= :admin/profiles (session/get :page)) "active")}
    [:a
     {:href "#/"}
     "Профили"]]
   [:li 
    {:class (when (= :admin/categories (session/get :page)) "active")}
    [:a
     {:href "#/"}
     "Специализации"]]
   [:li 
    {:class (when (= :admin/locations (session/get :page)) "active")}
    [:a
     {:href "#/"}
     "Локации"]]
   [:li 
    {:class (when (= :admin/locations (session/get :page)) "active")}
    [:a
     {:href "#/"}
     "Платежи"]]])

(defn admin-ui-component 
  "Компонент панели управления"
  []
  (if (and (auth?)
           (= :admin (keyword (-> @app-state :identity :role))))
    [:div.container
     [:div.page-header
      [:h3 "Панель управления"]]
     [admin-navbar]
     [:hr]]
    (reset-hash "/")))
