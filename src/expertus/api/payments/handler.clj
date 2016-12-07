(ns expertus.api.payments.handler
  (:require [ring.util.http-response :as resp]
            [compojure.api.sweet :refer :all]
            [expertus.util :as util]
            [expertus.auth :as auth]
            [expertus.db.core :as db]
            [expertus.api.payments.models :as model]
            [monger.util :refer [object-id random-uuid]]
            [monger.operators :as mo]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :as hashers]
            [schema.core :as s]
            [clj-time.core :as time]))

(def db-coll "payments")

(defn payment! [request token payment]
  (let [token (auth/unsign-token token)
        account (auth/get-account (:login token) :fields [:coins :transactions])]
    ;; Проверка токена  и права доступа
    (if (and (not-empty account)
             (= (:addr token) (:remote-addr request))
             (= (:login token) (:login payment)))
      (let [coins (+ (:coins account) (:coins payment))
            payment (model/payment payment)
            transactions (:transactions account)]
        (db/update! "accounts" {:login (:login token)} {:coins coins})
        (db/update! "accounts" {:login (:login token)} 
                    {:transactions 
                     (conj transactions
                      {:coins (:coins payment)
                       :description "Пополнение баланса"
                       :datetime (util/nowf :date-time)})})
        (db/create! db-coll payment)
        (resp/ok
         {:status 200
          :message "Платеж выполнен"}))
      (resp/forbidden
       {:status 403
        :message "Ошибка авторизации"}))))

(defn search! [request token & {:keys [date login coins fields limit]}]
  ;; Данные токена
  (let [token (auth/unsign-token token)
        account (auth/get-account (:login token) :fields [:login])]
    ;; Проверка равенства ip-addr токена с ip-addr запроса
    ;; Установка правил доступа к ресурсу
    (if (and (not-empty account) 
             (= (:addr token) (:remote-addr request))
             (= :admin (:role token)))
      ;; Если авторизация успешна
      ;; Получить результаты в соотвествии с запросом
      (let [results 
            (->> (let [query {}
                       query (if-not (nil? login) 
                               {:login login}
                               {})
                       query (if-not (nil? date) 
                               (merge query 
                                      {:datetime 
                                       {mo/$regex date}}) 
                               query)
                       query (if-not (nil? coins) 
                               (merge query {:coins coins}) 
                               query)] 
                   (db/query! db-coll
                    :query query
                    :fields (if-not (nil? fields) (util/str->vec fields)[])
                    :limit (if-not (nil? limit) limit 0)))
                 (map #(dissoc % :_id)))]
        ;; Ответ с данными в формате JSON
        (resp/ok
         {:status 200
          :count (count results)
          :result results}))
      ;; Авторизация не удалась
      (resp/forbidden 
       {:status 403
        :message "Ошибка авторизации"}))))

;; Валидация
(s/defschema SearchFields
  {})

(s/defschema Payment
  {:login s/Str
   :coins Long})

;; Маршруты
(defroutes* payments-routes
  (context* "/payments" []
            :tags ["Payments - платежи и сервисы"]

            ;;  Добавить платеж
            (POST* "/" request
                   :header-params [token :- s/Str]
                   :body [payment Payment]
                   :summary "Добавить платеж"
                   (payment! request token payment))

            ;; Просмотр и поиск платежей
            (GET* "/" request
                  :header-params [token :- s/Str]
                  :query-params [{date :- s/Str nil}
                                 {coins :- Long nil}
                                 {login :- s/Str nil}
                                 {fields :- s/Str nil}
                                 {limit :- Long nil}]
                  :summary "Поиск платежей"
                  (search! request token
                           :date date
                           :coins coins
                           :login login
                           :fields fields
                           :limit limit))))
