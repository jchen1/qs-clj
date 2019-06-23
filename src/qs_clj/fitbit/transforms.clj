(ns qs-clj.fitbit.transforms
  (:require [java-time :as time]
            [qs-clj.measurements :as measurements]
            [qs-clj.units :as units]))

(defn- dataset-type->interval
  [dataset-type dataset-interval]
  (when-let [f (case dataset-type
                 "minute" time/minutes)]
    (f dataset-interval)))

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
               :start (->inst date time)}))
       (map measurements/->quantity-measurement)))

(defmethod transform :weight
  [_ {:keys [admin-user]} {:keys [weight]}]
  (->> weight
       (map (fn [{:keys [date weight time] :as entry}]
              {:user-eid (:db/id admin-user)
               :provider :provider/fitbit
               :type :quantity/weight
               :value (units/kgs->lbs weight)
               :start (->inst date time)}))
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
                                :end      (-> date-time
                                              time/local-date-time
                                              (time/plus (time/seconds (Integer. seconds)))
                                              time/sql-timestamp)}))))))
       (map measurements/->category-measurement)))

(defn- transform-activity
  [type {:keys [admin-user]} data quantity-type]
  (let [activity-keyword (keyword (format "activities-%s" type))
        intraday-keyword (keyword (format "activities-%s-intraday" type))
        date (->> data activity-keyword first :date-time)
        interval (dataset-type->interval
                   (->> data intraday-keyword :dataset-type)
                   (->> data intraday-keyword :dataset-interval))]
    (->> data
         intraday-keyword
         :dataset
         (map (fn [{:keys [time value]}]
                {:user-eid (:db/id admin-user)
                 :provider :provider/fitbit
                 :type     quantity-type
                 :value    value
                 :start    (->inst date time)
                 ;; todo make this not shit
                 :end      (time/sql-timestamp (time/plus (time/local-date-time (->inst date time)) interval))}))
         (map measurements/->quantity-measurement))))

(defmethod transform :heart
  [type system data]
  (transform-activity type system data :quantity/heart-rate))

(defmethod transform :calories
  [type system data]
  (transform-activity type system data :quantity/calories))

(defmethod transform :steps
  [type system data]
  (transform-activity type system data :quantity/steps))

(defmethod transform :distance
  [type system data]
  (transform-activity type system data :quantity/distance))

(defmethod transform :floors
  [type system data]
  (transform-activity type system data :quantity/floors-climbed))

(defmethod transform :elevation
  [type system data]
  (transform-activity type system data :quantity/elevation))

(comment
  (ns-unmap 'qs-clj.fitbit.transforms 'transform)
  (transform :fat nil nil)
  (->inst "2019-03-01" "00:00:00")
  (let [dateTime "2019-02-28T20:12:00.000"
        seconds "30"]
    (-> dateTime time/local-date-time (time/plus (time/seconds (Integer. seconds))) time/sql-timestamp)))
