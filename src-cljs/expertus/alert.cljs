(ns expertus.alert
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [goog.string :as gstr]
            [expertus.state :refer [app-state]]))

;; Система предупреждений и вывода сообщений
(defn add-alert-message
  [res & {:keys [title message page]}]
  ;; Определить статус ответа
  (if-let [status (or (:status res) (get-in res [:response :status]))]
    ;; Сообщение ответа
    (let [msg (or (str (:message res)) (get-in res [:response :message]))
          sp (session/get :subpage)
          p (session/get :page)] 
      (condp = status
        200 ;; ОК
        (swap! app-state assoc
               :alert
               ;; Если агрументы не nil
               (if (and title message)
                 ;; Добавить их в alert
                 {:title title
                  :message message
                  :page (if page page p)
                  :subpage sp
                  :type :success}
                 ;; Иначе отобразить
                 ;; сообщение от сервера
                 {:title status
                  :message msg
                  :page (if page page p)
                  :subpage sp
                  :type :success}))
        201 ;; что-либо успешно добавлено на сервер
        (swap! app-state assoc
               :alert
               ;; Если агрументы не nil
               (if (and title message)
                 ;; Добавить их в alert
                 {:title title
                  :message message
                  :page (if page page p)
                  :subpage sp
                  :type :success}
                 ;; Иначе отобразить
                 ;; сообщение от сервера
                 {:title status
                  :message msg
                  :page (if page page p)
                  :subpage sp
                  :type :success}))
        400 ;; неверные аргументы или ошибка запроса
        (swap! app-state assoc
               :alert
               ;; Если агрументы не nil
               (if (and title message)
                 ;; Добавить их в alert
                 {:title "Ошибка запроса"
                  :message "Проверьте валидность введенных данных"
                  :page (if page page p)
                  :subpage sp
                  :type :danger}
                 ;; Иначе отобразить
                 ;; сообщение от сервера
                 {:title status
                  :message msg
                  :page (if page page p)
                  :subpage sp
                  :type :danger}))
        403 ;; Ошибка доступа
        (swap! app-state assoc
               :alert
               ;; Если агрументы не nil
               (if (and title message)
                 ;; Добавить их в alert
                 {:title "Доступ запрещен"
                  :message "Вы не имеете доступа к этому ресурсу либо
срок вашего токена истек"
                  :page (if page page p)
                  :subpage sp
                  :type :danger}
                 ;; Иначе отобразить
                 ;; сообщение от сервера
                 {:title status
                  :message msg
                  :page (if page page p)
                  :subpage sp
                  :type :danger}))
        404 ;; ничего не найдено
        (swap! app-state assoc
               :alert
               ;; Если агрументы не nil
               (if (and title message)
                 ;; Добавить их в alert
                 {:title title
                  :message message
                  :page (if page page p)
                  :subpage sp
                  :type :warning}
                 ;; Иначе отобразить
                 ;; сообщение от сервера
                 {:title status
                  :message msg
                  :page (if page page p)
                  :subpage sp
                  :type :warning}))
        500 ;; ошибка на стороне сервера
        (swap! app-state assoc
               :alert
               ;; Если агрументы не nil
               (if (and title message)
                 ;; Добавить их в alert
                 {:title title
                  :message message
                  :page (if page page p)
                  :subpage sp
                  :type :danger}
                 ;; Иначе отобразить
                 ;; сообщение от сервера
                 {:title status
                  :message msg
                  :page (if page page p)
                  :subpage sp
                  :type :danger}))))))

(defn alert-ui-component

  "Компонент вывода сообщений от обработчиков"

  []
  (if (and (get @app-state :alert)
           (= (-> @app-state :alert :subpage) (session/get :subpage))
           (= (-> @app-state :alert :page) (session/get :page)))
    [:div.container
     [:br]
     [:div.alert.alert-dismissable
      {:class 
       (cond
        (= :danger (-> @app-state :alert :type)) "alert-danger"
        (= :success (-> @app-state :alert :type)) "alert-success"
        (= :warning (-> @app-state :alert :type)) "alert-warning")}
      [:button.close
       {:type "button"
        :on-click #(swap! app-state assoc :alert nil)}
       (gstr/unescapeEntities "&times;")]
      [:h4 (-> @app-state :alert :title)]
      [:p (-> @app-state :alert :message)]]]
    (do (swap! app-state assoc :alert nil) [:div])))
