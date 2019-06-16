(ns qs-clj.oauth)

(defmulti get-authorize-url (fn [provider system {:keys [scopes redirect-uri]}] provider))
(defmulti exchange-token! (fn [provider system {:keys [grant-type authorization-code refresh-token]}] provider))

(defn- provider->redirect-uri
  [provider]
  (format "http://localhost:8080/oauth/%s/callback" (name provider)))

(defn authorize
  [{:keys [query-params] :as request}]
  (if-let [provider (some-> query-params :provider keyword)]
    (let [redirect-uri (provider->redirect-uri provider)
          url (get-authorize-url provider request {:redirect-uri redirect-uri})]
      {:status  301
       :headers {"Location" url}})
    {:status 400
     :body "`provider` is required."}))

(defn callback
  [{:keys [route-params query-params] :as request}]
  (let [provider (some-> route-params :provider keyword)
        code (some-> query-params :code)]
    (if (and provider code)
      (let [redirect-uri (provider->redirect-uri provider)
            tokens (exchange-token! provider request {:grant-type :authorization-code
                                                      :authorization-code code
                                                      :redirect-uri redirect-uri})]
        {:status 200
         :body   (format "Hello, %s %s %s!"
                         provider
                         code
                         tokens)})
      {:status 400
       :body   "`provider` is required."})))

(comment
  (authorize {:query-params {:provider :fitbit}}))