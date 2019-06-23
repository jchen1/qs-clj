(ns qs-clj.fitbit.api
  (:require [clj-http.client :as http]
            [qs-clj.common :as common]
            [clojure.set :as set]))

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

;; todo: handle rate limits
(defn authorized-request
  [url {:keys [access-token] :as token}]
  (let [response (http/get (format "%s/%s" api-base-url url)
                           {:headers              {"Authorization" (format "Bearer %s" access-token)}
                            :unexceptional-status (set/union http/unexceptional-status? #{429})
                            :as                   :json})]
    (case (:status response)
      429 (throw (ex-info "Rate limited" {:retry-after (->> response :headers "retry-after" (Integer.))
                                          :response response}))
      (->>
        response
        :body
        (common/deep-map-keys common/camel->kebab)))))

