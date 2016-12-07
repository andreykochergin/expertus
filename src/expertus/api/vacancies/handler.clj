(ns expertus.api.vacancies.handler
  (:require [ring.util.http-response :as resp]
            [compojure.api.sweet :refer :all]
            [expertus.util :as util]
            [expertus.auth :as auth]
            [expertus.db.core :as db]
            [expertus.api.vacancies.models :as model]
            [monger.util :refer [object-id random-uuid]]
            [monger.operators :as mo]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [schema.core :as s]
            [clj-time.core :as time]
            [bouncer.core :as b]
            [bouncer.validators :as v]))

(def db-coll "vacancies")

;; Обработчики
(defn get! 
  "Получить вакансию"
  [id & {:keys [fields]}]
  (let [vacancy (-> (db/get! db-coll {:_id id} 
                             (util/str->vec-keywords fields))
                    first
                    (assoc :_id (str id)))]
    (if-not (empty? vacancy)
      (resp/ok vacancy)
      (resp/not-found
       {:status 404
        :message "Вакансия не найдена"}))))

(defn search!
  "Поиск вакансий"
  [& {:keys [owner keywords 
             locations categories
             limit fields]}]
  (let [;; Поисковой запрос
        query {}
        query (if owner {:owner owner} {})
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
        
         ;; Убрать лишние поля
         (map #(assoc % :_id (str (:_id %)))))]
    (let [limit-results (take limit results)] 
      (resp/ok
       {:count (count limit-results)
        :result limit-results}))))

(defn update-vacancy!
  "Обновить вакансию"
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
        (db/update! 
         db-coll {:owner login} 
         (assoc data :updated (util/nowf :date-time)))
        (resp/ok
         {:status 200
          :message "Данные вакансии обновлены"}))
      ;; Ошибка авторизации
      (resp/forbidden
       {:status 403
        :message "Ошибка авторизации"}))))

;; Валидация
(s/defschema Vacancy
  {:title s/Str})

;; Маршруты
(defroutes* vacancies-routes
  
  (context* 
   "/vacancies" []
   :tags ["Vacancies - вакансии"]
   
   ;; Изменить 
   (PUT* "/:id" request
         :header-params [token :- s/Str]
         :path-params [id :- s/Str]
         :body [data Vacancy]
         :summary "Изменить основные данные вакансии"
         (update-vacancy! request token id data))
   
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
