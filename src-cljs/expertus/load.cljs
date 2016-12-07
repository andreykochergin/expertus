(ns expertus.load
  (:require [reagent.core :as reagent :refer [atom]]
            [expertus.state :refer [app-state]]))

(defn load-on
  
  "Включить состояние загрузки контента"
  
  []
  (swap! app-state assoc :load true))

(defn load-off

  "Выключить состояние загрузки контента"
  
  []
  (swap! app-state assoc :load false))

(defn load-ui-component
  
  "Компонент статуса загрузки страницы"
  
  []
  (if (:load @app-state) 
    [:div.container 
     [:div.progress.progress-striped.active
      [:div.progress-bar
       {:role "progressbar"
        :aria-valuenow "100"
        :aria-valuemin "0"
        :aria-valuemax "100"
        :style {:width "100%"}}]
      [:span.sr-only "Загрузка..."]]]))
