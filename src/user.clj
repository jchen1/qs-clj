(ns user
  (:require [com.stuartsierra.component :as component]
            [qs-clj.server :as server]))

(def webserver-system (atom nil))

(defn stop
  []
  (when (some? @webserver-system)
    (prn "Stopping system...")
    (component/stop-system @webserver-system)
    (reset! webserver-system nil)))

(defn start
  []
  (when (some? @webserver-system)
    (stop))
  (prn "Starting system...")
  (let [system (component/start-system (server/->webserver-system))]
    (reset! webserver-system system)))