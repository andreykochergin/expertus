(ns expertus.components.profile-view
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
            [expertus.alert :refer [add-alert-message]]
            [expertus.load :refer [load-on load-off]]
            [expertus.util :as util]))



;; ---------------------------
;; Обработчики запросов к API

(defn $get->profile
  "Получить профиль пользователя по логину"
  [state login]
  (load-on)
  (GET
   (str "/profiles/" login)
   {:keywords? true
    :response-format :json
    :handler
    (fn [res]
      (reset! state res)
      (load-off))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message
       res
       :title "Профиль не найден"
       :message "Не удалось найти профиль по вашему запросу"))}))


;; --------------------------------------------
;; UI копонент просмотра профиля пользователя


(defn profile-view-wrap
  "Шаблон UI компонента"
  [body]
  [:div.container
   [:div.row
    [:div.col-xs-12
     (body)]]])

(defn $get->like-profiles
  "Поиск похожих профилей"
  [profile results]
  (GET 
   "/profiles"
   {:keywords? true
    :response-format :json
    :params 
    {:categories (util/vec->str (:categories @profile))
     :locations (util/vec->str (:locations @profile))}
    :handler
    (fn [res]
      (reset! results
       (filter 
        #(not= (:login @profile) (:login %)) 
        (:result res))))}))



;; -----------------------------
;; Компонент похожих профилей

(defn like-profile-template
  "Шаблон профиля в списке"
  [n]
  [:div.row
   
   ;; Заголовок
   [:div.col-xs-12 
    [:h6
     
     ;; Определить роль
     (condp = (keyword (:role n))
       
       ;; Специалист
       :expert
       (str (:first-name n) " " (:last-name n))

       ;; Компания
       :company (str (:name n))

       ;; Магазин
       :store (str (:name n))

       "")]]

   ;; Специализации
   [:div.col-xs-12
    [:p
     (doall 
      (for [c (:categories n)]
        (let [c-data
              (first 
               (filter 
                #(= c (:alias %)) 
                (-> @app-state :app-data :categories)))]
          [:span.label.label-primary
           {:key (keyword (:alias c-data))}
           (:title c-data)])))]]

   ;; Локации
   [:div.col-xs-12
    [:p
     (doall 
      (for [l (:locations n)]
        (let [l-data
              (first 
               (filter 
                #(= l (:alias %)) 
                (-> @app-state :app-data :locations)))]
          [:span.label.label-default
           {:key (keyword (:alias l-data))}
           (:title l-data)])))]]

   ;; Аватар
   [:div.col-xs-6
    [:a.thumbnail
     {:href (str "#/profile/user/" (:login n))}
     [:img {:src "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNzEiIGhlaWdodD0iMTgwIj48cmVjdCB3aWR0aD0iMTcxIiBoZWlnaHQ9IjE4MCIgZmlsbD0iI2VlZSIvPjx0ZXh0IHRleHQtYW5jaG9yPSJtaWRkbGUiIHg9Ijg1LjUiIHk9IjkwIiBzdHlsZT0iZmlsbDojYWFhO2ZvbnQtd2VpZ2h0OmJvbGQ7Zm9udC1zaXplOjEycHg7Zm9udC1mYW1pbHk6QXJpYWwsSGVsdmV0aWNhLHNhbnMtc2VyaWY7ZG9taW5hbnQtYmFzZWxpbmU6Y2VudHJhbCI+MTcxeDE4MDwvdGV4dD48L3N2Zz4=" :alt ""}]]]])

(defn like-profiles-list 
  "Список похожих профилей"
  [results]
  (fn [] 
    [:div.row

     ;; Список похожих профилей
     [:div.col-xs-12
      [:h4 
       "Похожие профили"]
      [:div.row
       (if (> (count @results) 0)
         (doall 
          (for [n (take 3 (shuffle @results))]
          
            ;; Шаблон профиля в списке
            [:div.col-xs-12
             {:key (keyword (:login n))}
             [like-profile-template n]
             [:hr]]))
         [:div.col-xs-12
          [:p "Ничего не найдено"]])]]]))

(defn like-profiles
  "Список похожих профилей"
  [profile]
  (let [results (atom [])]
    
    ;; Запрос к API
    ($get->like-profiles profile results)

    ;; Контент
    [like-profiles-list results]))


(defn profile-view-ui-component
  "UI публичного просмотра профиля пользователя"
  []
  [profile-view-wrap
   (let [profile (atom nil)]

     ;; Запросить данные профиля у API
     ($get->profile profile (session/get :profile-login))

     ;; Если профиль найден
     (fn [] 
       (if @profile
         [:div.row

          ;; Заголовок
          [:div.col-xs-12
           [:div.page-header
            [:h3 
             (condp = (keyword (:role @profile))
               :expert (str (:first-name @profile) " " (:last-name @profile))
               :company (str (:name @profile))
               :store (str (:name @profile))
               "")
             [:small (str " " (util/translate-role (:role @profile)))]]]]
          
          ;; Блоки
          [:div.col-xs-12 
           [:div.row

            ;; Блок 1
            [:div.col-xs-12.col-md-3
             [:div.row

              ;; Аватар
              [:div.col-xs-10
               [:a.thumbnail
                [:img {:src "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNzEiIGhlaWdodD0iMTgwIj48cmVjdCB3aWR0aD0iMTcxIiBoZWlnaHQ9IjE4MCIgZmlsbD0iI2VlZSIvPjx0ZXh0IHRleHQtYW5jaG9yPSJtaWRkbGUiIHg9Ijg1LjUiIHk9IjkwIiBzdHlsZT0iZmlsbDojYWFhO2ZvbnQtd2VpZ2h0OmJvbGQ7Zm9udC1zaXplOjEycHg7Zm9udC1mYW1pbHk6QXJpYWwsSGVsdmV0aWNhLHNhbnMtc2VyaWY7ZG9taW5hbnQtYmFzZWxpbmU6Y2VudHJhbCI+MTcxeDE4MDwvdGV4dD48L3N2Zz4=" :alt ""}]]]
              
              ;; Дополнительная информация
              [:div.col-xs-12
               
               ;; Определить тип профиля
               (condp = (keyword (:role @profile))

                 ;; Специалист
                 :expert
                 [:ul.list-group
                  [:li.list-group-item
                   [:strong "Возраст"]
                   (str " " (:age @profile))]
                  [:li.list-group-item
                   [:strong "Опыт"]
                   (str " " (:experience @profile))]
                  [:li.list-group-item
                   [:span.glyphicon.glyphicon-eye-open]
                   (str " " 40302)]]

                 "")]]]

            ;; Блок 2
            [:div.col-xs-12.col-md-6
             [:div.row

              ;; Специализации
              [:div.col-xs-12
               [:p
                (doall 
                 (for [c (:categories @profile)]
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
                 (for [l (:locations @profile)]
                   (let [l-data
                         (first 
                          (filter 
                           #(= l (:alias %)) 
                           (-> @app-state :app-data :locations)))]
                     [:span.label.label-default
                      {:key (:alias l-data)}
                      (:title l-data)])))]]

              [:div.col-xs-12
               [:hr]]

              ;; Образование
              ;; отображается только в профилях специалистов
              (if (= :expert (keyword (:role @profile)))
                [:div.col-xs-12
                 [:h4 "Образование"]
                 [:p (:education @profile)]
                 [:hr]])

              ;; Описание
              [:div.col-xs-12
               [:h4 "Описание"]
               [:p (:about @profile)]]
              
              [:div.col-xs-12
               [:hr]]

              ;; Услуги
              ;; Скрыть блок если нет услуг
              [:div.col-xs-12
               [:h4 "Услуги"]
               (if (> (count (:services @profile)) 0) 
                 [:ul.list-group
                  (doall 
                   (for [n (reverse (:services @profile))]
                     [:li.list-group-item
                      {:key (rand-int 3000)}
                      [:h5 (:title n)
                       [:small " "
                        (when (-> n :price :above) "от ")
                        (-> n :price :sum)
                       [:del "Р"]]]
                      [:p (:description n)]]))])]]]
            

            ;; Блок 3
            [:div.col-xs-12.col-md-3
             [:div.row
              
              ;; Контакты
              ;; Скрыть блок если нет контактов
              [:div.col-xs-12
               [:h4 "Контакты"]
               (if (> (count (:contacts @profile)) 0)
                 [:ul.list-group
                  (doall 
                   (for [n (reverse (:contacts @profile))]
                     [:li.list-group-item
                      {:key (rand-int 3000)}
                      [:p
                       [:h6
                        (:content n)
                        " "
                        [:small
                         (condp = (keyword (:type n))
                           :url "сайт"
                           :email "email"
                           :phone "телефон"
                           :address "адрес"
                           :skype "skype"
                           "")]]]]))])]

              ;; Похожие профили
              [:div.col-xs-12
               [like-profiles profile]]
              ]]]]])))])
