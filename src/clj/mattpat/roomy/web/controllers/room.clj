(ns mattpat.roomy.web.controllers.room
  (:require
    [clojure.tools.logging :as log]
    [mattpat.roomy.time :as time]
    [mattpat.roomy.util :as util]
    [mattpat.roomy.web.controllers.event :as event]
    [mattpat.roomy.web.controllers.room-static :as room-static]))

(def all-buildings room-static/all-buildings)
(def all-setups room-static/all-setups)
(def building->floors room-static/building->floors)
(def floor->rooms room-static/floor->rooms)
(def search-all room-static/search-all)
(def get-locked room-static/get-locked)
(def id->room room-static/id->room)
(def id->user room-static/id->user)
(def room-id->services room-static/room-id->services)

(def id->room-title #(-> % id->room :description))
(def id->user-disp #(let [{:keys [first-name last-name]} (id->user %)]
                     (str first-name " " last-name)))

(defn assoc-services [m room-id]
  (merge-with merge
   {room-id (-> room-id
                id->room
                (select-keys [:setup-time :teardown-time]))}
   m))

(defn get-bookings-db [db date-str tz locked? room-id]
  (let [assoc-room-services (fn [{:keys [services start end] :as e}]
                              (if-let [{:keys [setup-time teardown-time] :as service-times} (get services room-id)]
                                (-> (merge e service-times)
                                    (assoc :start-setup (time/-min start setup-time) :end-teardown (time/+min end teardown-time)))
                                e))
        {:keys [mailbox]} (id->room room-id)
        start-of-day (time/->zoned-date-time date-str tz)
        end-of-day (time/+day start-of-day 1)] #_
    (->> (calendar-view/view-for db mailbox start-of-day end-of-day)
         (map #(assoc % :id room-id))
         (map assoc-room-services)
         (map #(util/rename % :subject :title)))))

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

(defn- ->pattern [week-start
                  t1
                  {:keys [repeat-type
                          interval
                          days
                          date-pattern]}]
  (let [type (case repeat-type
                   "daily" "daily"
                   "weekly" "weekly"
                   "monthly" (if (= date-pattern "date") "absoluteMonthly" "relativeMonthly")
                   "yearly" (if (= date-pattern "date") "absoluteYearly" "relativeYearly"))]
    (cond-> {:firstDayOfWeek (if week-start (.toLowerCase week-start) "sunday")
             :interval interval
             :month (-> t1 .getMonth .getValue)
             :type type}
            (< 0 (count days) 7) (assoc :daysOfWeek (map #(.toLowerCase %) days))
            (#{"absoluteMonthly" "absoluteYearly"} type) (assoc :dayOfMonth (.getDayOfMonth t1)))))

(defn- ->range [tz t1 {:keys [limit-type end-date]}]
  (let [type (case limit-type
                   "forever" "noEnd"
                   "date" "endDate"
                   "numbered")]
    (cond-> {:startDate (time/format-recurrence t1)
             :type type}
            (= type "endDate") (assoc :endDate (time/format-recurrence end-date))
            (= type "numbered") (assoc :numberOfOccurrences limit-type))))

(defn- ->patterned-recurrence [week-start tz t1 m]
  (when (-> m :repeat-type #{"daily" "weekly" "monthly" "yearly"})
        {:pattern (->pattern week-start t1 m)
         :range (->range tz t1 m)}))

(defn insert-booking [graph
                      event-bus
                      {:keys [tz week-start]}
                      service-info t1 t2
                      title details
                      repeat-info]
  (let [user "matt@example.com"
        room-attendees (map id->room (keys service-info))
        recurrence (->patterned-recurrence week-start tz t1 repeat-info)]))

(defn get-bookings
  ([_ date-str tz locked? room-id] (get-bookings _ date-str tz locked? room-id 0))
  ([_ date-str tz locked? room-id i] #_
   (or
    (->> (get @bookings room-id)
         (filter (time/filter-day date-str tz locked?))
         (sort-by :start)
         not-empty)
    (do
      (assert (zero? i) (prn date-str tz locked? room-id))
      (swap! bookings conj-random-booking date-str tz)
      (recur _ date-str tz locked? room-id 1)))))

(defn get-bookingss [date-str tz locked? companies]
  (mapcat #(get-bookings nil date-str tz locked? %) companies))

(defn- get-bookings-all [companies] #_
  (mapcat @bookings companies))

(defn booking-limits [locked? room-id start-date hour minute tz]
  (let [bookings (->> locked? keys (get-bookingss start-date tz locked?))]
    [(time/latest-start start-date hour minute tz bookings locked?)
     (time/earliest-beginning start-date hour minute tz bookings locked?)]))

(defn booking-limits-multiday [locked? room-id start-date hour minute tz]
  (let [bookings (->> locked? keys get-bookings-all)]
    [(time/latest-start-multiday start-date hour minute tz bookings locked?)
     (time/earliest-beginning-multiday start-date hour minute tz bookings locked?)]))

(defn other-bookings [locked? room-id start-date tz]
  (->> (dissoc locked? room-id)
       keys
       (get-bookingss start-date tz locked?)
       (time/other-bookings start-date tz locked? (get-bookings nil start-date tz locked? room-id))))
