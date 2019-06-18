(ns qs-clj.oauth
  (:require [datomic.api :as d]
            [clojure.string :as string]
            [java-time :as time]))

(defmulti get-authorize-url (fn [provider system {:keys [scopes redirect-uri]}] provider))
(defmulti exchange-token! (fn [provider system {:keys [grant-type authorization-code refresh-token]}] provider))

(defn- provider->redirect-uri
  [{:keys [base-url]} provider]
  (format "%s/oauth/%s/callback" base-url (name provider)))

(defn authorize
  [{:keys [query-params] :as request}]
  (if-let [provider (some->> query-params :provider (keyword "provider"))]
    (let [redirect-uri (provider->redirect-uri request provider)
          url (get-authorize-url provider request {:redirect-uri redirect-uri})]
      {:status  301
       :headers {"Location" url}})
    {:status 400
     :body "`provider` is required."}))

(comment
  (time/plus (time/instant) (time/seconds "12345")))

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

(defn callback
  [{:keys [route-params query-params admin-user-email db] :as request}]
  (let [provider (some->> route-params :provider (keyword "provider"))
        code (some-> query-params :code)]
    (if (and provider code)
      (let [redirect-uri (provider->redirect-uri request provider)
            tokens (exchange-token! provider request {:grant-type :authorization-code
                                                      :authorization-code code
                                                      :redirect-uri redirect-uri})
            tx (upsert-token-tx (d/entity (d/db (:connection db)) [:user/email admin-user-email])
                                provider
                                tokens)
            {:keys [db-after]} @(d/transact (:connection db) tx)]
        {:status 200
         :body   (format "Hello, %s!"
                         (->> (d/entity db-after [:user/email admin-user-email])
                              :user/oauths
                              (map #(into {} %))
                              vec))})
      {:status 400
       :body   "`provider` is required."})))

(comment
  (authorize {:query-params {:provider :fitbit}}))
