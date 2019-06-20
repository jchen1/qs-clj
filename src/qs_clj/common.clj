(ns qs-clj.common
  (:require [clojure.walk :refer [postwalk]]
            [clojure.string :as string])
  (:import [java.util Base64]))

(defn map-keys
  [f m]
  (->> m
       (map (fn [[k v]] [(f k) v]))
       (into {})))

(defn deep-map-keys
  [f m]
  (postwalk
    (fn [m] (cond->> m (map? m) (map-keys f)))
    m))

(defn map-vals
  [f m]
  (->> m
       (map (fn [[k v]] [k (f v)]))
       (into {})))

(defn encode-base64-str
  [s]
  (-> (Base64/getEncoder)
      (.encodeToString (.getBytes s))))

(defn snake->kabob
  [s]
  (cond
    (string? s) (string/replace s "_" "-")
    (keyword? s) (keyword (-> s namespace snake->kabob)
                          (-> s name snake->kabob))))

(defn kabob->snake
  [s]
  (cond
    (string? s) (string/replace s "-" "_")
    (keyword? s) (keyword (-> s namespace kabob->snake)
                          (-> s name kabob->snake))))

;; todo handle capital first letter
(defn camel->kebab
  [s]
  (cond
    (string? s) (string/lower-case (string/replace s #"[A-Z0-9]+" (partial str "-")))
    (keyword? s) (keyword (-> s namespace camel->kebab)
                          (-> s name camel->kebab))))

(defn kebab->camel
  [s]
  (cond
    (string? s) (string/replace s #"-(\w)" (comp string/upper-case second))
    (keyword? s) (keyword (-> s namespace kebab->camel)
                          (-> s name kebab->camel))))

(comment
  (camel->kebab "caloriesBMR"))
