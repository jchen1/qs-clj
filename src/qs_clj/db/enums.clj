(ns qs-clj.db.enums)

(defn- make-ident
  [k]
  {:pre [(keyword? k)]}
  {:db/ident k})

(def ^:const quantity->unit
  #:quantity{:calories       :unit/scalar,
             :distance       :unit/feet,
             :elevation      :unit/feet,
             :fat            :unit/scalar,
             :floors-climbed :unit/scalar,
             :heart-rate     :unit/scalar,
             :steps          :unit/scalar,
             :weight         :unit/pounds})

(def ^:const quantities
  (->> quantity->unit keys set (map make-ident)))

(def ^:const units
  (->> quantity->unit vals set (map make-ident)))

;; enum measurements & their possible values
(def ^:const category->categories
  {:category/sleep #{:sleep/deep :sleep/light :sleep/rem :sleep/wake}})

(def ^:const categories
  (->> category->categories keys set (map make-ident)))

(def ^:const category-vals
  (->> category->categories vals (mapcat #(map make-ident %))))

(def ^:const providers
  #{:provider/fitbit})

(def ^:const all-enums
  (into [] (concat quantities
                   units
                   categories
                   category-vals
                   (->> providers (map make-ident)))))
