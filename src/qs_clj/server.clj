(ns qs-clj.server
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :as ring-params]
            [ring.middleware.session :refer [wrap-session]]
            [qs-clj.db.datomic :as datomic]
            [qs-clj.middlewares :as middlewares]
            [qs-clj.routes :as routes]
            [taoensso.timbre :as timbre]))

(defrecord WebServer [web-port make-handler]
  component/Lifecycle
  (start [this]
    (let [handler (make-handler (:core-system this))
          web-port (or (-> web-port (Integer.)) 8080)
          server (run-jetty handler {:port web-port
                                     :join? false})]
      (assoc this :server server)))
  (stop [this]
    (some-> this :server .stop)
    (assoc this :server nil)))

(defn make-handler
  [system]
  (fn [request]
    ((-> #'routes/handler
         wrap-session
         middlewares/wrap-params
         ring-params/wrap-params
         (middlewares/wrap-system system)
         (middlewares/wrap-env env)) request)))

(defn ->webserver-system
  [env]
  (timbre/set-level! (-> (or (:log-level env) "warn") keyword))
  (let [core-system (component/system-map
                      :db (datomic/map->DatomicDatabase {:uri          (or (:datomic-uri env) "datomic:mem://localhost:4334/dev")
                                                         :initial-data (datomic/initial-data env)}))]
    (component/system-map
      :core-system core-system
      :webserver (component/using (map->WebServer {:make-handler make-handler
                                                   :web-port (:web-port env)})
                                  [:core-system]))))
