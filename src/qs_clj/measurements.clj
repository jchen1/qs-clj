(ns qs-clj.measurements
  (:require [hasch.core :as hasch]
            [qs-clj.db.enums :as enums]))

(defn- ->dedup-key
  [{:keys [user-eid provider type start end]}]
  (hasch/uuid [user-eid provider type start end]))

(defn ->quantity-measurement
  [{:keys [user-eid provider type value start end] :as quantity}]
  {:pre [#_(time/after? end start)
         (contains? (-> enums/quantity->unit keys set) type)
         (contains? enums/providers provider)]}
  {:quantity-measurement/user     user-eid
   :quantity-measurement/provider provider
   :quantity-measurement/key      (->dedup-key quantity)
   :quantity-measurement/type     type
   :quantity-measurement/value    (bigdec value)
   :quantity-measurement/start    start
   :quantity-measurement/end      (or end start)})

(defn ->category-measurement
  [{:keys [user-eid provider type value start end] :as quantity}]
  {:pre [#_(time/after? end start)
         (contains? enums/providers provider)
         (contains? (-> enums/category->categories keys set) type)
         (contains? (-> enums/category->categories type) value)]}
  {:category-measurement/user user-eid
   :category-measurement/provider provider
   :category-measurement/key (->dedup-key quantity)
   :category-measurement/type type
   :category-measurement/value value
   :category-measurement/start start
   :category-measurement/end (or end start)})
