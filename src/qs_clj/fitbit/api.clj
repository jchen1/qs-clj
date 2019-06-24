(ns qs-clj.fitbit.api
  (:require [clj-http.client :as http]
            [clojure.set :as set]
            [qs-clj.common :as common]
            [qs-clj.resource :as resource]))

(def ^:const api-base-url "https://api.fitbit.com")

(defn get-oauth-token
  [{:keys [client-id client-secret]} params]
  (->> (http/post
         (format "%s/oauth2/token" api-base-url)
         {:basic-auth  [client-id client-secret]
          :form-params (common/map-keys common/kabob->snake params)
          :accept :json
          :as :json})
       :body
       (common/map-keys common/snake->kabob)))

(defn authorized-request
  [url {:keys [access-token] :as token}]
  (let [full-url (format "%s/%s" api-base-url url)
        cached? (resource/exists? full-url)
        response (if cached?
                   (resource/resource full-url)
                   (http/get (format "%s/%s" api-base-url url)
                             {:headers              {"Authorization" (format "Bearer %s" access-token)}
                              :unexceptional-status (set/union http/unexceptional-status? #{429})
                              :as                   :json}))]
    (case (:status response)
      429 (throw (ex-info "Rate limited" {:retry-after (-> response :headers (get "retry-after") (Integer.))
                                          :response    response}))
      (do
        (when-not cached? (resource/save full-url (select-keys response [:status :headers :body])))
        (->>
          response
          :body
          (common/deep-map-keys common/camel->kebab))))))

