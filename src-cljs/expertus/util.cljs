(ns expertus.util
  (:require [clojure.string :as str]))

(defn translate-role
  "Перевести ключ роль в название"
  [role]
  (condp = (keyword role)
    :expert "специалист"
    :company "компания"
    :store "магазин"
    :headhunter "работодатель"
    :admin "администратор"))

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
