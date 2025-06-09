(ns mattpat.roomy.web.views.calendar
    (:require
      [clojure.java.io :as io]
      [mattpat.roomy.time.calendar :as calendar]
      [mattpat.roomy.util :as util :refer [defm-dev]]
      [mattpat.roomy.web.controllers.room :as room]
      [mattpat.roomy.web.views.icons :as icons]))

(defm-dev fragments []
  (-> "templates/calendar.html" io/resource slurp (.split "%s") seq))

(def months
  [nil
   "January"
   "February"
   "March"
   "April"
   "May"
   "June"
   "July"
   "August"
   "September"
   "October"
   "November"
   "December"])

(defn- dec-ym [year month]
  (if (= month 1)
    {:year (dec year) :month 12}
    {:year year :month (dec month)}))

(defn- inc-ym [year month]
  (if (= month 12)
    {:year (inc year) :month 1}
    {:year year :month (inc month)}))

(defn navigator [year month]
  (list
   [:button {:type "button"
             :hx-get "calendar"
             :hx-vals (dec-ym year month)
             :class "flex h-9 w-12 items-center justify-center pr-2 text-gray-400 hover:text-gray-500 focus:relative md:w-9 md:pr-0 md:hover:bg-gray-50"}
    [:svg {:class "h-5 w-5"
           :viewBox "0 0 20 20"
           :fill "currentColor"
           :aria-hidden "true"}
     [:path {:fill-rule "evenodd"
             :d "M12.79 5.23a.75.75 0 01-.02 1.06L8.832 10l3.938 3.71a.75.75 0 11-1.04 1.08l-4.5-4.25a.75.75 0 010-1.08l4.5-4.25a.75.75 0 011.06.02z"
             :clip-rule "evenodd"}]]]
   [:time {:datetime (format "%s-%02d" year month)}
    (months month) " " year]
   [:button {:type "button"
             :hx-get "calendar"
             :hx-vals (inc-ym year month)
             :class "flex h-9 w-12 items-center justify-center rounded-r-md pl-2 text-gray-400 hover:text-gray-500 focus:relative md:w-9 md:pl-0 md:hover:bg-gray-50"}
    [:svg {:class "h-5 w-5"
           :viewBox "0 0 20 20"
           :fill "currentColor"
           :aria-hidden "true"}
     [:path {:fill-rule="evenodd"
             :d "M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z"
             :clip-rule "evenodd"}]]]))

(defn- header [day]
  [:div {:class "flex justify-center bg-white py-2"}
   [:span (.substring day 0 1)]
   [:span {:class "sr-only sm:not-sr-only"} (.substring day 1)]])

[:div {:class "relative py-2 px-3"}]
[:div {:class "bg-white"}]
[:div {:class "bg-gray-50 text-gray-500"}]
[:div {:class "flex h-6 w-6 items-center justify-center rounded-full bg-indigo-600 font-semibold text-white"}]
(defn- d-desktop [year month d tz bookings]
  [:div {:class (util/cond-class "relative py-2 px-3"
                                 (calendar/this-month? year month d) "bg-white" "bg-gray-50 text-gray-500")}
   [:time {:class (when (calendar/today? d tz) "flex h-6 w-6 items-center justify-center rounded-full bg-indigo-600 font-semibold text-white")
           :datetime (calendar/format-datetime d)} (.getDayOfMonth d)]
   (for [{:keys [title start]} bookings]
     [:div (calendar/event->disp start tz) " " title])])

[:div {:class "flex h-14 flex-col py-2 px-3 hover:bg-gray-100 focus:z-10"}]
[:div {:class "bg-white bg-gray-50"}]
[:div {:class "font-semibold text-indigo-600"}]
[:div {:class "text-gray-900"}]
[:div {:class "text-gray-500"}]
(defn- d-mobile [year month d tz bookings]
  (let [this-month? (calendar/this-month? year month d)
        today? (calendar/today? d tz)]
    [:button {:class (util/cond-class "flex h-14 flex-col py-2 px-3 hover:bg-gray-100 focus:z-10"
                                      this-month? "bg-white" "bg-gray-50"
                                      today? "font-semibold text-indigo-600"
                                      (and this-month? (not today?)) "text-gray-900"
                                      (and (not this-month?) (not today?)) "text-gray-500")
              :type "button"}
     [:time.ml-auto {:datetime (calendar/format-datetime d)} (.getDayOfMonth d)]
     (for [{:keys [title start]} bookings]
       [:div (calendar/event->disp start tz) " " title])]))

(defn calendar
  ([tz week-start]
   (let [[year month] (calendar/this-month)]
     (calendar year month tz week-start)))
  ([year month tz week-start]
   (let [range (calendar/calendar-range year month week-start)
         days (if (= "Sunday" week-start)
                ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"]
                ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"])
         bookings (group-by
                   #(calendar/event->date % tz)
                   (room/get-bookings-month year month tz))]
     ;; we interleave because we're mixing html with hiccup
     (util/interleave-all
      (fragments)
      [(navigator year month)
       (map header days)
       (map #(d-desktop year month % tz (bookings %)) range)
       (map #(d-mobile year month % tz (bookings %)) range)]))))
