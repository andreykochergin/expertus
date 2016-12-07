(ns expertus.components.experts
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [reagent-forms.core :refer [bind-fields]]
            [ajax.core :refer [GET PUT]]
            [cljs-time.core :as time]
            [cljs-time.format :as timef]
            [cljs-time.coerce :as timec]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [expertus.state :refer [app-state]]
            [expertus.auth :refer [auth?]]
            [expertus.forms :refer 
             [input row
              live-search-categories
              live-search-locations]]
            [expertus.routes :refer [reset-hash]]
            [expertus.load :refer [load-on load-off]]
            [expertus.alert :refer [add-alert-message]]
            [expertus.util :as util]))

;; ---------------------
;; Обработчики запросов к API

(defn $get->profiles
  "Получить список профилей"
  [search-data results]
  (load-on)
  
  ;; Собрать параметры запроса
  (let [query (atom (select-keys @search-data [:limit :role]))
        categories (:categories @search-data)
        locations (:locations @search-data)] 

    ;; Проверить специализации
    (if (not-empty categories)
      (swap! query assoc
             :categories (util/vec->str categories)))

    ;; Проверить локации
    (if (not-empty locations)
      (swap! query assoc
             :locations (util/vec->str locations)))

    ;; Запрос к API
    (GET
     "/profiles"
     :keywords? true
     :response-format :json
     :params @query
     :handler
     (fn [res]
       (reset! results (:result res))
       (load-off)))))



;; -------------------------
;; Разметка UI элементов компонента

;; Навигация
(defn experts-navbar 

  "Навигация по списку специалистов"

  []
  [:div
   [:ul.nav.nav-pills.nav-stacked
    [:li
     {:class (when (= :experts/search (session/get :subpage)) "active")}
     [:a 
      {:href "#/experts"}
      "Поиск"]]
    [:li
     {:class (when (= :experts/categories (session/get :subpage)) "active")}
     [:a 
      {:href "#/experts/categories"}
      "Все специализации"]]]])

(defn experts-header

  "Заголовок компонента поиска специалистов"

  []
  [:div.page-header
   [:h3 "Частные тренеры"]])

(defn experts-search-form

  "Форма поиска специалистов"
  
  [search-data results]

  (fn [] 
    [:div.row
     [:div.col-xs-12
      [:h4 "Поиск"]]

     ;; Выбор специализаций
     [:div.col-xs-12
      [:h5 "Специализации"]
      [live-search-categories search-data]]

     ;; Выбор локаций
     [:div.col-xs-12
      [:h5 "Метро и районы"]
      [live-search-locations search-data]]

     ;; Кнопка отправки данных
     [:div.col-xs-12
      [:button.btn.btn-primary
       {:role "button"
        :on-click
        #($get->profiles search-data results)}
       "Найти"]]]))

(defn experts-wrap
  "Шаблон для компонента и всех его подстраниц"
  [body]
  [:div.container 
     [experts-header] 
     [:div.row
      [:div.col-xs-2
       [experts-navbar]]
      [:div.col-xs-10
       (body)]]])


;; -------------------------
;; UI поиска и списка

(defn expert-search-subpage
  "UI списка и поиска специалистов"
  []
  (let [;; Параметры поискового запроса
        search-data (atom {:role "expert"
                           :limit 50})
        
        ;; Атом результатов поиска
        results (atom nil)]

    ;; Объявить начало загрузки данных
    (load-on)

    ;; Запрос к API
    ($get->profiles search-data results)

    ;; Тело компонента
    (fn [] 
      [:div.row

       ;; Результаты поиска
       [:div.col-xs-9
        (if (< 0 (count @results)) 
          [:div.row
           (doall 
            (for [profile @results]

              ;; Шаблон профиля в списке
              [:div.col-xs-4.col-md-6
               {:key (:login profile)} 
               
               [:div.row

                ;; Блок 1
                ;; Аватар
                [:div.col-xs-12.col-md-5
                 [:a.thumbnail
                  {:href (str "#/profile/user/" (:login profile))}
                  [:img {:src "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNzEiIGhlaWdodD0iMTgwIj48cmVjdCB3aWR0aD0iMTcxIiBoZWlnaHQ9IjE4MCIgZmlsbD0iI2VlZSIvPjx0ZXh0IHRleHQtYW5jaG9yPSJtaWRkbGUiIHg9Ijg1LjUiIHk9IjkwIiBzdHlsZT0iZmlsbDojYWFhO2ZvbnQtd2VpZ2h0OmJvbGQ7Zm9udC1zaXplOjEycHg7Zm9udC1mYW1pbHk6QXJpYWwsSGVsdmV0aWNhLHNhbnMtc2VyaWY7ZG9taW5hbnQtYmFzZWxpbmU6Y2VudHJhbCI+MTcxeDE4MDwvdGV4dD48L3N2Zz4=" :alt ""}]]]

                ;; Блок 2
                [:div.col-xs-12.col-md-6
                 [:div.row
                  
                  ;; Имя
                  [:div.col-xs-12
                   [:h5
                    (str (:first-name profile) " " (:last-name profile))]]
                  
                  ;; Специализации
                  [:div.col-xs-12
                   [:p
                    (doall 
                     (for [c (:categories profile)]
                       (let [c-data
                             (first 
                              (filter 
                               #(= c (:alias %)) 
                               (-> @app-state :app-data :categories)))]
                         [:span.label.label-primary
                          {:key (:alias c-data)}
                          (:title c-data)])))]]

                  ;; Локации
                  [:div.col-xs-12
                   [:p
                    (doall 
                     (for [l (:locations profile)]
                       (let [l-data
                             (first 
                              (filter 
                               #(= l (:alias %)) 
                               (-> @app-state :app-data :locations)))]
                         [:span.label.label-default
                          {:key (:alias l-data)}
                          (:title l-data)])))]]
                  
                  [:div.col-xs-12
                   [:hr]]]]]]))]

          ;; Если результат пуст
          [:p.text-center "Поиск не дал результатов"])]
       
       ;; Форма поиского запроса
       [:div.col-xs-3
        [experts-search-form search-data results]]])))


;; -----------------------------------------
;; UI компонент списка и поиска специалистов

(defn experts-ui-component 
  
  "UI компонент поиска по спику специалистов"
  
  []
  [experts-wrap
   
   (fn []
     
     ;; Определить подстраницу
     (condp = (session/get :subpage)
      
       ;; Поиск специалистов
       :experts/search
       [expert-search-subpage]))])
