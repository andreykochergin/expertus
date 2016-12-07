(ns expertus.components.home
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [ajax.core :refer [GET]]
            [cljs-time.core :as time]
            [cljs-time.format :as timef]
            [cljs-time.coerce :as timec]))

;; Главная страница
(defn home-ui-component []
  [:div.container
   [:div.page-header
    [:h3 "Спортивный поисковик"]]])
