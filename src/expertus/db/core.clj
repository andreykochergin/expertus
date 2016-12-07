(ns expertus.db.core
    (:require [monger.core :as mg]
              [monger.collection :as mc]
              [monger.operators :refer :all]
              [monger.query :as mq]
              [environ.core :refer [env]]
              monger.joda-time))

(declare db exist? query! delete! update! get! create!)

;; Tries to get the Mongo URI from the environment variable
(defonce db (let [uri (or (:database-url env) "mongodb://127.0.0.1/expertus_dev")
                  {:keys [db]} (mg/connect-via-uri uri)]
              db))

(defn exist? [coll query]
  (-> (get! coll query [:_id] :one? true) empty?))

(defn query!
  "Вернуть пользователей в соотвествии с аргументами"
  [coll & {:keys [query fields sort limit skip]
           :or {query {} fields [] sort [:_id -1]
                limit 0 skip 0}}]
  (mq/with-collection db coll
    (mq/find query)
    (mq/fields fields)
    (mq/sort (apply array-map sort))
    (mq/limit limit)
    (mq/skip skip)))

(defn delete!
  "Удалить документ"
  [coll query]
  (mc/remove db coll query))

(defn update!
  "Обновить документ"
  [coll query doc]
  (mc/update db coll query {$set doc}))

(defn create!
  "Добавить документ(ы)"
  [coll docs & {:keys [return?]}]
  (if (vector? docs)
    (mc/insert-batch db coll docs)
    (if return?
      (mc/insert-and-return db coll docs)
      (mc/insert db coll docs))))

(defn get!
  ;; Вернуть все коллекцию
  ([coll]
   (map #(assoc % :_id (str (:_id %))) 
        (mc/find-maps db coll)))

  ;; Вернуть данные по запросу
  ([coll query]
   (map #(assoc % :_id (str (:_id %)))
        (mc/find-maps db coll query)))

  ;; Вернуть указанные поля по запросу
  ([coll query fields & {:keys [one?]}]
   (if one?
     (let [doc (mc/find-one-as-map db coll query fields)]
       (assoc doc :_id (str (:_id doc))))
     (map #(assoc % :_id (str (:_id %))) 
          (mc/find-maps db coll query fields)))))
