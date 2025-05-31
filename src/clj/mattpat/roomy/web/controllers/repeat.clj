(ns mattpat.roomy.web.controllers.repeat
  (:require
    [java-time.api :as jt]
    [mattpat.roomy.time :refer [+day +month ->local-date]]))

(def repeat-schema
  [:map
   [:repeat-type [:enum "daily" "weekly" "monthly" "yearly"]]
   [:interval :int] ;; long? interval means repeating every nth
   [:limit-type [:or [:enum "forever" "date"] :int]]
   [:end-date string?] ;; "MM/dd/yyyy", same as standard format.  ignored when limit-type = "forever"
   [:days [:vector [:enum "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday" "Sunday"]]]
   [:date-pattern [:enum "date" "week" "end-week"]]
   ;; date means on fixed date, 7th, 14th etc.  date matches date of booking
   ;; week means on the 1st, 2nd 3rd Wednesday of that month determined by initial booking date
   ;; end-week means on he last Wednesday of that month
   ])

(def dayval {"Monday" 0
             "Tuesday" 1
             "Wednesday" 2
             "Thursday" 3
             "Friday" 4
             "Saturday" 5
             "Sunday" 6})

(defmulti repeat* :repeat-type)

(defmethod repeat* "daily" [start {:keys [interval]}]
  (iterate #(+day % interval) start))

;; in the current UI weeks are always starting monday
(defmethod repeat* "weekly" [start {:keys [interval days]}]
  (let [offset (-> start .getDayOfWeek .getValue dec -)
        day-offsets (map dayval days)
        all-offsets (for [i (range) day-offset day-offsets]
                      (+ offset day-offset (* i 7 interval)))]
    (->> all-offsets
         (drop-while neg?)
         (map #(+day start %)))))

(defn- week-seq [i day-of-week]
  (let [month (jt/month (inc (mod i 12)))]
    (->> (jt/local-date (quot i 12) (inc (mod i 12)) 1)
         (iterate #(+day % 1))
         (some #(when (-> % .getDayOfWeek (= day-of-week)) %))
         (iterate #(+day % 7))
         (take-while #(-> % .getMonth (= month))))))

(defn- nth-max [s i]
  (->> s count dec (min i) (nth s)))

(defmethod repeat* "monthly" [start {:keys [interval date-pattern]}]
  (if (= "date" date-pattern)
    (iterate #(+month % interval) start)
    (let [day-of-week (.getDayOfWeek start)
          i0 (+ (* 12 (.getYear start)) (.getMonthValue start) -1)]
      (if (= "end-week" date-pattern)
        (map #(-> % (* interval) (+ i0) (week-seq day-of-week) last) (range))
        (let [k (->> (week-seq i0 day-of-week) (take-while #(not= % start)) count)]
          (map #(-> % (* interval) (+ i0) (week-seq day-of-week) (nth-max k)) (range)))))))

(defmethod repeat* "yearly" [start m]
  (repeat* start (assoc m :interval 12 :repeat-type "monthly")))

(defn repeat-dates [start {:keys [limit-type end-date] :as m}]
  (let [s (repeat* start m)]
    (case limit-type
      "forever" s
      "date" (let [end-date (->local-date end-date)]
               (take-while #(jt/<= % end-date) s))
      (take limit-type s))))
