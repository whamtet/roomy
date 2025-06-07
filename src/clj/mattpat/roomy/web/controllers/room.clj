(ns mattpat.roomy.web.controllers.room
  (:require
    [mattpat.roomy.time :as time]
    [mattpat.roomy.util :as util]
    [mattpat.roomy.web.controllers.event :as event]
    [mattpat.roomy.web.controllers.room-static :as room-static]))

(def all-buildings room-static/all-buildings)
(def all-setups room-static/all-setups)
(def building->floors room-static/building->floors)
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

(defn- assoc-conj [m k v]
  (update m k conj v))

(defn- conj-single-booking [date-str tz]
  (fn [m [room-id attendees]]
    (let [minutes (* (rand-int (* 24 12)) 5)
          duration (-> 30 rand-int (* 5) (+ 30))
          start (time/random-time date-str minutes tz)
          end (time/random-time date-str (+ minutes duration) tz)
          {:keys [setup-time teardown-time]} (id->room room-id)]
      (assoc-conj m room-id
                  {:start start
                   :title (event/random-event)
                   :end end
                   :id room-id
                   :start-setup (time/-min start setup-time)
                   :setup? (pos? setup-time)
                   :end-teardown (time/+min end teardown-time)
                   :attendees attendees
                   :teardown? (pos? teardown-time)}))))

(defn- conj-random-booking [m date-str tz]
  (reduce
   (conj-single-booking date-str tz)
   m
   (room-static/random-allocation)))

(defn- room-services-assocer [room-id]
  (fn [{:keys [services start end] :as e}]
    (if-let [{:keys [setup-time teardown-time] :as service-times} (get services room-id)]
      (-> (merge e service-times)
          (assoc :start-setup (time/-min start setup-time) :end-teardown (time/+min end teardown-time)))
      e)))

#_
(defn get-bookings-db [date-str tz locked? room-id]
  (let [assoc-room-services (fn [{:keys [services start end] :as e}]
                              (if-let [{:keys [setup-time teardown-time] :as service-times} (get services room-id)]
                                (-> (merge e service-times)
                                    (assoc :start-setup (time/-min start setup-time) :end-teardown (time/+min end teardown-time)))
                                e))
        start-of-day (time/->zoned-date-time date-str tz)
        end-of-day (time/+day start-of-day 1)] #_
    (->> (calendar-view/view-for mailbox start-of-day end-of-day)
         (map #(assoc % :id room-id))
         (map assoc-room-services)
         (map #(util/rename % :subject :title)))))

(def bookings (atom {}))

(defn insert-booking [{:keys [tz week-start]}
                      service-info t1 t2
                      title _details
                      repeat-info])

(defn get-bookings-db
  ([date-str tz locked? room-id] (get-bookings-db date-str tz locked? room-id 0))
  ([date-str tz locked? room-id i]
   (or
    (->> (get @bookings room-id)
         (filter (time/filter-day date-str tz locked?))
         (map (room-services-assocer room-id))
         (sort-by :start)
         not-empty)
    (do
      (assert (zero? i) (prn date-str tz locked? room-id))
      (swap! bookings conj-random-booking date-str tz)
      (recur date-str tz locked? room-id 1)))))

(defn get-bookingss [date-str tz locked? companies]
  (mapcat #(get-bookings-db date-str tz locked? %) companies))

(defn- get-bookings-all [companies]
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
       (time/other-bookings start-date tz locked? (get-bookings-db start-date tz locked? room-id))))
