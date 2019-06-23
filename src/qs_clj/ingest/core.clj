(ns qs-clj.ingest.core
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [hasch.core :as hasch]
            [java-time :as time]
            [qs-clj.data :as data]
            [qs-clj.oauth :as oauth]))

;; where the magic happens - make api calls and shit

;; wait for oauth tokens
;; find first date
;; add all the shit to the queue

(defn- queue-for-provider
  [db provider]
  (d/entity db [:ingest-queue/provider provider]))

(defn- queue-key
  [provider time]
  (hasch/uuid [provider time]))

(defn- populate-queue-tx
  ([provider first-date-with-data] (populate-queue-tx provider first-date-with-data (time/local-date)))
  ([provider first-date-with-data last-date-with-data]
   {:pre [(time/before? first-date-with-data last-date-with-data)]}
   (let [dates (->> (time/days 1)
                    (time/iterate time/plus first-date-with-data)
                    (take-while #(time/before? % last-date-with-data))
                    (map time/sql-timestamp))]
     {:ingest-queue/provider provider
      :ingest-queue/queue    (->> dates
                                  (map (fn [date]
                                         {:ingest-queue-item/date date
                                          :ingest-queue-item/key  (queue-key provider date)})))})))

(defn- maybe-populate-queue
  [{:keys [provider db admin-user] :as system}]
  (when-let [tokens (oauth/token-for-provider system provider)]
    (when-not (queue-for-provider db provider)
      (let [first-date-with-data (data/first-day-with-data* system provider)]
        @(d/transact db (populate-queue-tx provider first-date-with-data))))))

(defn- process-queue-item
  [{:keys [provider connection db admin-user] :as system} {:ingest-queue-item/keys [date key] :as queue-item}]
  (when-let [tokens (oauth/token-for-provider system provider)]
    (let [tx (concat (data/data-for-day* provider system tokens date {})
                     [[:db/retract [:ingest-queue/provider provider] :ingest-queue/queue queue-item]])]
      @(d/transact connection tx))))

(defn- process-queue
  [{:keys [provider db] :as system}]
  (let [earliest-item-in-queue (->> (queue-for-provider db provider)
                                    :ingest-queue/queue
                                    (sort-by :ingest-queue-item/date)
                                    first)]
    (when earliest-item-in-queue
      (process-queue-item system earliest-item-in-queue))))

;; sleep for 1 min between ingests by default
(def ^:const default-sleep-time (* 1000 60))

(defrecord IngestQueue [provider]
  component/Lifecycle
  (start [{:keys [db-wrapper admin-user-email] :as this}]
    (let [running? (atom true)
          f (fn []
              (let [next-sleep-time (volatile! default-sleep-time)]
                (while (and @running?
                            (not (.isInterrupted (Thread/currentThread))))
                  (let [db (-> db-wrapper :connection d/db)
                        admin-user (d/entity db [:user/email admin-user-email])
                        system (assoc this :db db :admin-user admin-user :connection (:connection db-wrapper))]
                    (try
                      (maybe-populate-queue system)
                      (process-queue system)
                      (catch Throwable t
                        (let [data (ex-data t)]
                          (when-let [retry-after (:retry-after data)]
                            (vreset! next-sleep-time (* 1000 retry-after))))))
                    (try
                      (Thread/sleep @next-sleep-time)
                      (catch InterruptedException _
                        (reset! running? false)))))))
          thread (-> (Thread. f) .start)]
      (assoc this :thread thread :provider provider)))
  (stop [this]
    (some-> this :running? (reset! false))
    (some-> this :thread (.join))
    (assoc this :running? nil :thread nil)))

(comment
  (queue-key :provider/fitbit (time/sql-timestamp (time/local-date "2019-03-01")))
  (time/sql-timestamp (time/local-date))
  (->> (time/days 1)
       (time/iterate time/plus (time/local-date "2019-03-01"))
       (take-while #(time/before? % (time/local-date)))))
