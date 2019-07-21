(ns qs-clj.fitbit.transforms
  (:require [qs-clj.time :as time]
            [qs-clj.measurements :as measurements]
            [qs-clj.units :as units]))

(defn- parse-dt
  ([date time zone]
   (parse-dt (format "%sT%s" date time) zone))
  ([dt zone]
   (-> dt
       (time/local-date-time)
       (time/->date-time zone))))

(defn- dataset-type->interval
  [dataset-type dataset-interval]
  (when-let [f (case dataset-type
                 "minute" time/minutes)]
    (f dataset-interval)))

(defmulti transform (fn [type {:keys [admin-user] :as system} data] type))

(defmethod transform :default
  [_ _ _] [])

(defmethod transform :fat
  [_ {:keys [admin-user]} {:keys [fat]}]
  (let [{:keys [:db/id :user/tz]} admin-user]
    (->> fat
         (map (fn [{:keys [date fat time] :as entry}]
                {:user-eid id
                 :provider :provider/fitbit
                 :type     :quantity/fat
                 :value    fat
                 :start    (parse-dt date time (:user/tz admin-user))}))
         (map measurements/->quantity-measurement))))

(comment
  (time/local-date-time "2019-03-10T00:00:00"))

(defmethod transform :weight
  [_ {:keys [admin-user]} {:keys [weight]}]
  (->> weight
       (map (fn [{:keys [date weight time] :as entry}]
              {:user-eid (:db/id admin-user)
               :provider :provider/fitbit
               :type :quantity/weight
               :value (units/kgs->lbs weight)
               :start (parse-dt date time (:user/tz admin-user))}))
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
                               (let [start (parse-dt date-time (:user/tz admin-user))]
                                 {:user-eid (:db/id admin-user)
                                  :provider :provider/fitbit
                                  :type     :category/sleep
                                  :value    (keyword "sleep" level)
                                  :start    start
                                  :end      (time/plus start (time/seconds (Integer. seconds)))})))))))
       (map measurements/->category-measurement)))

(defn- transform-activity
  [type {:keys [admin-user]} data quantity-type]
  (let [activity-keyword (keyword (format "activities-%s" (name type)))
        intraday-keyword (keyword (format "activities-%s-intraday" (name type)))
        date (->> data activity-keyword first :date-time)
        dataset-type (->> data intraday-keyword :dataset-type)
        dataset-interval (->> data intraday-keyword :dataset-interval)
        _ (assert (and dataset-type dataset-interval) (format "uh oh: %s %s" type data))
        interval (dataset-type->interval
                   dataset-type
                   dataset-interval)
        ret (->> data
                 intraday-keyword
                 :dataset
                 (map (fn [{:keys [time value]}]
                        (let [start (parse-dt date time (:user/tz admin-user))]
                          {:user-eid (:db/id admin-user)
                           :provider :provider/fitbit
                           :type     quantity-type
                           :value    value
                           :start    (parse-dt date time (:user/tz admin-user))
                           :end      (time/plus start interval)})))
                 (map measurements/->quantity-measurement))
        ;; handle daylight savings...
        deduped (->> ret
                     (group-by :quantity-measurement/key)
                     vals
                     (map (fn [vs] (->> vs (sort-by :value) first))))]
    deduped))

(comment
  (> 2 1)
  (group-by :a [{:a 1 :b 2} {:a 1 :b 3} {:a 2 :b 1}]))

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
  (->inst "2015-03-08" "03:00:00")
  (let [dateTime "2019-02-28T20:12:00.000"
        seconds "30"]
    (-> dateTime time/local-date-time (time/plus (time/seconds (Integer. seconds))) time/sql-timestamp))
  (let [measurements (-> "fuck.edn" slurp read-string)]
    (->> measurements
         (group-by :quantity-measurement/key)
         (vals)
         #_#_(sort-by count)
         reverse
         (filter #(> (count %) 1))
         #_count
         #_(take 2))))
