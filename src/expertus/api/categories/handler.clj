(ns expertus.api.categories.handler
  (:require [ring.util.http-response :as resp]
            [compojure.api.sweet :refer :all]
            [expertus.util :as util]
            [expertus.auth :as auth]
            [expertus.db.core :as db]
            [expertus.api.categories.models :as model]
            [monger.util :refer [object-id random-uuid]]
            [monger.operators :as mo]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [schema.core :as s]
            [clj-time.core :as time]))

(def db-coll "categories")

(defn- exist?
  "Проверка категории"
  [alias]
  (-> (db/get! db-coll {:alias alias} [:alias])
      first empty?))

;; Обработчики
(defn delete! [request token alias]
  (let [token (auth/unsign-token token)]
    (if-not (exist? alias)
      (if (and (= (:addr token) (:remote-addr request))
               (= :admin (:role token)))
        (do
          (db/delete! db-coll {:alias alias})
          (resp/ok {:message "Категория удалена"}))
        (resp/not-found {:message "Ошибка авторизации"}))
      (resp/not-found {:message "Ничего не найдено"}))))

(defn update! [request token alias category]
  (let [token (auth/unsign-token token)]
    (if-not (exist? alias)
      (if (and (= (:addr token) (:remote-addr request))
               (= :admin (:role token)))
        (do 
          (db/update! db-coll {:alias alias} category)
          (resp/ok {:message "Категория обновлена"}))
        (resp/not-found {:message "Ошибка авторизации"}))
      (resp/not-found {:message "Ничего не найдено"}))))

(defn get! [alias & {:keys [fields]}]
  (let [category (-> (db/get! db-coll {:alias alias}
                              (util/str->vec-keywords fields))
                     first (dissoc :_id))]
    (if-not (empty? category)
      (resp/ok category)
      (resp/not-found
       {:message "Категория не найдена"}))))

(defn search! [& {:keys [fields limit alias title]}]
  (let [query {}
        query (if-not (nil? alias) {:alias alias} query)
        query (if-not (nil? title)
                (merge query {:title {mo/$regex (str title ".*")
                                      mo/$options "i"}}) query)
        results 
        (map #(dissoc % :_id) 
             (db/query! db-coll
                        :query query
                        :fields (if-not (nil? fields) (util/str->vec fields) [])
                        :limit (if-not (nil? limit) limit 0)))]
    (resp/ok
     {:count (count results)
      :result (reverse results)})))

(defn create! [request token category]
  (let [token (auth/unsign-token token)
        alias (:alias category)]
    (if (and (= (:addr token) (:remote-addr request)) 
             (= :admin (:role token)))
      (if (exist? alias)
        (do 
          (db/create! db-coll (model/category category))
          (resp/ok
           {:status 201
            :message (str "Категория: " alias " - создана")}))
        (resp/not-found
         {:status 400
          :message "Такая категория уже есть"}))
      (resp/not-found
       {:status 400
        :message "Ошибка авторизации"}))))

;; Валидация
(s/defschema Category
  {:alias s/Str
   :title s/Str
   :description s/Str
   :mimg s/Str
   :pimg s/Str})

;; Маршруты
(defroutes* categories-routes
  (context* "/categories" []
            :tags ["Categories - каталог категорий"]

            ;; Удалить
            (DELETE* "/:alias" request
                     :header-params [token :- s/Str]
                     :path-params [alias :- s/Str]
                     :summary "Удалить категорию"
                     (delete! request token alias))

            ;; Обновить
            (PUT* "/:alias" request
                  :header-params [token :- s/Str]
                  :path-params [alias :- s/Str]
                  :body [category Category]
                  :summary "Обновить категорию"
                  (update! request token alias category))

            ;; Вернуть
            (GET* "/:alias" [alias] 
                  :query-params [{fields :- s/Str nil}]
                  :summary "Данные категории"
                  (get! alias :fields fields))

            ;; Список
            (GET* "/" []
                  :query-params [{title :- s/Str nil}
                                 {alias :- s/Str nil}
                                 {fields :- s/Str nil}
                                 {limit :- Long nil}]
                  :summary "Поик категорий"
                  (search! :title title
                           :alias alias
                           :fields fields
                           :limit limit))

            ;; Создание
            (POST* "/" request
                   :header-params [token :- s/Str]
                   :body [category Category]
                   :summary "Создание категории"
                   (create! request token category))))
