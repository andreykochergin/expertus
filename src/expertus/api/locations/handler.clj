(ns expertus.api.locations.handler
  (:require [ring.util.http-response :as resp]
            [compojure.api.sweet :refer :all]
            [expertus.util :as util]
            [expertus.auth :as auth]
            [expertus.db.core :as db]
            [expertus.api.locations.models :as model]
            [monger.util :refer [object-id random-uuid]]
            [monger.operators :as mo]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [schema.core :as s]
            [clj-time.core :as time]))

(def db-coll "locations")

(defn- exist?
  "Проверка локации"
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
          (resp/ok {:message "Локация удалена"}))
        (resp/not-found {:message "Ошибка авторизации"}))
      (resp/not-found {:message "Ничего не найдено"}))))

(defn update! [request token alias category]
  (let [token (auth/unsign-token token)]
    (if-not (exist? alias)
      (if (and (= (:addr token) (:remote-addr request))
               (= :admin (:role token)))
        (do 
          (db/update! db-coll {:alias alias} category)
          (resp/ok {:message "Локация обновлена"}))
        (resp/not-found {:message "Ошибка авторизации"}))
      (resp/not-found {:message "Ничего не найдено"}))))

(defn get! [alias & {:keys [fields]}]
  (let [category (-> (db/get! db-coll {:alias alias}
                              (util/str->vec-keywords fields))
                     first (dissoc :_id))]
    (if-not (empty? category)
      (resp/ok category)
      (resp/not-found
       {:message "Локация не найдена"}))))

(defn search! 
  [& {:keys [fields limit alias title type]}]
  (let [query {}
        query (if-not (nil? alias) {:alias alias} query)
        query (if-not (nil? title)
                (merge query {:title {mo/$regex (str title ".*")
                                      mo/$options "i"}}) query)
        query (if-not (nil? type)
                (merge query {:type type}) query)
        results 
        (map #(dissoc % :_id) 
             (db/query! db-coll
                        :query query
                        :fields (if-not (nil? fields) (util/str->vec fields) [])
                        :limit (if-not (nil? limit) limit 0)))]
    (resp/ok
     {:count (count results)
      :result (reverse results)})))

(defn create! [request token location]
  (let [token (auth/unsign-token token)
        alias (:alias location)]
    (if (and (= (:addr token) (:remote-addr request)) 
             (= :admin (:role token)))
      (if (exist? alias)
        (do 
          (db/create! db-coll (model/location location))
          (resp/ok
           {:status 201
            :message (str "Локация: " alias " - создана")}))
        (resp/not-found
         {:status 400
          :message "Такая локация уже есть"}))
        (resp/not-found
         {:status 400
          :message "Ошибка авторизации"}))))

;; Валидация
(s/defschema Location
  {:alias s/Str
   :title s/Str
   :type s/Str})

;; Маршруты
(defroutes* locations-routes
  (context* "/locations" []
            :tags ["Locations - каталог локаций"]

            ;; Удалить
            (DELETE* "/:alias" request
                     :header-params [token :- s/Str]
                     :path-params [alias :- s/Str]
                     :summary "Удалить локацию"
                     (delete! request token alias))

            ;; Обновить
            (PUT* "/:alias" request
                  :header-params [token :- s/Str]
                  :path-params [alias :- s/Str]
                  :body [location Location]
                  :summary "Обновить локацию"
                  (update! request token alias location))

            ;; Вернуть
            (GET* "/:alias" [alias] 
                  :query-params [{fields :- s/Str nil}]
                  :summary "Данные локации"
                  (get! alias :fields fields))

            ;; Список аккаунтов
            (GET* "/" []
                  :query-params [{type :- s/Str nil}
                                 {title :- s/Str nil}
                                 {alias :- s/Str nil}
                                 {fields :- s/Str nil}
                                 {limit :- Long nil}]
                  :summary "Поик локаций"
                  (search! :type type
                           :title title
                           :alias alias
                           :fields fields
                           :limit limit))

            ;; Создание
            (POST* "/" request
                   :header-params [token :- s/Str]
                   :body [location Location]
                   :summary "Создание локации"
                   (create! request token location))))
