(ns qs-clj.oauth
  (:require [datomic.api :as d]
            [clojure.string :as string]
            [java-time :as time]
            [ring.util.codec :refer [url-encode url-decode]]))

(defmulti get-authorize-url (fn [provider system {:keys [scopes callback-uri redirect-uri]}] provider))
(defmulti exchange-token* (fn [provider system {:keys [grant-type authorization-code refresh-token]}] provider))

(defn- provider->callback-uri
  [{:keys [base-url]} provider]
  (format "%s/oauth/%s/callback" base-url (name provider)))

(defn- upsert-token-tx
  [user provider {:keys [access-token expires-in refresh-token user-id scope]}]
  (let [new-oauth {:oauth/active-token            access-token
                   :oauth/active-token-expiration (-> (time/instant)
                                                      (time/plus (-> expires-in Integer. time/seconds))
                                                      (time/java-date))
                   :oauth/refresh-token           refresh-token
                   :oauth/user-id                 user-id
                   :oauth/provider                provider
                   :oauth/scopes                  (sort (string/split scope #" "))}]
    (->>
      [(when-let [maybe-existing-oauth (->> user
                                            :user/oauths
                                            (filter #(= (:oauth/provider %) provider))
                                            first
                                            :db/id)]
         [:db/retract (:db/id user) :user/oauths maybe-existing-oauth])
       {:user/email (:user/email user)
        :user/oauths [new-oauth]}]
      (keep identity))))

(defn exchange-token!
  [{:keys [connection admin-user] :as system} provider opts]
  (let [tokens (exchange-token* provider system opts)
        tx (upsert-token-tx admin-user provider tokens)]
    @(d/transact connection tx)))

(defn authorize
  [{:keys [query-params] :as request}]
  (if-let [provider (some->> query-params :provider (keyword "provider"))]
    (let [callback-uri (provider->callback-uri request provider)
          redirect-uri (some->> query-params :redirect-uri)
          url (get-authorize-url provider request {:callback-uri (url-encode callback-uri)
                                                   :redirect-uri (url-encode redirect-uri)})]
      {:status  302
       :headers {"Location" url}})
    {:status 400
     :body "`provider` is required."}))

(defn callback
  [{:keys [route-params query-params admin-user] :as request}]
  (let [provider (some->> route-params :provider (keyword "provider"))
        code (some-> query-params :code)
        redirect-uri (some-> query-params :state url-decode)]
    (if (and provider code)
      (let [callback-uri (provider->callback-uri request provider)
            params {:grant-type :authorization-code
                    :authorization-code code
                    :callback-uri callback-uri}
            {:keys [db-after]} (exchange-token! request provider params)]
        (if redirect-uri
          {:status  302
           :headers {"Location" redirect-uri}}
          {:status 200
           :body   (format "Hello, %s!"
                           (->> (d/entity db-after (:db/id admin-user))
                                :user/oauths
                                (map #(into {} %))
                                vec))}))
      {:status 400
       :body   "`provider` is required."})))

(defn token-for-provider
  [{:keys [admin-user] :as system} provider]
  (when-let [active-token
        (when-let [token (->> admin-user
                              :user/oauths
                              (filter #(= (:oauth/provider %) provider))
                              first)]
          (if (time/after? (time/instant) (time/instant (:oauth/active-token-expiration token)))
            (let [{:keys [db-after]} (exchange-token! system provider {:grant-type    :refresh-token
                                                                       :refresh-token (:oauth/refresh-token token)
                                                                       :callback-uri  (provider->callback-uri system provider)})
                  user (d/entity db-after (:db/id admin-user))]
              (->> user
                   :user/oauths
                   (filter #(= (:oauth/provider %) provider))
                   first))
            token))]
    {:user-id (:oauth/user-id active-token)
     :access-token (:oauth/active-token active-token)}))

(comment
  (authorize {:query-params {:provider :fitbit}}))
