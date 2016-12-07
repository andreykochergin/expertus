(ns expertus.auth
  (:require [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [buddy.sign.jws :as jws]
            [ring.util.http-response :as resp]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [expertus.util :as util]
            [expertus.layout :refer [*identity*]]
            [clj-time.core :as time]
            [expertus.db.core :as db]))

;; Ключ шифрования
(def secret "expetrus-malbertus-albertus")

(defn get-account 
  "Получить данные аккаунта"
  [login & {:keys [fields] :or [fields []]}]
  (-> (db/get! "accounts" {:login login} fields) 
      first (dissoc :_id)))

(defn unsign-token 
  "Просмотреть токен"
  [token]
  (try 
    (let [t (jws/unsign token secret)]
      (assoc t :role (keyword (:role t))))
    (catch Exception e (str e))))

(defn check-password 
  "Подверждение пароля"
  [login password]
  (let [acc (first 
             (db/get! "accounts" 
                      {:login login}
                      [:password]))]
    (if (and (not-empty acc) 
             (hashers/check password (:password acc)))
      true false)))

(defn- check-auth-data 
  "Проверка логина и пароля"
  [login password]
  (let [acc (db/get! 
             "accounts" 
             {:login login}
             [:password :role]
             :one? true)]
    (if (and (not-empty acc) 
             (hashers/check password (:password acc)))
      (-> acc
          (dissoc :password)
          (assoc :role (:role acc)))
      false)))

(defn session-handler! 
  "Обработчик создания сессии"
  [request {:keys [login password]}]
  (let [account (check-auth-data login password)]
    (if account
      (let [addr (get request :remote-addr)
            claims 
            {:login login
             :role (:role account)
             :addr addr
             :exp (time/plus (time/now) (time/seconds 3600))}
            token (jws/sign claims secret)] 
        (do (db/update! "accounts" {:login login}
                        {:visited (util/nowf :date-time)})
            (resp/ok {:token token})))
      (resp/bad-request 
       {:message "Неверные имя пользователя или пароль"}))))

(defn- token! 
  "Вернет данные токена"
  [request token]
  (let [token (unsign-token token)]
    (let [addr (get request :remote-addr)]
      (if (= addr (:addr token)) 
        (resp/ok {:result token})
        (resp/not-found {:message "Ошибка авторизации"})))))

;; Валидация
(s/defschema AuthData
  {:login s/Str
   :password s/Str})

(s/defschema AuthToken
  {:token s/Str})

;; Widdleware
(def backend (jws-backend {:secret secret}))

(defn on-error [request response]
  (resp/forbidden 
   {:status 403
    :message (str "Access not authorized")}))

(defn wrap-auth [handler]
  (-> handler
      (wrap-authentication backend)))

;; Маршруты
(defroutes* auth-routes  
  (context* "/auth" []
            :tags ["Auth - авторизация и токены"]
            
            ;; Аутентификация
            (POST* "/" request
                   :body [data AuthData]
                   :return AuthToken
                   :summary "Аутентификация аккаунта"
                   (session-handler! request data))

            ;; Просмотр токена
            (GET* "/session/:token" request
                  :path-params [token :- s/Str]
                  :summary "Просмотр сессии"
                  (token! request token))))
