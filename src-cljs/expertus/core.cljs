(ns expertus.core
  (:require [reagent.core :as reagent :refer [atom]]
            [expertus.routes :refer 
             [reset-hash 
              hook-browser-navigation!]]
            [expertus.state :refer 
             [set-locations!
              set-categories!
              app-state]]
            [expertus.pages :refer [page]]
            [expertus.alert :refer
             [alert-ui-component]]
            [expertus.load :refer
             [load-ui-component]]
            [expertus.navbar :refer
             [navbar-ui-component]]))

;; Компоненты которые неободимо отрисовать
(defn mount-components []
  ;; Навигация
  (reagent/render [navbar-ui-component] 
                  (.getElementById js/document "navbar"))
  ;; Сообщения
  (reagent/render [alert-ui-component] 
                  (.getElementById js/document "alert"))

  ;; Загрузка
  (reagent/render [load-ui-component]
                  (.getElementById js/document "loading"))

  ;; Страница
  (reagent/render [page]
                  (.getElementById js/document "app")))

;; Инициализация приложения
(defn init! 
  []
  (set-categories!)
  (set-locations!)
  (hook-browser-navigation!)
  (mount-components))
