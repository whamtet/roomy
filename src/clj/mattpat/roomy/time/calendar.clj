(ns mattpat.roomy.time.calendar
    (:require
      [java-time.api :as jt]
      [mattpat.roomy.util :as util])
    (:import
      java.time.format.TextStyle
      java.util.Locale))

(defn- inc-day [a]
  (jt/plus a (jt/days 1)))
(defn- -day [a b]
  (jt/minus a (jt/days b)))

(defn calendar-range [year month week-start]
  (let [start (jt/local-date year month 1)
        offset (if (= "Sunday" week-start) 0 -1)]
    (->> start
         jt/day-of-week
         .getValue
         (+ offset)
         (-day start)
         (iterate inc-day)
         (take 42))))

(defn event->date [{:keys [start-setup]} tz]
  (-> start-setup
      (jt/with-zone-same-instant tz)
      jt/local-date))

(defn event->disp [start tz]
  (-> start
      (jt/with-zone-same-instant tz)
      (->> (jt/format "HH:mm"))))

(defn today? [d tz]
  (-> (jt/zoned-date-time)
      (jt/with-zone-same-instant tz)
      jt/local-date
      (= d)))

(defn this-month? [year month d]
  (-> d jt/year-month (= (jt/year-month year month))))

(defn format-datetime [d]
  (jt/format "YYYY-MM-dd" d))

(defn this-month []
  (let [ym (jt/year-month)]
    (->> ym .getMonth .getValue (list (.getYear ym)))))
