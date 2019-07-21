(ns qs-clj.measurements
  (:require [hasch.core :as hasch]
            [qs-clj.db.enums :as enums]
            [qs-clj.time :as time]))

(defn- ->dedup-key
  [{:keys [user-eid provider type start end]}]
  (hasch/uuid [user-eid provider type start end]))

(defn- parse-measurement
  [{:keys [start end] :as measurement}]
  (merge measurement
         {:start (time/->inst start)
          :end   (time/->inst (or end start))}))

(defn ->quantity-measurement*
  [{:keys [provider type value start end] :as measurement}]
  {:pre [#_(time/after? end start)
         (contains? (-> enums/quantity->unit keys set) type)
         (contains? enums/providers provider)]}
  {:quantity-measurement/provider provider
   :quantity-measurement/key      (->dedup-key measurement)
   :quantity-measurement/type     type
   :quantity-measurement/value    (bigdec value)
   :quantity-measurement/start    start
   :quantity-measurement/end      end})

(defn ->category-measurement*
  [{:keys [provider type value start end] :as measurement}]
  {:pre [#_(time/after? end start)
         (contains? enums/providers provider)
         (contains? (-> enums/category->categories keys set) type)
         (contains? (-> enums/category->categories type) value)]}
  {:category-measurement/provider provider
   :category-measurement/key      (->dedup-key measurement)
   :category-measurement/type     type
   :category-measurement/value    value
   :category-measurement/start    start
   :category-measurement/end      end})

(defn ->quantity-measurement [m] (-> m parse-measurement ->quantity-measurement*))
(defn ->category-measurement [m] (-> m parse-measurement ->category-measurement*))
