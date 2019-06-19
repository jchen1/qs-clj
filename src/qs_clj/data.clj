(ns qs-clj.data
  (:require [cheshire.core :as json]
            [qs-clj.oauth :as oauth]))

(defmulti data-for-day* (fn [provider {:keys [user-id access-token] :as token} day opts] provider))

(defn data-for-day
  [{:keys [admin-user query-params] :as system}]
  (let [provider (some->> query-params :provider (keyword "provider"))
        ;; todo check this is yyyy-mm-dd
        day (some->> query-params :day)]
    (if (and provider day)
      (if-let [tokens (oauth/token-for-provider system provider)]
        (let [result (into {} (data-for-day* provider tokens day {}))]
          {:status 200
           :body   result})
        ;; todo save the expected url
        {:status  302
         :headers {"Location" (format "/oauth/authorize?provider=%s" (name provider))}})
      {:status 400
       :body   "`provider` and `day` are required."})))
