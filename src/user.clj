(ns user
  (:require [com.stuartsierra.component.user-helpers :refer [dev go set-dev-ns]]
            [com.stuartsierra.component.repl :refer [reset set-init start stop system]]
            [environ.core :refer [env]]))

(set-dev-ns 'user)

(defn ns-call
  [sym & args]
  (apply (requiring-resolve sym) args))

(defn new-system [_]
  (ns-call 'com.stuartsierra.component/system-map
    :webserver (ns-call 'qs-clj.server/->webserver-system env)))

(set-init new-system)
