(ns qs-clj.schema)


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
   {:db/ident :user/fitbit-auth
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/isComponent true
    :db/doc "Fitbit auth data"}])

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
   {:db/ident :oauth/scopes
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "Scopes associated with the token"}])

(def ^:const providers
  [{:db/ident :provider/fitbit}])

(def ^:const schema
  (into [] (concat user-schema
                   oauth-schema
                   providers)))
