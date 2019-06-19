(ns qs-clj.middlewares
  (:require [qs-clj.common :as common]
            [datomic.api :as d]))

(defn wrap-params
  [handler]
  (fn [{:keys [query-params params] :as request}]
    (-> request
        (update :query-params (fn [m] (common/map-keys #(if (string? %) (keyword %) %) m)))
        (update :params (fn [m] (common/map-keys #(if (string? %) (keyword %) %) m)))
        handler)))

;; depends on wrap-env and wrap-system
(defn wrap-admin-user
  [handler]
  (fn [{:keys [admin-user-email db] :as request}]
    (handler (assoc request :admin-user (d/entity db [:user/email admin-user-email])))))

;; depends on wrap-system
(defn wrap-db
  [handler]
  (fn [{:keys [db-wrapper] :as request}]
    (handler (assoc request :db (d/db (:connection db-wrapper)) :connection (:connection db-wrapper)))))

(defn wrap-env
  [handler env]
  (fn [request]
    (handler (merge request env))))

(defn wrap-system
  [handler system]
  (fn [request]
    (handler (merge request system))))
