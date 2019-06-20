(ns qs-clj.fitbit.transforms
  (:require [java-time :as time]
            [qs-clj.measurements :as measurements]
            [qs-clj.units :as units]))

(defn- ->inst
  ([date] (->inst date "00:00:00"))
  ([date time]
   (time/sql-timestamp (time/local-date-time "yyyy-MM-dd HH:mm:ss" (format "%s %s" date time)))))

(defmulti transform (fn [type {:keys [admin-user] :as system} data] type))

(defmethod transform :default
  [_ _ _] [])

(defmethod transform :fat
  [_ {:keys [admin-user]} {:keys [fat]}]
  (->> fat
       (map (fn [{:keys [date fat time] :as entry}]
              {:user-eid (:db/id admin-user)
               :provider :provider/fitbit
               :type :quantity/fat
               :value fat
               :start (->inst date time)
               :end (->inst date time)}))
       (map measurements/->quantity-measurement)))

;; todo deep map camel case...
(defmethod transform :weight
  [_ {:keys [admin-user]} {:keys [weight]}]
  (->> weight
       (map (fn [{:keys [date weight time] :as entry}]
              {:user-eid (:db/id admin-user)
               :provider :provider/fitbit
               :type :quantity/weight
               :value (units/kgs->lbs weight)
               :start (->inst date time)
               :end (->inst date time)}))
       (map measurements/->quantity-measurement)))

(defmethod transform :sleep
  [_ {:keys [admin-user]} {:keys [sleep]}]
  (->> sleep
       ;; todo figure out daily summaries...
       (mapcat (fn [{:keys [date-of-sleep type levels]}]
                 ;; todo handle not-stage sleep
                 (when (= type "stages")
                   (->> (:data levels)
                        (map (fn [{:keys [date-time level seconds]}]
                               {:user-eid (:db/id admin-user)
                                :provider :provider/fitbit
                                :type     :category/sleep
                                :value    (keyword "sleep" level)
                                :start    (-> date-time time/local-date-time time/sql-timestamp)
                                :end      (-> date-time time/local-date-time (time/plus (time/seconds (Integer. seconds))) time/sql-timestamp)}))))))
       (map measurements/->category-measurement)))

(comment
  (let [dateTime "2019-02-28T20:12:00.000"
        seconds "30"]
    (-> dateTime time/local-date-time (time/plus (time/seconds (Integer. seconds))) time/sql-timestamp)))
