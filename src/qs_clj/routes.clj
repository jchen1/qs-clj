(ns qs-clj.routes
  (:require [bidi.ring :refer [make-handler]]
            [qs-clj.oauth :as oauth]
            [qs-clj.data :as data]))

(defn index
  [request]
  {:status 200
   :body "Hello, world!"})

(defn not-found
  [_]
  {:status 404
   :body "Not found"})

(def routes ["/" {""       #'index
                  "oauth/" {[:provider "/callback"] #'oauth/callback
                            "authorize"             #'oauth/authorize}
                  "data/"  {"day" #'data/data-for-day}
                  true     #'not-found}])

(def handler
  (make-handler routes))

(comment
  (bidi.bidi/match-route routes "/test"))
