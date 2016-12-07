(ns expertus.db.indexes
  (:require [expertus.db.core :refer [db]]
            [monger.collection :as mc]))

(defn indexes-on [coll]
  (mc/indexes-on db coll))

(defn create-profile-indexes []
  (mc/create-index db "profiles"
                   (array-map :login 1 :uuid 1)
                   {:unique true}))

(defn create-accounts-indexes []
  (mc/create-index db "accounts" 
                   (array-map :login 1 :email 1 :uuid 1)
                   {:unique true}))
