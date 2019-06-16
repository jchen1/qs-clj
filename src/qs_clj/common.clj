(ns qs-clj.common
  (:import [java.util Base64]))

(defn map-keys
  [m map-fn]
  (->> m
       (map (fn [[k v]] [(map-fn k) v]))
       (into {})))

(defn map-vals
  [m map-fn]
  (->> m
       (map (fn [[k v]] [k (map-fn v)]))
       (into {})))

(defn encode-base64-str
  [s]
  (-> (Base64/getEncoder)
      (.encodeToString (.getBytes s))))