(ns expertus.forms
  (:import goog.net.IframeIo)
  (:require [expertus.state :refer [app-state]]
            [goog.events :as gev]
            [reagent.core :as reagent :refer [atom]]
            [reagent-forms.core :refer [bind-fields]]))

(defn upload-avatar
  "Загрузка аватара"
  []
  (let [io (IframeIo.)]
    (gev/listen 
     io
     (aget goog.net.EventType "SUCCESS")
     #(js/alert "SUCCESS!"))
    (gev/listen 
     io
     (aget goog.net.EventType "ERROR")
     #(js/alert "ERROR!"))
    (gev/listen 
     io
     (aget goog.net.EventType "COMPLETE")
     #(js/alert "COMPLETE!"))
    (.setErrorChecker 
     io 
     #(not= "ok" (.getResponseText io)))
    (.sendFromForm 
     io 
     (.getElementById js/document "upload-avatar-input") 
     "/profiles/upload-avatar")))


(defn live-search-locations
  
  "Шаблон живого поиска локаций по списку"

  [search-data]
  (let [query (atom nil)
        list? (atom false)]
    (fn []
      
      [:div.row

       ;; Список выбранных элементов
       (if (not-empty (:locations @search-data)) 
         [:div.col-xs-12.form-group
          (doall (for [n (:locations @search-data)]
             (let [n-data 
                   (first 
                    (filter 
                     #(= n (:alias %)) 
                     (-> @app-state :app-data :locations)))] 
               [:span.label.label-primary
                {:key (:alias n-data)}
                (:title n-data)
                " "
               
                ;; Удаление элемента
                [:span
                 {:on-click
                  #(do
                     (reset! list? false)
                     (swap! search-data assoc
                            :locations
                            (remove 
                             (fn [n] (= n (:alias n-data)))
                             (:locations @search-data))))}
                 "x"]])))])

       ;; Элемент ввода значения
       [:div.col-xs-12.form-group
        [:input.form-control
         {:type "text"
          :value @query
          :on-change
          #(do
             (reset! list? true)
             (reset! query (-> % .-target .-value)))}]
        
        ;; Список совпадений
        ;; вывести если пользователь сфокусирован 
        ;; на элементе поиска и ввел уже минимум 1 символ
        (if (and @list? (< 0 (count @query)))
          ;; Форма (список)
          [bind-fields
           [:ul.list-group
            {:field :multi-select :id :locations
             :on-click 
             #(do
                (reset! query nil)
                (reset! list? false))}

            ;; Вывести только те значения
            ;; которые проходят регулярное выражение
            ;; либо уже выбраны в search-data
            (doall (for [c (filter
                      #(or
                        (re-find 
                         (re-pattern (str "(?i)^" @query))
                         (:title %))
                        (not-empty 
                         (filter 
                          (fn [c] (= c (:alias %)))
                          (:locations @search-data)))) 
                      (-> @app-state :app-data :locations))]
               [:li.list-group-item
                {:key (:alias c)}
                [:span 
                 (:title c)]]))]
           search-data])]])))

(defn live-search-categories
  
  "Шаблон живого поиска категорий по списку"

  [search-data]
  (let [query (atom nil)
        list? (atom false)]
    (fn []
      
      [:div.row

       ;; Список выбранных элементов
       (if (not-empty (:categories @search-data)) 
         [:div.col-xs-12.form-group
          (doall (for [n (:categories @search-data)]
             (let [n-data 
                   (first 
                    (filter 
                     #(= n (:alias %)) 
                     (-> @app-state :app-data :categories)))] 
               [:span.label.label-primary
                {:key (:alias n-data)}
                (:title n-data)
                " "
               
                ;; Удаление элемента
                [:span
                 {:on-click
                  #(do
                     (reset! list? false)
                     (swap! search-data assoc
                            :categories 
                            (remove 
                             (fn [n] (= n (:alias n-data)))
                             (:categories @search-data))))}
                 "x"]])))])

       ;; Элемент ввода значения
       [:div.col-xs-12.form-group
        [:input.form-control
         {:type "text"
          :value @query
          :on-change
          #(do
             (reset! list? true)
             (reset! query (-> % .-target .-value)))}]
        
        ;; Список совпадений
        ;; вывести если пользователь сфокусирован 
        ;; на элементе поиска и ввел уже минимум 1 символ
        (if (and @list? (< 0 (count @query)))
          ;; Форма (список)
          [bind-fields
           [:ul.list-group
            {:field :multi-select :id :categories
             :on-click 
             #(do
                (reset! query nil)
                (reset! list? false))}

            ;; Вывести только те значения
            ;; которые проходят регулярное выражение
            ;; либо уже выбраны в search-data
            (for [c (filter
                     #(or
                       (re-find 
                        (re-pattern (str "(?i)^" @query))
                        (:title %))
                       (not-empty 
                        (filter 
                         (fn [c] (= c (:alias %)))
                         (:categories @search-data)))) 
                     (-> @app-state :app-data :categories))]
              [:li.list-group-item
               {:key (:alias c)}
               [:span 
                (:title c)]])]
           search-data])]])))

;; Элементы форм
(defn row 
  [label input 
   & {:keys [inline? label-size input-size] 
      :or {:inline? false :label-size 4 :input-size 8}}]
  (if-not inline? 
    [:div.row
     [:div.col-md-12
      [:label label]]
     [:div.col-md-12
      input]]
    [:div.row
     [:div
      {:class (str "col-md-" label-size)}
      [:label label]]
     [:div
      {:class (str "col-md-" input-size)}
      input]]))
(defn radio [label name value]
  [:div.radio
   [:label
    [:input {:field :radio :name name :value value}]
    label]])

(defn input [label type id]
  (row label [:input.form-control {:field type :id id}]))
