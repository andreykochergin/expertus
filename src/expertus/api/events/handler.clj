(ns expertus.api.profiles.handler
  (:require [ring.util.http-response :as resp]
            [compojure.api.sweet :refer :all]
            [expertus.util :as util]
            [expertus.auth :as auth]
            [expertus.db.core :as db]
            [expertus.api.profiles.models :as model]
            [monger.util :refer [object-id random-uuid]]
            [monger.operators :as mo]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [schema.core :as s]
            [clj-time.core :as time]
            [bouncer.core :as b]
            [bouncer.validators :as v]))

(def db-coll "profiles")

;; Обработчики
(defn update-about!
  "Обновить описание профиля"
  [request token login data]
  (let [token (auth/unsign-token token)]
    ;; Проверка токена и авторизация
    (if (and (= (:addr token) (:remote-addr request))
             (or (= login (:login token))
                 (= :admin (:role token))))
      (do
        (db/update! 
         db-coll {:login login}
         (assoc data :updated (util/nowf :date-time)))
        (resp/ok
         {:status 200
          :message "Данные профиля обновлены"}))
      (resp/forbidden
       {:status 403
        :message "Ошибка авторизации"}))))

(defn update-contacts!
  "Обновить список контактов"
  [request token login data]
  (let [token (auth/unsign-token token)]
    ;; Проверка токена и авторизация
    (if (and (= (:addr token) (:remote-addr request))
             (or (= login (:login token))
                 (= :admin (:role token))))
      (do
        (db/update! 
         db-coll {:login login}
         {:contacts (:contacts data)
          :updated (util/nowf :date-time)})
        (resp/ok
         {:status 200
          :message "Данные профиля обновлены"}))
      (resp/forbidden
       {:status 403
        :message "Ошибка авторизации"}))))

(defn update-services!
  "Обновить список услуг"
  [request token login data]
  (let [token (auth/unsign-token token)]
    ;; Проверка токена и авторизация
    (if (and (= (:addr token) (:remote-addr request))
             (or (= login (:login token))
                 (= :admin (:role token))))
      (do
        (db/update! 
         db-coll {:login login}
         {:services (:services data)
          :updated (util/nowf :date-time)})
        (resp/ok
         {:status 200
          :message "Данные профиля обновлены"}))
      (resp/forbidden
       {:status 403
        :message "Ошибка авторизации"}))))

(defn expanded-get! [request token login & {:keys [fields]}]
  (let [t (auth/unsign-token token)]
    (let [profile (-> (db/get! "profiles" {:login login}
                               (util/str->vec-keywords fields))
                      first (dissoc :_id))]
      (if-not (empty? profile)
        (if (and (= (:addr t) (:remote-addr request))
                 (or (= login (:login t))
                     (= :admin (:role t)))) 
          (resp/ok profile)
          (resp/forbidden
           {:status 403
            :message "Ошибка авторизации"}))
        (resp/not-found
         {:status 404
          :message "Профиль не найден"})))))

(defn get! [login & {:keys [fields]}]
  (let [profile (-> (db/get! "profiles" {:login login} 
                             (util/str->vec-keywords fields))
                    first
                    (dissoc :_id :uuid :subscription
                            :updated :created))]
    (if-not (empty? profile)
      (resp/ok profile)
      (resp/not-found
       {:status 404
        :message "Профиль не найден"}))))

(defn search!
  "Поиск профилей"
  [& {:keys [login role keywords 
             locations categories
             limit fields]}]
  (let [;; Поисковой запрос
        query {}
        query (if login {:login login} {})
        query (if role (merge query {:role role}) query)
        query (if keywords
                (merge 
                 query 
                 {:keywords 
                  {mo/$in (util/str->vec keywords)}}) 
                query)
        query (if locations
                (merge 
                 query 
                 {:locations 
                  {mo/$in (util/str->vec locations)}}) 
                query)
        query (if categories
                (merge 
                 query 
                 {:categories 
                  {mo/$in (util/str->vec categories)}})
                query)

        ;; Результаты поиска
        results 
        (->> 

         ;; Запрос к БД
         (db/query! 
          db-coll
          :query query
          ;;:fields (if fields (util/str->vec fields) [])
          ;;:limit (if limit limit 0)
          )

         ;; Перемешать
         (shuffle)
        
         ;; Отфильтровать
         ;; по валидации
         (filter 
          #(and 
            (b/valid? 
             %
             ;; Базовы поля
             :locations v/required
             :categories v/required
             :about v/required)
            ;; Специфичные поля
            (condp = (keyword (:role %))
              :store 
              (b/valid? % 
                        :name v/required
                        :address v/required)
              :company 
              (b/valid? % 
                        :name v/required 
                        :address v/required)
              :expert 
              (b/valid? %
                        :first-name v/required
                        :last-name v/required)
              :headhunter
              (b/valid? %
                        :first-name v/required
                        :last-name v/required
                        [:company :name] v/required
                        :address v/required)
              false)))

         ;; Убрать лишние поля
         (map #(dissoc % :_id :uuid :updated)))]
    (let [limit-results (take limit results)] 
      (resp/ok
       {:count (count limit-results)
        :result limit-results}))))

(defn update-profile!
  "Обновит поля профиля"
  [request token login data]
  ;; Дешифровать токен
  (let [token (auth/unsign-token token)
        user (:login token)
        role (:role token)
        addr (:addr token)]
    ;; Проверка авторизации
    (if (and (= addr (:remote-addr request))
             (or (= role :admin)
                 (= user login)))
      ;; Если доступ разрешен
      ;; обновить профиль и вернуть ответ
      (do
        (db/update! db-coll {:login login} 
           (assoc data :updated (util/nowf :date-time)))
        (resp/ok
         {:status 200
          :message "Данные профиля обновлены"}))
      ;; Ошибка авторизации
      (resp/forbidden
       {:status 403
        :message "Ошибка авторизации"}))))

;; Валидация
(s/def Contacts
  [(s/maybe 
    {:type s/Keyword
     :content s/Str})])

(s/def Locations
  [(s/maybe s/Str)])

(s/def Categories
  [(s/maybe s/Str)])

;; Поисковой запрос локаций
(s/defschema ProfileContacts
  {:contacts Contacts})

;; Новая услуга
(s/def ServicePrice
  {:sum s/Str
   :above Boolean})

(s/def Services
  [(s/maybe 
    {:title s/Str
     :price ServicePrice
     :description s/Str})])

(s/defschema ProfileServices
  {:services Services})

;; Новое ключевое слово
(s/defschema ProfileKeyword
  {})

;; Локации профиля
(s/defschema ProfileLocations
  {:locations Locations})

;; Категории профиля
(s/defschema ProfileCategories
  {:categories Categories})

;; Аватар
(s/defschema ProfileAvatar
  {:avatar s/Str})

;; Описание
(s/defschema ProfileAbout
  {:about s/Str})

;; Данные работодателя
(s/defschema ProfileHeadhunter
  {:first-name s/Str
   :last-name s/Str
   :address s/Str
   :company {:name s/Str}})

;; Специфические данные магазина
(s/defschema ProfileStore
  {:name s/Str
   :address s/Str})

;; Специфические данные компании
(s/defschema ProfileCompany
  {:name s/Str
   :address s/Str})

;; Специфические данные специалиста
(s/defschema ProfileExpert
  {:first-name s/Str
   :last-name s/Str
   :age s/Str
   :experience s/Str
   :education s/Str
   :job s/Str
   :price s/Str})

;; Поисковой запрос
(s/defschema SearchQuery
  {:limit s/Int
   :role s/Str
   :categories [(s/maybe s/Str)]
   :locations [(s/maybe s/Str)]})

;; Маршруты
(defroutes* profiles-routes
  
  (context* 
   "/profiles" []
   :tags ["Profiles - профили сообщества"]
   
   ;; Полные данные профиля
   (GET* "/expanded/:login" request
         :header-params [token :- s/Str]
         :path-params [login :- s/Str]
         :query-params [{fields :- s/Str []}]
         :summary "Расширенная информация"
         (expanded-get! request token login
                        :fields fields))

   ;; Обнвовить описание
   (PUT* "/about/:login" request
         :header-params [token :- s/Str]
         :path-params [login :- s/Str]
         :body [data ProfileAbout]
         :summary "Оновить описание профиля"
         (update-about! request token login data))

   ;; Обновить контакты
   (PUT* "/contacts/:login" request
          :header-params [token :- s/Str]
          :path-params [login :- s/Str]
          :body [data ProfileContacts]
          :summary "Оновить список контактов"
          (update-contacts! request token login data))

   ;; Обновить услуги
   (PUT* "/services/:login" request
          :header-params [token :- s/Str]
          :path-params [login :- s/Str]
          :body [data ProfileServices]
          :summary "Оновить список услуг"
          (update-services! request token login data))

   ;; Изменить локации профиля
   (PUT* "/locations/:login" request
         :header-params [token :- s/Str]
         :path-params [login :- s/Str]
         :body [data ProfileLocations]
         :summary "Изменить локации профиля"
         (update-profile! request token login data))
   
   ;; Изменить категории профиля
   (PUT* "/categories/:login" request
         :header-params [token :- s/Str]
         :path-params [login :- s/Str]
         :body [data ProfileCategories]
         :summary "Изменить категории профиля"
         (update-profile! request token login data))

   ;; Изменить общие данные
   ;; профиля работодателя
   (PUT* "/headhunter/:login" request
         :header-params [token :- s/Str]
         :path-params [login :- s/Str]
         :body [data ProfileHeadhunter]
         :summary "Изменить основные данные профиля работодателя"
         (update-profile! request token login data))

   ;; Изменить общие данные
   ;; профиля магазина
   (PUT* "/store/:login" request
         :header-params [token :- s/Str]
         :path-params [login :- s/Str]
         :body [data ProfileStore]
         :summary "Изменить основные данные профиля магазина"
         (update-profile! request token login data))

   ;; Изменить общие данные
   ;; профиля компании
   (PUT* "/company/:login" request
         :header-params [token :- s/Str]
         :path-params [login :- s/Str]
         :body [data ProfileCompany]
         :summary "Изменить основные данные профиля компании"
         (update-profile! request token login data))
   
   ;; Изменить общие данные
   ;; профиля специалиста
   (PUT* "/expert/:login" request
         :header-params [token :- s/Str]
         :path-params [login :- s/Str]
         :body [data ProfileExpert]
         :summary "Изменить основные данные профиля специалиста"
         (update-profile! request token login data))
   
   ;; Вернуть профиль
   (GET* "/:login" [login]
         :query-params [{fields :- s/Str []}]
         :summary "Данные профиля"
         (get! login :fields fields))

   ;; Поиск профилей
   (GET* "/" []
         :query-params 
         [{categories :- s/Str false}
          {locations :- s/Str false}
          {keywords :- s/Str false}
          {role :- s/Str false}
          {login :- s/Str false}
          {fields :- s/Str false}
          {limit :- Long 100}]
         :summary "Поиск профилей"
         (search!
          :categories categories
          :locations locations
          :keywords keywords
          :role role
          :login login
          :fields fields
          :limit limit))))
