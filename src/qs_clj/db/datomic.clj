(ns qs-clj.db.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [qs-clj.db.schema :refer [schema]]))

(defn- create-db
  [uri]
  (d/create-database uri)
  @(d/transact (d/connect uri) schema))

(defn initial-data
  [env]
  (when-let [admin-email (:admin-user-email env)]
    [{:db/id      (d/tempid :db.part/user)
      :user/id    (d/squuid)
      :user/email admin-email}]))

(defrecord DatomicDatabase [uri initial-data]
  component/Lifecycle
  (start [this]
    (create-db uri)
    (let [c (d/connect uri)]
      (when initial-data
        @(d/transact c initial-data))
      (assoc this :connection c)))
  (stop [this]
    (assoc this :connection nil)))

(comment
  schema
  (let [uri "datomic:mem://localhost:4334/dev"
        _ (create-db uri)
        conn (d/connect uri)]
    (->> (d/entity (d/db conn) [:ingest-queue/provider "fitbit"])
         :ingest-queue/queue
         (into [])
         (sort-by :ingest-queue-item/date)
         (map #(into {} %))
         (take 5)))

  (require '[hasch.core])

  (let [uri "datomic:mem://localhost:4334/dev"
        _ (create-db uri)
        conn (d/connect uri)
        tx [{:ingest-queue/provider "fitbit"
             :ingest-queue/queue    [{:ingest-queue-item/date (java.util.Date.)
                                      :ingest-queue-item/key  (hasch.core/uuid)}]}]
        db-after @(d/transact conn tx)]
    (into {} (d/entity (:db-after db-after) [:ingest-queue/provider "fitbit"]))))
