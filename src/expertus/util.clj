(ns expertus.util
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn vec->str
  "Перевести вектор ключевых слов 
  в слова перечисленные через запятую"
  ([vec]
   (if (not-empty vec)
     (apply str (map #(str % ",") vec))))
  ([vec limit]
   (take limit (vec->str vec))))

(defn str->vec
  "Перевести слова перечисленные через запятую 
  в вектор ключевых слов"
  ([string]
   (if (not-empty string)
     (let [out (into []
                     (map #(str/trim %) 
                          (str/split string #",")))]
       (vec (filter #(not-empty %) out))
       (map #(keyword %) out))
     string))
  ([string limit]
   (take limit (str->vec string))))

(defn str->vec-keywords [string]
  (->> string
       (str->vec)
       (map #(keyword %))
       (into [])))

(declare datetime)

(def date-formatter 
  (f/formatter 
   "YYYY-MM-dd" (t/default-time-zone)))

(def date-time-formatter 
  (f/formatter 
   "YYYY-MM-dd HH:mm" (t/default-time-zone)))

(defn valid?
  "Проверить объект #DateTime на годность"
  [date]
  (t/after? date (datetime)))

(defn datetime+months
  "Возвращает #DateTime объект с прибавленным 
колличеством месяцев к текущей дате"
  [months]
  (t/plus (datetime) (t/months months)))

(defn datetime-parse [datetime]
  (t/in-minutes (f/parse date-formatter datetime)))

(defn unparse-datetime
  "Отфармотировать #DateTime объект"
  [date f]
  (f/unparse
   (cond
    (= f :date) date-formatter
    (= f :date-time) date-time-formatter)
   date))

(defn datetime
  "Текущие время и дата UTC+3 Москва"
  []
  (t/from-time-zone 
   (t/now) (t/default-time-zone)))

(defn nowf [f]
  (-> (datetime)
      (unparse-datetime f)))
