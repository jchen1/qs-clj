(ns qs-clj.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [qs-clj.schema :refer [schema]]))

(defn- create-db
  [uri]
  (d/create-database uri)
  @(d/transact (d/connect uri) schema))

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
  (create-db "datomic:mem://localhost:4334/dev"))
