(ns qs-clj.fitbit.core
  (:require [clojure.string :as str]
            [clj-http.client :as http]
            [qs-clj.common :as common]
            [qs-clj.data :as data]
            [qs-clj.fitbit.transforms :as transforms]
            [qs-clj.oauth :as oauth]
            [java-time :as time]))

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

(defmethod oauth/get-authorize-url :provider/fitbit
  [_ _ {:keys [scopes callback-uri redirect-uri]}]
  (let [scopes (->> (or scopes all-scopes)
                    (map name)
                    (str/join " "))]
    (format "%s/oauth2/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&expires_in=604800&state=%s"
            "https://www.fitbit.com"                        ;; fuck fitbit for real
            client-id
            callback-uri
            scopes
            redirect-uri)))

(defmethod oauth/exchange-token* :provider/fitbit
  [_ {:keys [fitbit]} {:keys [grant-type authorization-code refresh-token callback-uri]}]
  (let [params (case grant-type
                 :authorization-code  {:code authorization-code}
                 :refresh-token       {:refresh_token refresh-token})]
    (->> (http/post
           (format "%s/oauth2/token" base-url)
           {:basic-auth  [client-id (:client-secret fitbit)]
            :form-params (merge {:client_id    client-id
                                 :redirect_uri callback-uri
                                 :grant_type   (-> grant-type name common/kabob->snake)}
                                params)
            :accept      :json
            :as          :json})
         :body
         (common/map-keys common/snake->kabob))))

(defn authorized-get
  [url {:keys [access-token] :as token}]
  (http/get url {:headers {"Authorization" (format "Bearer %s" access-token)}
                 :as      :json}))

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
    ;; other available data: :minutesSedentary :minutesLightlyActive :minutesFairlyActive :minutesVeryActive :activityCalories
    (->> [:heart :calories :caloriesBMR :steps :distance :floors :elevation]
         (map (fn [a] [a {:api-version 1
                          :fragment    (format "activities/%s" (name a))
                          :period      "1d"}]))
         (into {}))))

(defn api-call
  [{:keys [user-id access-token] :as token} log-type date]
  (let [{:keys [api-version fragment period]} (log-types log-type)
        date (if period
               (format "%s/%s" date period)
               date)
        url (format "%s/%s/user/%s/%s/date/%s.json"
                    base-url
                    api-version
                    user-id
                    fragment
                    date)]
    (->> (authorized-get url token)
         :body
         (common/deep-map-keys common/camel->kebab))))

(defn new-client
  [{:keys [fitbit-client-secret]}]
  {:client-secret fitbit-client-secret})

(defmethod data/data-for-day* :provider/fitbit
  [_ system token day opts]
  (->> (keys log-types)
       (filter #(contains? (->> (methods transforms/transform) keys set) %))
       (pmap #(do [% (api-call token % day)]))
       (mapcat (fn [[type data]] (transforms/transform type system data)))
       (doall)))

(defmethod data/first-day-with-data* :provider/fitbit
  [_ system {:keys [user-id] :as token}]
  (let [url (format "%s/1/user/%s/badges.json" base-url user-id)]
    (->> (authorized-get url token)
         :body
         (common/deep-map-keys common/camel->kebab)
         :badges
         (map :date-time)
         (map time/local-date)
         (apply time/min)
         (time/sql-date))))

(comment
  (->> (methods transforms/transform) keys)
  (let [redirect-uri "http://localhost:8080/oauth/fitbit/callback"
        authorize-url (oauth/get-authorize-url :fitbit {} {:redirect-uri redirect-uri})]
    authorize-url))
