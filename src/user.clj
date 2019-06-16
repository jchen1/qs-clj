(ns user
  (:require [com.stuartsierra.component.user-helpers :refer [dev go set-dev-ns]]
            [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [qs-clj.server :as server]))

(set-dev-ns 'user)

(defn new-system [_]
  (component/system-map
    :webserver (server/->webserver-system env)))

(set-init new-system)
