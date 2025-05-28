(ns mattpat.roomy.util
    (:require
      [clojure.data.json :as json]
      [clojure.java.io :as io]
      [clojure.string :as string]
      [mattpat.roomy.web.js :as js]
      [mattpat.roomy.env :refer [dev?]]))

(defn distinct-by [f s]
  (->
   (reduce #(assoc %1 (f %2) %2) {} s)
   (dissoc nil)
   vals))

(defn key-by [f s]
  (zipmap (map f s) s))

(defn unique-by [f s]
  (vals (key-by f s)))

(defn keyset-by [f s]
  (reduce
   (fn [m x]
     (update m (f x) conj x))
   {}
   s))

(defn keyset-by2 [f s]
  (reduce
   (fn [m x]
     (reduce
      (fn [m i]
        (update m i conj x))
      m
      (f x)))
   {}
   s))

(defn zipmap-by [f1 f2 s]
  (zipmap
   (map f1 s)
   (map f2 s)))

(defn- mk* [m [a b & rest]]
  (cond
   (keyword? a) (recur (assoc m a b) rest)
   a (recur (assoc m (keyword a) (symbol a)) (list* b rest))
   :else m))
(defmacro mk [& syms]
  (mk* {} syms))
(defmacro assoc-mk [m & syms]
  `(merge ~m ~(mk* {} syms)))

(defmacro defm [sym & rest]
  `(def ~sym (memoize (fn ~@rest))))

(defmacro defm-dev [sym & rest]
  `(def ~sym ((if dev? identity memoize) (fn ~@rest))))

(defn slurp-resource [resource]
  (-> resource io/resource slurp))

(defn read-str [s]
  (json/read-str s {:key-fn keyword}))

(def write-str json/write-str)

(defn slurp-json [resource]
  (read-str
   (slurp-resource resource)))

(defn slurp-edn [resource]
  (read-string
   (slurp-resource resource)))

(defn transform-in [m1 & transforms]
  (reduce
   (fn [m2 transform]
     (->> (subvec transform 1)
          (get-in m1)
          (assoc m2 (transform 0))))
   {}
   transforms))

(defn rename [m & transforms]
  (->> transforms
       (partition 2)
       (reduce
        (fn [m [k1 k2]]
          (-> m (assoc k2 (m k1)) (dissoc k1)))
        m)))

(defn assoc-f [m k f]
  (assoc m k (f m)))

(defn map-vals [f m]
  (->> m vals (map f) (zipmap (keys m))))

(defn collmap [& args]
  (into {}
        (for [[ks v] (partition 2 args)
              k ks]
          [k v])))

(defn invert [m]
  (zipmap (vals m) (keys m)))

(defn group-by-map [f1 f2 s]
  (->> s (filter f1) (group-by f1) (map-vals f2)))

(defn map-first-last [f s]
  (let [c (-> s count dec)]
    (map-indexed
     (fn [i x]
       (f x (zero? i) (= c i)))
     s)))

(defn dissoc-in [m [k & ks]]
  (if ks
    (let [v (dissoc-in (get m k) ks)]
      (if (empty? v)
        (dissoc m k)
        (assoc m k v)))
    (dissoc m k)))

(defn conj-in [m [k & ks]]
  (if ks
    (assoc m k (conj-in (get m k) ks))
    (conj (or m #{}) k)))

(defn toggle-set [s v]
  (cond
   (nil? s) #{v}
   (contains? s v) (disj s v)
   :else (conj s v)))

(defn max-by [f [x & rest]]
  (when x
        (first
         (reduce
          (fn [[x y] xd]
            (let [yd (f xd)]
              (if (pos? (compare yd y))
                [xd yd]
                [x y])))
          [x (f x)]
          rest))))

(defn min-by [f [x & rest]]
  (when x
        (first
         (reduce
          (fn [[x y] xd]
            (let [yd (f xd)]
              (if (neg? (compare yd y))
                [xd yd]
                [x y])))
          [x (f x)]
          rest))))

(defmacro format-js$ [s]
  `(-> ~s
    ~@(for [[to-replace replacement] (re-seq #"\$\{([^\}]+)}" s)]
       `(string/replace-first ~to-replace ~(read-string replacement)))))

(defn format-json [fmt & args]
  (->> args
       (map json/write-str)
       (apply format fmt)))

(defn format-js [fmt & args]
  (->> args
       (map js/write-js)
       (apply format fmt)))

(defn mod12 [x]
  (-> x dec (mod 12) inc))
(defn mod12-suffix [x]
  (str (mod12 x)
       (if (< x 12) " AM" " PM")))
(defn mod12-suffix-short [x]
  (str (mod12 x)
       (if (< x 12) "AM" "PM")))

(defn promote-by [f s]
  (->> s
       (reduce
        (fn [vs x]
          (update vs (if (f x) 0 1) conj x))
        [[] []])
       (apply concat)))

(defn bind [a x b]
  (-> x (max a) (min b)))

(defn interleave-all [a b]
  (let [c (min (count a) (count b))]
    (concat
     (interleave a b)
     (drop c a)
     (drop c b))))

(defn plural? [s]
  (> (count s) 1))

(defn map1 [f1 f2 s]
  (when (not-empty s)
        (conj
         (->> s rest (map f2))
         (-> s first f1))))

(def some-str #(some-> % .trim not-empty))

(defn- cond-class* [[a b c & rest]]
  (cond
   (and (string? b) (string? c))
   (concat
    [a `(str " " ~b) `(not ~a) `(str " " ~c)]
    (cond-class* rest))
   (string? b)
   (concat
    [a `(str " " ~b)]
    (cond-class* (conj rest c)))))

(defmacro cond-class [s & conditions]
  `(cond-> ~s ~@(cond-class* conditions)))

(defn map-last [f s]
  (let [j (-> s count dec)]
    (map-indexed
     (fn [i x]
       (f (= i j) x))
     s)))
