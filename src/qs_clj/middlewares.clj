(ns qs-clj.middlewares
  (:require [qs-clj.common :as common]))

(defn wrap-params
  [handler]
  (fn [{:keys [query-params params] :as request}]
    (-> request
        (update :query-params (fn [m] (common/map-keys #(if (string? %) (keyword %) %) m)))
        (update :params (fn [m] (common/map-keys #(if (string? %) (keyword %) %) m)))
        handler)))

(defn wrap-env
  [handler env]
  (fn [request]
    (handler (merge request env))))

(defn wrap-system
  [handler system]
  (fn [request]
    (handler (merge request system))))
