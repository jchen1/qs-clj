(ns qs-clj.data
  (:require [datomic.api :as d]
            [hasch.core :as hasch]
            [java-time :as time]
            [qs-clj.oauth :as oauth]
            [ring.util.codec :refer [url-encode]]
            [ring.util.request :refer [request-url]]))

(defmulti data-for-day* (fn [provider {:keys [admin-user] :as system} {:keys [user-id access-token] :as token} day opts] provider))
(defmulti first-day-with-data* (fn [provider system token] provider))

(defn data-for-day-tx
  [provider {:keys [admin-user] :as system} tokens date opts]
  (let [date (time/sql-date (time/local-date date))]
    [{:db/id              (:db/id admin-user)
      :user/provider-data [{:data-date/id   (hasch/uuid [(:db/id admin-user) date])
                            :data-date/date date
                            :data-date/data (data-for-day* provider system tokens date opts)}]}]))

(defn load-data-for-day
  [{:keys [admin-user connection query-params] :as system}]
  (let [provider (some->> query-params :provider (keyword "provider"))
        debug? (some->> query-params :debug)
        ;; todo check this is yyyy-mm-dd
        day (some->> query-params :day)]
    (if (and provider day)
      (if-let [tokens (oauth/token-for-provider system provider)]
        (let [result (data-for-day-tx provider system tokens day {})]
          (when-not debug? @(d/transact connection result))
          {:status 200
           :body   result})
        {:status  302
         :headers {"Location" (format "/oauth/authorize?provider=%s&redirect-uri=%s"
                                      (name provider)
                                      (-> (request-url system)
                                          url-encode))}})
      {:status 400
       :body   "`provider` and `day` are required."})))

(comment
  (remove-ns 'qs-clj.data))
