(ns mattpat.roomy.web.js
    (:require
      [clojure.string :as string]))

(declare write-js)

(defn- write-pair [[k v]]
  (format "%s: %s" (name k) (write-js v)))
(defn- write-map [m]
  (->> m
       (map write-pair)
       (string/join ", ")
       (format "{%s}")))

(defn- write-coll [s]
  (->> s
       (map write-js)
       (string/join ", ")
       (format "[%s]")))

(defn write-js [m]
  (cond
   (map? m) (write-map m)
   (coll? m) (write-coll m)
   :else m))
