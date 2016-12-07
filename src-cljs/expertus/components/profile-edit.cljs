(ns expertus.components.profile-edit
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




;; ******************
;; ------------------
;; Вспомогательные функции

(defn vector-to-hash-map-set-ids

  "Перевести вектор в ассоциативный массив;

  Необходимо для построения структуры в которой
  каждый элемент можно редактировать в UI;

  Каждому элементу вектора данных добавить 
  уникальный инкурементнный ключ id;

  Перевести структуру в hash-map;"

  [data]
  (let [id (atom 0)] 
    (into 
     {} 
     (map 
      (fn [item] 
        {(keyword (str "id" (swap! id inc))) item}) 
      data))))

(defn hash-map-to-vector

  "Перевести ассоциативный массив услуг в вектор;
  
  Необходимо для отправки данных в БД;
  Данная функция уберет key с id
  у всех элементов и вернет вектор данных;"

  [services]
  (into [] (map (fn [s] (val s)) services)))

(defn refresh-profile!
  "Обновить значение профиля в app-state"
  []
  (GET 
   (str "/profiles/expanded/" (-> @app-state :identity :login))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :handler
    (fn [res]
      (swap! app-state assoc :profile res))
    :error-handler
    (fn [res]
      (add-alert-message 
       res
       :title "Ваш профиль не найден"
       :message "Возможно вы аутентифицированы как администратор.
Если это не так свяжитесь с технической поддержкой!"
       :page :home))}))




;; **************************
;; --------------------------
;; Обработчики запросов к API

(defn $put->update-about
  
  "Запрос к API на обновление списка контактов профиля;"
  
  [about]
  (load-on)
  (PUT
   (str "/profiles/about/" (-> @app-state :identity :login))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :params @about
    :handler
    (fn [res]
      (refresh-profile!) 
      (load-off)
      (add-alert-message 
       res
       :title "Описание обновлено"
       :message "Вы успешно отредаткирвоали описание профиля"))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message
       res
       :title "Не удалось обновить описание"
       :message "Проверьте правильность введеннных данных"))}))

(defn $put->update-contacts
  
  "Запрос к API на обновление списка контактов профиля;"
  
  [contacts]
  (load-on)
  (PUT
   (str "/profiles/contacts/" (-> @app-state :identity :login))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :params {:contacts contacts}
    :handler
    (fn [res]
      (refresh-profile!) 
      (load-off)
      (add-alert-message 
       res
       :title "Контакты обновлены"
       :message "Список контактов вашего профиля обновлен"))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message
       res
       :title "Не удалось обновить контакты"
       :message "Проверьте правильность введеннных данных"))}))

(defn $put->update-services
  
  "Запрос к API на обновление списка услуг профиля;"

  [services]
  (load-on)
  (PUT
   (str "/profiles/services/" (-> @app-state :identity :login))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :params {:services services}
    :handler
    (fn [res]
      (refresh-profile!) 
      (load-off)
      (add-alert-message 
       res
       :title "Услуги обновлены"
       :message "Список услуг вашего профиля обновлен"))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message
       res
       :title "Не удалось обновить услуги"
       :message "Проверьте правильность введеннных данных"))}))

(defn $put->update-categories

  "Запрос к API на обновление списка категорий профиля;"

  [profile-data]
  (load-on)
  (PUT
   (str "/profiles/categories/" (-> @app-state :identity :login))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :params (select-keys @profile-data [:categories])
    :handler
    (fn [res]
      (refresh-profile!) 
      (load-off)
      (add-alert-message 
       res
       :title "Список категорий обновлен"
       :message "Список категорий вашего профиля изменен"))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message
       res
       :title "Не удалось изменить список категорий"
       :message "Проверьте правильность введеннных данных"))}))

(defn $put->update-locations

  "Запрос к API на обновление списка локаций профиля;"

  [profile-data]
  (load-on)
  (PUT
   (str "/profiles/locations/" (-> @app-state :identity :login))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :params (select-keys @profile-data [:locations])
    :handler
    (fn [res]
      (refresh-profile!)
      (load-off)
      (add-alert-message 
       res
       :title "Список локаций обновлен"
       :message "Список локаций вашего профиля изменен"))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message
       res
       :title "Не удалось изменить список локаций"
       :message "Проверьте правильность введеннных данных"))}))

(defn $put->update-base 
  "Обновить основную информацию профиля"
  [profile-data]
  (load-on)
  (PUT
   (str 
    "/profiles/" (:role @profile-data) "/" (:login @profile-data))
   {:keywords? true
    :response-format :json
    :headers {:token (-> @app-state :identity :auth-token)}
    :params 
    (select-keys 
     @profile-data 
     (condp = (keyword (:role @profile-data))
       :expert
       [:first-name :last-name :age :job :price :experience :education]
       :company [:name :address]
       :store [:name :address]
       :headhunter [:first-name :last-name :company :address]

       []))
    :handler
    (fn [res]
      (refresh-profile!) 
      (load-off)
      (add-alert-message 
       res
       :title "Основная информация изменена"
       :message "Вы успешно изменили основную информацию своего профиля"))
    :error-handler
    (fn [res]
      (load-off)
      (add-alert-message
       res
       :title "Не удалось изменить профиль"
       :message "Проверьте правильность введенных данных"))}))




;; *************************
;; -------------------------
;; Разметка UI элемента

;; Элементы навигации
(defn profile-navbar 
  "Навигация для управления профилем"
  []
  [:div
   [:ul.nav.nav-pills.nav-stacked
    [:li
     {:class (when (= :profile/edit-base (session/get :subpage)) "active")}
     [:a 
      {:href "#/profile/edit"}
      "Основная информация"]]
    (when (= (-> @app-state :identity :role) "headhunter")
      [:li
       {:class (when (= :profile/edit-vacancies (session/get :subpage)) "active")}
       [:a 
        {:href "#/profile/edit/vacancies"}
        "Мои вакансии"]])
    [:li
     {:class (when (= :profile/edit-categories (session/get :subpage)) "active")}
     [:a 
      {:href "#/profile/edit/categories"}
      "Специализации"]]
    [:li
     {:class (when (= :profile/edit-locations (session/get :subpage)) "active")}
     [:a 
      {:href "#/profile/edit/locations"}
      "Локации"]]
    [:li
     {:class (when (= :profile/edit-services (session/get :subpage)) "active")}
     [:a 
      {:href "#/profile/edit/services"}
      "Услуги"]]
    [:li
     {:class (when (= :profile/edit-contacts (session/get :subpage)) "active")}
     [:a 
      {:href "#/profile/edit/contacts"}
      "Контакты"]]
    [:li
     {:class (when (= :profile/edit-about (session/get :subpage)) "active")}
     [:a 
      {:href "#/profile/edit/about"}
      "Описание"]]
    [:li
     {:class (when (= :profile/edit-avatar (session/get :subpage)) "active")}
     [:a 
      {:href "#/profile/edit/avatar"}
      "Аватар"]]
    [:li
     {:class (when (= :profile/edit-files (session/get :subpage)) "active")}
     [:a 
      {:href "#/profile/edit/files"}
      "Галерея"]]]])

(defn profile-header
  "Заголовок компонента редактирования профиля"
  []
  [:div.page-header
   [:h3 "Мой профиль "
    [:a {:href (str "#/profile/user/" (-> @app-state :identity :login))} 
     [:span.glyphicon.glyphicon-eye-open]]]])

(defn profile-edit-wrap
  "Шаблон для компонента и всех его подстраниц"
  [body]
  (if (auth?)
    [:div.container 
     [profile-header] 
     [:div.row
      [:div.col-xs-2
       [profile-navbar]]
      [:div.col-xs-10
       (body)]]]
    (reset-hash "/login")))



;; *************************************
;; -------------------------------------
;; Изменение автара
;; Подстраница UI компонента Profile-Edit

(defn profile-edit-avatar-subpage

  "UI редактирования файлов"

  [profile-data]
  [:div
   [:h1 "sad"]])

;; *************************************
;; -------------------------------------
;; Изменение файлов
;; Подстраница UI компонента Profile-Edit

(defn profile-edit-files-subpage

   "UI редактирования файлов"

   [profile-data]
   [:div
    [:h1 "wre"]])

 ;; *************************************
 ;; -------------------------------------
 ;; Изменение описания профиля
 ;; Подстраница UI компонента Profile-Edit

(defn profile-edit-about-subpage
   
  "UI редактирования описания"

  [profile-data]
  (let [about-data (atom {:about (:about @profile-data)})]
    (fn [] 
      [:div.row
       [:div.col-xs-8
        
        ;; Заголовок
        [:h4 "Описание"]
        [:hr]
        
        ;; Форма
        [bind-fields
         [:div.row
          [:div.col-xs-12.form-group
           (row
            "Опиcание вашего профиля"
            [:textarea.form-control
             {:field :textarea :id :about :rows 15}])]]
         about-data]

        ;; Кнопка отправки данных
        [:button.btn.btn-default

         ;; Валидация данных
         (if (b/valid?
              @about-data
              :about v/required)

           {:type "button"

            ;; По клику на кнопку отправить PUT запрос к API
            :on-click 
            #($put->update-about about-data)}
           
           ;; Ошибка валидации - деактивировать кнопку
           {:type "button"
            :class "disabled"})

         ;; Название кнопки
         "Сохранить"]]])))

;; *************************************
;; -------------------------------------
;; Изменение контактов
;; Подстраница UI компонента Profile-Edit

(def profile-edit-add-contact-form-template

  ;; Шаблон формы добавления нового контакта

  [:div.row
   [:div.col-xs-12.form-group
    (input "Содержание" :text :content)]

   ;; Выбор типа контакта
   [:div.col-xs-8.form-group
    [:label "Тип"]
    [:select.form-control {:field :list :id :type}
     (doall
      (for [c [{:type :email :title "Email"}
               {:type :address :title "Адрес"}
               {:type :url :title "Сайт"}
               {:type :phone :title "Телефон"}
               {:type :skype :title "Skype"}]]
        [:option {:key (:type c)} (:title c)]))]]])

(defn add-contact-form

  "UI формы добавления новой услуги;"

  [contacts]
  (let [;; Данные нового контакта
        contact-data (atom nil)]
    (fn [] 
      [:div.col-xs-12

       ;; Заголовок
       [:h5 "Добавить контакт"]
       [:hr]

       ;; Отрисовка формы
       [bind-fields
        profile-edit-add-contact-form-template
        contact-data]

       ;; Кнопка отправки данных
       [:button.btn.btn-default

        ;; Валидация данных
        (if (b/valid?
             @contact-data
             :type v/required
             :content v/required)

          {:type "button"

           ;; По клику на кнопку отправить PUT запрос к API
           :on-click 
           #(let [conts ;; обновленный вектор услуг
                  (conj (hash-map-to-vector @contacts) @contact-data)] 

              ;; Вычислить последовательно
              (do
                ;; PUT запрос
                ($put->update-contacts conts)

                ;; Добавить контакт в локальный список контактов
                (reset! contacts (vector-to-hash-map-set-ids conts))
                
                ;; Очистить поле ввода
                (reset! contact-data nil)))}
          
          ;; Ошибка валидации - деактивировать кнопку
          {:type "button"
           :class "disabled"})

        ;; Название кнопки
        "Добавить контакт"]])))

(defn contacts-list-group 

  "Отрисовать в html список контактов"
  
  [contacts]

  [:div.row
   [:div.col-xs-12
    
    ;; Кол-во контактов
    (when (not-empty @contacts)
      [:small 
       (str "Колличество добавленных контактов: "
            (count @contacts))
       [:hr]])]
   
   [:div.col-xs-12

    ;; Контакты не пусты
    ;; отрисовать список
    (if (not-empty @contacts) 
    
    [:ul.list-group

     ;; Элементы списка
     (doall
      
      ;; Вернуть контакты в обратной последовательности
      (for [contact (reverse @contacts)]
        
        ;; Шаблон элемента контакта
        (let [[id data] contact] 
          
          [:li.list-group-item 
           {:key (keyword id)}

           [:div
            
            ;; Тип
            [:h5
             (condp = (keyword (:type data))
               :email "Email"
               :phone "Телефон"
               :address "Адрес"
               :url "Веб-сайт"
               :skype "Skype"
               "")]

            ;; Содержание
            [:span (:content data)]

            [:br]

            ;; Кнопки действий
            [:button.btn.btn-default
             {:type "button"

              ;; Выполнить последовательно
              :on-click 
              #(do
                 ;; Отправить PUT запрос к API
                 ($put->update-contacts 
                  ;; Удалить контакт из локальной копии контактов
                  (hash-map-to-vector (swap! contacts dissoc id))))} 

             ;; Значек
             [:span.glyphicon.glyphicon-trash]]]])))]
    
    ;; Если список пуст
    ;; вернуть сообщение
    [:p.text-center "Нет добавленных контактов"])]])

(defn profile-edit-contacts-subpage

  "UI редактирование услуг;
  
  Содержит два блока: 
  1) список контактов; 
  2) форма добавления нового контакта;"

  [profile-data]
  (let [;; Перевести вектор контактов в hash-map с id's
        contacts (atom (vector-to-hash-map-set-ids (:contacts @profile-data)))] 
    [:div.row
     [:div.col-xs-8

      ;; Заголовок
      [:h4 "Список контактов"]
      [:hr]
            
      ;; Блок 1) Список контактов
      [contacts-list-group contacts]]
     
     ;; Форма добавления новой услуги
     [:div.col-xs-4
      [add-contact-form contacts]]]))





;; ********************************
;; --------------------------------
;; Изменение услуг профиля
;; подстраница UI компонента Profile-Edit

(def profile-edit-add-service-form-template

  ;; Шаблон формы добавления новой услуги

  [:div.row
   [:div.col-xs-12.form-group
    (input "Заголовок" :text :title)]
   [:div.col-xs-5.form-group
    (input "Стоимость" :text :price.sum)]
   [:div.col-xs-1.form-group
    (row "от"
         [:input {:field :checkbox :id :price.above}])]
   [:div.col-xs-12.form-group
      (row
       "Комментарий"
       [:textarea.form-control
        {:field :textarea :id :description :rows 4}])]])

(defn add-service-form
  
  "UI форма добавления новой услуги;"
  
  [services]
  (let [;; Данные новой услуги
        service-data (atom {:description "" :price {:above false}})]
    (fn [] 
      [:div.col-xs-12

       ;; Заголовок
       [:h5 "Добавить услугу"]
       [:hr]

       ;; Форма добавления
       [bind-fields
        profile-edit-add-service-form-template
        service-data]

       ;; Кнопка отправки данных
       [:button.btn.btn-default
        
        ;; Валидация данных
        (if (b/valid?
             @service-data
             :title v/required
             :price v/required)
          
          ;; Запрос к API по клику на кнопку
          {:type "button"
           :on-click 
           #(let [servs ;; обновленный вектор услуг
                  (conj (hash-map-to-vector @services) @service-data)] 

              ;; Выполнить последовательно
              (do
                
                ;; Отправить PUT запрос к API 
                ($put->update-services servs)

                ;; Добавить услугу в локальный список услуг
                (reset! services (vector-to-hash-map-set-ids servs))
                
                ;; Очистить поля формы
                (reset! service-data {:description "" :price {:above false}})))}
          {:type "button"
           :class "disabled"})

        ;; Название кнопки
        "Добавить услугу"]])))

(defn services-group-list

  "Отрисовать список услуг в html"

  [services]
  
  [:div.row
   [:div.col-xs-12
    
    ;; Кол-во услуг
    (when (not-empty @services) 
        [:small 
         (str "Колличество добавленных услуг: "
              (count @services))
         [:hr]])

    ;; Список не должен быть пустым
    (if (not-empty @services) 
    
      [:ul.list-group
       (doall
        (for [service (reverse @services)]
        
          ;; Шаблон услуги
          (let [[id data] service] 
            [:li.list-group-item 
             {:key (rand-int 9999)}

             [:div
            
              ;; Заголовок
              [:h5 (:title data)]
            
              ;; Стоимость
              [:p.small
               (str 
                "Цена: "
                (when (-> data :price :above) "от ")
                (-> data :price :sum))
               [:del "Р"]]
            
              ;; Описание
              [:p (:description data)]
            
              ;; Кнопки действий
              [:button.btn.btn-default
               {:type "button"
                :on-click 
                #(do
                   ;; Отправить запрос к API 
                   ($put->update-services 
                    ;; Удалить услугу из локальной копии
                    (hash-map-to-vector (swap! services dissoc id))))} 
             
               ;; Иконка кнопки
               [:span.glyphicon.glyphicon-trash]]]])))]
    
      ;; Если список пуст
      ;; вернуть сообщение
      [:p.text-center "Нет добавленных услуг"])]])

(defn profile-edit-services-subpage

  "UI редактирования услуг;

  Содержит два блока:
  1) Список услуг;
  2) Форма добавления новой услуги;"

  [profile-data]
  (let [;; Создать список услуг с ключами для управления ими
        services (atom (vector-to-hash-map-set-ids (:services @profile-data)))] 
    [:div.row
     [:div.col-xs-8
      
      ;; Заголовок
      [:h4 "Список услуг"]
      [:hr]
            
      ;; Список услуг
      [services-group-list services]]
     
     ;; Форма добавления новой услуги
     [:div.col-xs-4
      [add-service-form services]]]))




;; *******************************
;; -------------------------------
;; Изменение локаций профиля
;; Подстраница Ui компонента Profile-Edit

(defn profile-edit-locations-subpage

  "UI редактирования локаций профиля;

  Содержит список метро и районов
  доступных для добавления в локации;"

  [profile-data]
  [:div.row
   [:div.col-xs-4

    ;; Заголовок
    [:h4 "Станции метро и районы"]
    [:hr]

    ;; Форма выбора локаций
    [live-search-locations profile-data]

    ;; Кнопка отправки данных
    [:button.btn.btn-default
     
     ;; Валидация данных
     (if (b/valid?
          @profile-data
          :locations
          [(fn [in] 
             (and 
              (or (seq? in) (vector? in)) 
              (not-empty in)))])
       
       ;; Запрос к API
       {:type "button"
        :on-click #($put->update-locations profile-data)}

       ;; Ошибка при валидации
       {:type "button"
        :class "disabled"})

     ;; Название
     "Сохранить"]]])




;; ***************************
;; --------------------------
;; Редактирование категорий

(defn profile-edit-categories-subpage

  "UI редактирования категорий профиля;

  Содержит список специализаций (категорий);"

  [profile-data]
  [:div.row
   [:div.col-xs-4
    [:h4 "Специализации"]
    [:hr]
    
    ;; Форма выбора специализаций
    [live-search-categories profile-data]

    ;; Кнопка отправки данных
    [:button.btn.btn-default
     
     ;; Валидация данных
     (if (b/valid?
          @profile-data
          :categories
          [(fn [in] 
             (and 
              (or (seq? in) (vector? in)) 
              (not-empty in)))])
       
       ;; Запрос к API
       {:type "button"
        :on-click #($put->update-categories profile-data)}

       ;; Ошбика валидации
       {:type "button"
        :class "disabled"})

     ;; Название
     "Сохранить"]]])




;; *************************
;; ------------------------
;; UI основные настройки профиля
;; Подстраница компонента Edit-Profile

;; Шаблоны форм для изменения настроек профиля
(def profile-store-edit-base-form-template

  ;; Форма основных настроек для магазина

  [:div.row
   [:div.col-xs-8
    [:div.row
     [:div.col-xs-4.form-group
      (input "Название" :text :name)]
     [:div.col-xs-8.form-group
      (input "Адрес" :text :address)]]]])

(def profile-headhunter-edit-base-form-template

  ;; Форма основных настроек для работодателя

  [:div.row
   [:div.col-xs-8
    [:div.row
     [:div.col-xs-4.form-group
      [:h5 "Представитель компании"]
      (input "Имя" :text :first-name)
      (input "Фамилия" :text :last-name)]
     [:div.col-xs-8.form-group
      [:h5 "Информация компании"]
      (input "Название компании" :text :company.name)
      (input "Адрес" :text :address)]]]])

(def profile-company-edit-base-form-template

  ;; Форма основных настроек для компании

  [:div.row
   [:div.col-xs-8
    [:div.row
     [:div.col-xs-4.form-group
      (input "Название" :text :name)]
     [:div.col-xs-8.form-group
      (input "Адрес" :text :address)]]]])

(def profile-expert-edit-base-form-template

  ;; Форма основных настроек для специалиста

  [:div.row
   [:div.col-xs-8
    [:div.row
     [:div.col-xs-4.form-group
      (input "Имя" :text :first-name)]
     [:div.col-xs-4.form-group
      (input "Фамилия" :text :last-name)]
     [:div.col-xs-2.form-group
      (input "Возраст" :text :age)]
     [:div.col-xs-2.form-group
      (input "Опыт" :text :experience)]
     [:div.col-xs-8.form-group
      (input "Место работы" :text :job)]
     [:div.col-xs-4.form-group
      (input "Стоимость услуг" :text :price)]
     [:div.col-xs-12.form-group
      (row
       "Образование"
       [:textarea.form-control
        {:field :textarea :id :education :rows 8}])]]]])

(defn profile-edit-base-subpage

  "UI редактирования основной информации профиля;

  Определяет тип профиля и выводит соответствующий
  ему шаблон формы редактирования основной информации;"

  [profile-data]
  [:div.row

   ;; Заголовок
   [:div.col-xs-12
    [:h4 "Основная информация"]
    [:hr]]

   (str @profile-data)
  
   (if-not (b/valid?
            @profile-data
            :categories v/required
            :locations v/required
            :contacts v/required
            :services v/required
            :about v/required)
     [:div.col-xs-12 
      [:p.text-warning
       "Ваш профиль заполнен не полностью, поэтому он 
не отображается в результатах поиска."]
      [:br]
      [:br]])

   ;; Определить роль профиля и его форму
   (condp = (keyword (-> @app-state :identity :role))
       
       ;; Специалист
     :expert
     [:div.col-xs-12

      ;; Форма
      [bind-fields
       profile-expert-edit-base-form-template
       profile-data]

      ;; Кнопка отправки данных
      [:button.btn.btn-default
       
       ;; Валидация данных
       (if (b/valid?
            @profile-data
            :first-name v/required
            :last-name v/required
            :age v/required
            :job v/required
            :education v/required
            :experience v/required
            :price v/required)
         
         ;; Запрос к API по клику
         {:type "button"
          :on-click #($put->update-base profile-data)}
         
         ;; Ошибка при валидации - деактивировать форму
         {:type "button"
          :class "disabled"})

       ;; Название кнопки
       "Сохранить"]]

     ;; Компания
     :company
     [:div.col-xs-12

      ;; Форма
      [bind-fields
       profile-company-edit-base-form-template
       profile-data]

      ;; Кнопка отправки данных
      [:button.btn.btn-default
       
       ;; Валидация данных
       (if (b/valid?
            @profile-data
            :name v/required
            :address v/required
            )
         
         ;; Запрос к API по клику
         {:type "button"
          :on-click #($put->update-base profile-data)}
         
         ;; Ошибка при валидации - деактивировать форму
         {:type "button"
          :class "disabled"})

       ;; Название кнопки
       "Сохранить"]]

     ;; Магазин
     :store
     [:div.col-xs-12

      ;; Форма
      [bind-fields
       profile-store-edit-base-form-template
       profile-data]

      ;; Кнопка отправки данных
      [:button.btn.btn-default
       
       ;; Валидация данных
       (if (b/valid?
            @profile-data
            :name v/required
            :address v/required)
         
         ;; Запрос к API по клику
         {:type "button"
          :on-click #($put->update-base profile-data)}
         
         ;; Ошибка при валидации - деактивировать форму
         {:type "button"
          :class "disabled"})

       ;; Название кнопки
       "Сохранить"]]

     ;; Работодатель
     :headhunter
     [:div.col-xs-12

      ;; Форма
      [bind-fields
       profile-headhunter-edit-base-form-template
       profile-data]

      ;; Кнопка отправки данных
      [:button.btn.btn-default
       
       ;; Валидация данных
       (if (b/valid?
            @profile-data
            :first-name v/required
            :last-name v/required
            [:company :name] v/required
            :address v/required)
         
         ;; Запрос к API по клику
         {:type "button"
          :on-click #($put->update-base profile-data)}
         
         ;; Ошибка при валидации - деактивировать форму
         {:type "button"
          :class "disabled"})

       ;; Название кнопки
       "Сохранить"]]

     [:div])])



;; *********************************
;; ---------------------------------
;; Компонент редактирования профиля

(defn profile-edit-ui-component 

  "Компонент редактирования профиля пользователя;

  Создает копию профиля из app-state
  и передает ее в компоненты подстраниц;

  Определяет и возвращает текущую подстраницу;"

  []

  [profile-edit-wrap

   ;; Создать копию профиля из app-state
   (let [profile-data (atom (:profile @app-state))] 

     (fn []

       ;; определить подстраницу
       (condp = (session/get :subpage)

         ;; Основные настройки
         :profile/edit-base 
         [profile-edit-base-subpage profile-data]

         ;; Специализации
         :profile/edit-categories 
         [profile-edit-categories-subpage profile-data]

         ;; Места
         :profile/edit-locations 
         [profile-edit-locations-subpage profile-data]

         ;; Услуги
         :profile/edit-services 
         [profile-edit-services-subpage profile-data]
         
         ;; Контакты
         :profile/edit-contacts 
         [profile-edit-contacts-subpage profile-data]

         ;; Описание
         :profile/edit-about
         [profile-edit-about-subpage profile-data]
         
         ;; Аватар
         :profile/edit-avatar
         [profile-edit-avatar-subpage profile-data]
         
         ;; Файлы
         :profile/edit-files
         [profile-edit-files-subpage profile-data])))])
