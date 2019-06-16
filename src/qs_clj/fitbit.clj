(ns qs-clj.fitbit
  (:require [clojure.string :as str]
            [clj-http.client :as client]
            [qs-clj.oauth :as oauth]
            [qs-clj.common :as common]))

(def ^:const base-url "https://api.fitbit.com")
(def ^:const client-id "22DFW9")
(def ^:const all-scopes #{:activity
                          :heartrate
                          :location
                          :nutrition
                          :profile
                          :settings
                          :sleep
                          :social
                          :weight})

(defmethod oauth/get-authorize-url :fitbit
  [_ _ {:keys [scopes redirect-uri]}]
  (let [scopes (->> (or scopes all-scopes)
                    (map name)
                    (str/join " "))]
    (format "%s/oauth2/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&expires_in=604800"
            "https://www.fitbit.com"                        ;; fuck fitbit for real
            client-id
            redirect-uri
            scopes)))

(defmethod oauth/exchange-token! :fitbit
  [_ system {:keys [grant-type authorization-code refresh-token redirect-uri]}]
  (let [client-secret (:fitbit-client-secret system)
        params (case grant-type
                 :authorization-code  {:code authorization-code
                                       :grant_type "authorization_code"}
                 :refresh-token       {:refresh_token refresh-token
                                       :grant_type "refresh_token"})]
    (-> (client/post
          (format "%s/oauth2/token" base-url)
          {:basic-auth  [client-id client-secret]
           :form-params (merge {:client_id    client-id
                                :redirect_uri redirect-uri}
                               params)
           :accept      :json
           :as          :json})
        :body)))

(comment
  (let [redirect-uri "http://localhost:8080/oauth/fitbit/callback"
        authorize-url (oauth/get-authorize-url :fitbit {} {:redirect-uri redirect-uri})]
    authorize-url)
  (oauth/exchange-token! :fitbit {:fitbit-client-secret "test"} {:authorization-code "b825fa27b02492590cdda8107d89a0540cd21380"
                                                                 :redirect-uri "http://localhost:8080/oauth/fitbit/callback"}))