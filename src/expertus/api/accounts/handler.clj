(ns expertus.api.accounts.handler
  (:require [ring.util.http-response :as resp]
            [compojure.api.sweet :refer :all]
            [expertus.util :as util]
            [expertus.auth :as auth]
            [expertus.db.core :as db]
            [expertus.api.accounts.models :as model]
            [expertus.api.profiles.models :as profiles-model]
            [monger.util :refer [object-id random-uuid]]
            [monger.operators :as mo]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [schema.core :as s]
            [clj-time.core :as time]))

(def db-coll "accounts")

;; Обработчики
(defn delete! [request token login]
  (let [token (auth/unsign-token token)
        account-exist? 
        (empty?
         (db/get! "accounts" {:login login} [:login]))]
    (if-not account-exist?
      (if (and (= (:addr token) (:remote-addr request))
               (or (= login (:login token))
                   (= :admin (:role token))))
        (do
          (db/delete! "profiles" {:login login})
          (db/delete! "accounts" {:login login})
          (resp/accepted
           {:status 202
            :message (str "Аккаунт: " login " - удален")}))
        (resp/forbidden
         {:status 403
          :message "Доступ запрещен"}))
      (resp/not-found
         {:status 404
          :message "Аккаунт не найден"}))))

(defn change-login! [request token login password new-login]
  (let [token (auth/unsign-token token)]
    (if (and (= (:addr token) (:remote-addr request))
             (or (= login (:login token))
                 (= :admin (:role token)))
             (auth/check-password login password))
      (do
        (db/update! "accounts" {:login login} 
                    {:login new-login
                     :updated (util/nowf :date-time)})
        (resp/accepted
         {:status 201
          :message (str "Аккаунт: " login " - логин изменен на:" new-login)}))
      (resp/forbidden
       {:status 403
        :message (str "Доступ запрещен")}))))

(defn change-role! [request token login new-role]
  (let [token (auth/unsign-token token)]
    (if (and (= (:addr token) (:remote-addr request))
             (= :admin (:role token)))
      (do
        (db/update! "accounts" {:login login} 
                    {:role new-role
                     :updated (util/nowf :date-time)})
        (resp/accepted
         {:status 201
          :message (str "Аккаунт: " login " - изменен тип")}))
      (resp/forbidden
       {:status 403
        :message (str "Доступ запрещен")}))))

(defn change-password! 
  [request token login password new-password]
  (let [token (auth/unsign-token token)]
    (if (and (<= 6 (count password))
             (<= 6 (count new-password)))
      (if (and (= (:addr token) (:remote-addr request))
               (or (= login (:login token))
                   (= :admin (:role token)))
               (auth/check-password login password))
        (do
          (db/update! "accounts" {:login login}
                      {:password (hashers/encrypt new-password)
                       :updated (util/nowf :date-time)})
          (resp/ok
           {:status 200
            :message "Пароль изменен"}))
        (resp/forbidden
         {:status 403
          :message "Ошбика авторизации"}))
      (resp/bad-request
       {:status 400
        :message "Некорректные данные"}))))

(defn update! [request token login account]
  (let [token (auth/unsign-token token)]
    (if (and (= (:addr token) (:remote-addr request)) 
             (or (= login (:login token))
                 (= :admin (:role token)))) 
      (do
        (db/update! "accounts" {:login login}
                    (assoc account :updated (util/nowf :date-time)))
        (resp/ok
         {:status 200
          :message (str "Аккаунт: " login " - обновлен")}))
      (resp/forbidden
       {:status 403
        :message "Доступ запрещен"}))))

(defn get! 
  [request token login & {:keys [fields]}]
  (let [token (auth/unsign-token token)]
    (if (and (= (:addr token) (:remote-addr request))
             (or (= login (:login token))
                 (= :admin (:role token)))) 
      (let [account (-> (db/get! "accounts" {:login login}
                                 (util/str->vec-keywords fields))
                        first
                        (dissoc :password))
            account (assoc account 
                      :profile (str (:profile account)))]
        (if-not (empty? account) 
          (resp/ok account)
          (resp/not-found
           {:status 404
            :message "Аккаунт не найден"})))
      (resp/forbidden 
       {:status 403
        :message "Доступ запрещен"}))))

(defn search! 
  [request token & {:keys [fields limit email login role]}]
  (let [token (auth/unsign-token token)]
    (if (and (= (:addr token) (:remote-addr request)) 
             (= :admin (:role token))) 
      (let [query {}
            query (if-not (nil? email) {:email {mo/$regex (str email ".*")
                                                mo/$options "i"}} query)
            query (if-not (nil? login)
                    (merge query {:login {mo/$regex (str login ".*")
                                          mo/$options "i"}}) query)
            query (if-not (nil? role) (merge query {:role role}) query)
            results 
            (->> (db/query! db-coll
                  :query query
                  :fields (if-not (nil? fields) (util/str->vec fields) [])
                  :limit (if-not (nil? limit) limit 0))
                 (map #(dissoc % :password)))]
        (resp/ok 
         {:count (count results)
          :result results}))
      (resp/forbidden
       {:status 403
        :message "Доступ запрещен"}))))

(defn create! [account]
  (let [hash-password (hashers/encrypt (:password account))
        account (assoc account :password hash-password)
        role (keyword (:role account))
        login (:login account)
        account-exists? 
        (empty? 
         (db/get! "accounts" {:login login} 
                  [:login]))] 
    ;; Валидация введенного типа аккаунта
    (if (or (= :expert role)
            (= :company role)
            (= :store role)
            (= :headhunter role))
      (if account-exists?
        (do
          (db/create! 
           "accounts"
           (model/account
            (assoc account
              ;; Создать профиль с выбранным типом
              ;; и добавить его ObjectId к полю
              ;; profile в аккаунте пользователя
              :profile 
              (:_id
               (db/create! 
                "profiles" 
                (condp = (keyword (:role account))
                  ;; Профиль специалиста
                  :expert (profiles-model/expert login)
                  ;; Профиль компании
                  :company (profiles-model/company login)
                  ;; Профиль магазина
                  :store (profiles-model/store login)
                  ;; Профиль работодателя
                  :headhunter (profiles-model/headhunter login))
                :return? true)))))
          (resp/created
           {:status 201
            :message (str "Аккаунт: " (:login account) " - создан")}))
        (resp/bad-request
         {:status 400
          :message (str "Аккаунт: " (:login account) " - уже существует")}))
      (resp/bad-request
       {:status 400
        :message "Неверный тип аккаунта"}))))

;; Валидация
(s/defschema ChangePassword
  {:password s/Str
   :new-password s/Str})

(s/defschema AccountUpdate
  {:email s/Str
   :mailing Boolean})

(s/defschema AccountCreate
  {:login s/Str
   :password s/Str
   :email s/Str
   :role s/Str})

;; Маршруты
(defroutes* accounts-routes
  (context* "/accounts" []
            :tags ["Accounts - пользовательские аккаунты"]

            ;; Удалить
            (DELETE* "/:login" request
                     :header-params [token :- s/Str]
                     :path-params [login :- s/Str]
                     :summary "Удалить аккаунт"
                     (delete! request token login))

            ;; Изменить тип
            (PUT* "/change-role/:login" request
                  :header-params [token :- s/Str]
                  :path-params [login :- s/Str]
                  :body-params [new-role :- s/Str]
                  :summary "Изменить тип аккаунта"
                  (change-role! request token login new-role))

            ;; Изменить логин
            (PUT* "/change-login/:login" request
                  :header-params [token :- s/Str]
                  :path-params [login :- s/Str]
                  :body-params [new-login :- s/Str
                                password :- s/Str]
                  :summary "Изменить логин аккаунта"
                  (change-login! request token login password new-login))

            ;; Изменить пароль
            (PUT* "/change-password/:login" request
                  :header-params [token :- s/Str]
                  :path-params [login :- s/Str]
                  :body-params [new-password :- s/Str
                                old-password :- s/Str]
                  :summary "Изменить пароль аккаунта"
                  (change-password! request token login
                                    old-password new-password))
            
            ;; Обновить
            (PUT* "/:login" request
                  :header-params [token :- s/Str]
                  :path-params [login :- s/Str]
                  :body [account AccountUpdate]
                  :summary "Обновить основную информацию аккаунта"
                  (update! request token login account))

            ;; Вернуть
            (GET* "/:login" request 
                  :header-params [token :- s/Str]
                  :path-params [login :- s/Str]
                  :query-params [{fields :- s/Str nil}]
                  :summary "Данные аккаунта"
                  (get! request token login :fields fields))

            ;; Список аккаунтов
            (GET* "/" request
                  :header-params [token :- s/Str]
                  :query-params [{login :- s/Str nil}
                                 {email :- s/Str nil}
                                 {role :- s/Str nil}
                                 {fields :- s/Str nil}
                                 {limit :- Long nil}]
                  :summary "Поик аккаунтов"
                  (search! request token
                         :login login
                         :email email
                         :role role
                         :fields fields
                         :limit limit))

            ;; Создание
            (POST* "/" []
                   :body [account AccountCreate]
                   :summary "Создание аккаунта"
                   (create! account))))
