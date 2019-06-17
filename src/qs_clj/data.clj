(ns qs-clj.data)

(defmulti data-for-day (fn [provider system day opts] provider))
