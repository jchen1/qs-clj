(ns qs-clj.resource
  (:require [hasch.core :as hasch]
            [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io]))

(def ^:const resource-folder "resources")

(defn exists?
  [id]
  (let [filename (format "%s/%s.edn" resource-folder (hasch/uuid id))]
    (-> filename io/file .exists)))

(defn save
  ([id m] (save id m {:overwrite? false}))
  ([id m {:keys [overwrite?]}]
   (when (and (exists? id) overwrite?)
     (throw (ex-info "File exists! Pass :overwrite? true if you don't care..." {:id id})))
   (let [filename (format "%s/%s.edn" resource-folder (hasch/uuid id))]
     (spit filename m))))

(defn resource
  [id]
  (let [filename (format "%s/%s.edn" resource-folder (hasch/uuid id))]
    (try
      (-> filename slurp edn/read-string)
      (catch Throwable t
        (when-not (-> t
                      Throwable->map
                      (string/includes? "No such file or directory"))
          (throw t))))))

(comment
  (save "test" {:a :b})
  (-> "resources/0de92508-b44a-5a15-8cf8-f5efbe604b07.edn" slurp edn/read-string)
  (resource "test"))
