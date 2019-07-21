(ns qs-clj.time
  (:require [java-time]
            [java-time :as time]
            [potemkin :as potemkin])
  (:import [java.time ZonedDateTime LocalDate]))

(potemkin/import-vars
  [java-time

   local-date-time
   local-date
   local-time
   zoned-date-time
   iterate
   period])

;; goal - pass around local-date & zoned-date-time everywhere...

(defn- date-time->inst
  [^ZonedDateTime zoned-date-time]
  (java-time/sql-timestamp zoned-date-time))

(defn- local-date->inst
  [^LocalDate local-date]
  (java-time/sql-date local-date))

(def ^:const type->inst-fn
  {ZonedDateTime date-time->inst
   LocalDate     local-date->inst})

(defn ->inst
  [t]
  (if-let [->inst-fn (get type->inst-fn (type t))]
    (->inst-fn t)
    (throw (ex-info "Type not supported!" {:object t
                                           :type (type t)}))))

(defn inst->date-time
  ([inst] (inst->date-time inst (time/zone-id)))
  ([inst zone]
   (-> inst (java-time/zoned-date-time zone))))

(defn ->date-time
  [& args] (apply java-time/zoned-date-time args))

(defn now
  ([] (now (time/zone-id)))
  ([zone]
   (time/zoned-date-time (time/local-date-time) zone)))

(defn plus [o & args] (apply java-time/plus o args))
(defn minus [o & args] (apply java-time/minus o args))

(defn max [& args] (apply java-time/max args))
(defn min [& args] (apply java-time/min args))
(defn before? [& args] (apply java-time/before? args))
(defn after? [& args] (apply java-time/after? args))

(defn duration
  [{:keys [millis seconds minutes hours days weeks months years]}]
  (cond->
    (java-time/duration)
    millis (java-time/plus (java-time/millis millis))
    seconds (java-time/plus (java-time/seconds seconds))
    minutes (java-time/plus (java-time/minutes minutes))
    hours (java-time/plus (java-time/hours hours))
    days (java-time/plus (java-time/days days))
    weeks (java-time/plus (java-time/weeks weeks))
    months (java-time/plus (java-time/months months))
    years (java-time/plus (java-time/years years))))

(defn millis [d] (duration {:millis d}))
(defn seconds [d] (duration {:seconds d}))
(defn minutes [d] (duration {:minutes d}))
(defn hours [d] (duration {:hours d}))
(defn days [d] (duration {:days d}))
(defn weeks [d] (duration {:weeks d}))
(defn months [d] (duration {:months d}))
(defn year [d] (duration {:years d}))

(defn date [& args] (apply java-time/local-date args))

(comment
  (java-time/zoned-date-time (java-time/local-date-time "2015-03-08T00:00") "America/Los_Angeles")
  (java-time/zoned-date-time (java-time/local-date "2019-03-20") (java-time/local-time "00:00:00") "America/Los_Angeles")
  (-> (now)
      (plus (java-time/seconds 1440))
      (->inst))
  (now)
  (-> (java-time/duration)
      (java-time/plus (java-time/seconds nil)))
  (java-time/local-date-time)
  (java-time/zoned-date-time "yyyy-MM-dd HH:mm:ss Z" "2019-03-01 00:00:00 +0000")
  (java-time/sql-timestamp (java-time/zoned-date-time #inst "2019-03-01T00:00:00-08:00" "America/Los_Angeles")))
