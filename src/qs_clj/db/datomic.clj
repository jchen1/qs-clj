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
        conn (d/connect uri)
        tx [{:user/email       "hello@jeff.yt"
             :user/fitbit-auth {:oauth/active-token            "asdf"
                                ;; todo do a plus-time
                                :oauth/active-token-expiration (java.util.Date.)
                                :oauth/refresh-token           "asdf2"
                                ;; todo unify all provider namespacing
                                :oauth/provider                (keyword "provider" (name :fitbit))
                                :oauth/scopes                  (clojure.string/split "fat weight" #" ")}}]
        db-after @(d/transact conn tx)]
    (into {} (:user/fitbit-auth (d/entity (:db-after @(d/transact conn tx)) [:user/email "hello@jeff.yt"])))))
