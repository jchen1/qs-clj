(ns qs-clj.middlewares
  (:require [environ.core :refer [env]]
            [qs-clj.common :as common]))

(defn wrap-params
  [handler]
  (fn [{:keys [query-params params] :as request}]
    (-> request
        (update :query-params (fn [m] (common/map-keys m #(if (string? %) (keyword %) %))))
        (update :params (fn [m] (common/map-keys m #(if (string? %) (keyword %) %))))
        handler)))

(defn wrap-env
  [handler]
  (fn [request]
    (handler (merge request env))))

(defn wrap-system
  [handler system]
  (fn [request]
    (handler (merge request system))))