(ns qs-clj.fitbit.core
  (:require [clojure.string :as str]
            [clj-http.client :as http]
            [qs-clj.common :as common]
            [qs-clj.data :as data]
            [qs-clj.fitbit.api :as api]
            [qs-clj.fitbit.transforms :as transforms]
            [qs-clj.oauth :as oauth]
            [java-time :as time]))

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
  (let [params (merge (case grant-type
                        :authorization-code {:code authorization-code}
                        :refresh-token {:refresh-token refresh-token})
                      {:client-id    client-id
                       :redirect-uri callback-uri
                       :grant-type   (-> grant-type name common/kabob->snake)})]
    (api/get-oauth-token fitbit params)))

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
        url (format "%s/user/%s/%s/date/%s.json"
                    api-version
                    user-id
                    fragment
                    date)]
    (api/authorized-request url token)))

(defn new-client
  [{:keys [fitbit-client-secret]}]
  {:client-id client-id
   :client-secret fitbit-client-secret})

(defmethod data/data-for-day* :provider/fitbit
  [_ system token day opts]
  (->> (keys log-types)
       (filter #(contains? (->> (methods transforms/transform) keys set) %))
       (pmap #(do [% (api-call token % day)]))
       (mapcat (fn [[type data]] (transforms/transform type system data)))
       (doall)))

(defmethod data/first-day-with-data* :provider/fitbit
  [_ system {:keys [user-id] :as token}]
  (let [url (format "1/user/%s/badges.json" user-id)]
    (->> (api/authorized-request url token)
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
