(ns qs-clj.data
  (:require [qs-clj.oauth :as oauth]
            [ring.util.codec :refer [url-encode]]
            [ring.util.request :refer [request-url]]))

(defmulti data-for-day* (fn [provider {:keys [admin-user] :as system} {:keys [user-id access-token] :as token} day opts] provider))

(defn data-for-day
  [{:keys [admin-user query-params] :as system}]
  (let [provider (some->> query-params :provider (keyword "provider"))
        ;; todo check this is yyyy-mm-dd
        day (some->> query-params :day)]
    (if (and provider day)
      (if-let [tokens (oauth/token-for-provider system provider)]
        (let [result (data-for-day* provider system tokens day {})]
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
