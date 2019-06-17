(ns qs-clj.common
  (:require [clojure.string :as string])
  (:import [java.util Base64]))

(defn map-keys
  [map-fn m]
  (->> m
       (map (fn [[k v]] [(map-fn k) v]))
       (into {})))

(defn map-vals
  [map-fn m]
  (->> m
       (map (fn [[k v]] [k (map-fn v)]))
       (into {})))

(defn encode-base64-str
  [s]
  (-> (Base64/getEncoder)
      (.encodeToString (.getBytes s))))

(defn camel-case-to-kabob
  [s]
  (cond
    (string? s) (string/replace s "_" "-")
    (keyword? s) (keyword (-> s namespace camel-case-to-kabob)
                          (-> s name camel-case-to-kabob))))

(defn kabob-to-camel-case
  [s]
  (cond
    (string? s) (string/replace s "-" "_")
    (keyword? s) (keyword (-> s namespace kabob-to-camel-case)
                          (-> s name kabob-to-camel-case))))
