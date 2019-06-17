(ns qs-clj.fitbit
  (:require [clojure.string :as str]
            [clj-http.client :as http]
            [qs-clj.common :as common]
            [qs-clj.data :as data]
            [qs-clj.oauth :as oauth]))

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
                 :authorization-code  {:code authorization-code}
                 :refresh-token       {:refresh_token refresh-token})]
    (->> (http/post
           (format "%s/oauth2/token" base-url)
           {:basic-auth  [client-id client-secret]
            :form-params (merge {:client_id    client-id
                                 :redirect_uri redirect-uri
                                 :grant_type (-> grant-type name common/kabob-to-camel-case)}
                                params)
            :accept      :json
            :as          :json})
         :body
         (common/map-keys common/camel-case-to-kabob))))

(def ^:const log-types
  (merge
    {:fat       {:api-version 1
                 :fragment    "body/log/fat"}
     :weight    {:api-version 1
                 :fragment    "body/log/weight"}
     :sleep     {:api-version 1.2
                 :fragment    "sleep"}
     :activity  {:api-version 1
                 :fragment    "activities"}}
    (->> [:heart :calories :caloriesBMR :steps :distance :floors :elevation :minutesSedentary :minutesLightlyActive :minutesFairlyActive :minutesVeryActive :activityCalories]
         (map (fn [a] [a {:api-version 1
                          :fragment    (format "activities/%s" (name a))
                          :period      "1d"}]))
         (into {}))))

(defn api-call
  [client log-type date]
  (let [{:keys [api-version fragment period]} (log-types log-type)
        date (if period
               (format "%s/%s" date period)
               date)
        url (format "%s/%s/user/%s/%s/date/%s.json"
                    base-url
                    api-version
                    (:user-id client)
                    fragment
                    date)]
    (->> (http/get
           url
           {:headers {"Authorization" (format "Bearer %s" (:access-token client))}
            :as :json})
         :body
         (common/map-keys common/camel-case-to-kabob))))

(defn new-client
  [user-id access-token refresh-token]
  {:access-token access-token
   :refresh-token refresh-token
   :user-id user-id})

(defmethod data/data-for-day :fitbit
  [_ {:keys [fitbit] :as system} day opts]
  (->> (keys log-types)
       (map #(api-call fitbit % day))
       (doall)))

(comment
  (let [redirect-uri "http://localhost:8080/oauth/fitbit/callback"
        authorize-url (oauth/get-authorize-url :fitbit {} {:redirect-uri redirect-uri})]
    authorize-url))
