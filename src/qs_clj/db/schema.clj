(ns qs-clj.db.schema
  (:require [qs-clj.db.enums :as enums]
            [qs-clj.db.fns :refer [db-fns]]))

(def ^:const user-schema
  [{:db/ident :user/id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "User ID"}
   {:db/ident :user/email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "User email"}
   {:db/ident :user/oauths
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "OAuth data"}])

(def ^:const oauth-schema
  [{:db/ident :oauth/active-token
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Active (maybe expired!) OAuth access token"}
   {:db/ident :oauth/active-token-expiration
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the active token expires"}
   {:db/ident :oauth/refresh-token
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "OAuth refresh token"}
   {:db/ident :oauth/provider
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The token's provider"}
   {:db/ident :oauth/user-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The provider user id associated with the token"}
   {:db/ident :oauth/scopes
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "Scopes associated with the token"}])

(def ^:const quantity-measurement-schema
  [{:db/ident :quantity-measurement/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "User the measurement belongs to"}
   {:db/ident :quantity-measurement/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Type of the measurement"}
   {:db/ident :quantity-measurement/provider
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Which provider sent the data"}
   {:db/ident :quantity-measurement/value
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/doc "Measurement value"}
   {:db/ident :quantity-measurement/start
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the measurement started"}
   {:db/ident :quantity-measurement/end
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the measurement ended"}
   {:db/ident :quantity-measurement/key
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one
    :db/doc "Dedup key for measurements"}])

(def ^:const category-measurement-schema
  [{:db/ident :category-measurement/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "User the measurement belongs to"}
   {:db/ident :category-measurement/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Type of the measurement"}
   {:db/ident :category-measurement/provider
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Which provider sent the data"}
   {:db/ident :category-measurement/value
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Measurement value - must be an enum value"}
   {:db/ident :category-measurement/start
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the measurement started"}
   {:db/ident :category-measurement/end
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the measurement ended"}
   {:db/ident :category-measurement/key
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one
    :db/doc "Dedup key for measurements"}])

(def ingest-queue-schema
  [{:db/ident :ingest-queue/provider
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Data to be ingested for a provider"}
   {:db/ident :ingest-queue/queue
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true
    :db/doc "The queue of items to ingest"}
   {:db/ident :ingest-queue-item/date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Inst of the api call to make"}
   {:db/ident :ingest-queue-item/key
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Unique key for a queue item"}])

(def ^:const schema
  (into [] (concat user-schema
                   oauth-schema
                   quantity-measurement-schema
                   category-measurement-schema
                   ingest-queue-schema
                   enums/all-enums
                   db-fns)))
