(ns mattpat.roomy.web.controllers.room
  (:require
    [mattpat.roomy.time :as time]
    [mattpat.roomy.time.repeat :as repeat]
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

(defn- conj-random-booking [date-str tz]
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
                   :start-setup (time/-min start setup-time)
                   :setup? (pos? setup-time)
                   :end-teardown (time/+min end teardown-time)
                   :attendees attendees
                   :teardown? (pos? teardown-time)}))))

(defn- make-booking [t1 t2 title repeat]
  (cond-> {:start t1
           :title (event/random-event) ;; TODO - change
           :end t2
           :start-setup t1
           :setup? false
           :end-teardown t2
           :attendees []
           :teardown? false}
    (:repeat-type repeat) (assoc :repeat repeat)))

(def bookings (atom {}))
(def bookings-self (atom nil))

(defn- conj-random-bookings [m date-str tz]
  (reduce
   (conj-random-booking date-str tz)
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

(defn insert-booking [{:keys [tz week-start]}
                      service-info t1 t2
                      title _details
                      repeat-info]
  (let [booking (make-booking t1 t2 title repeat-info)
        rooms (keys service-info)
        booking-self (assoc booking :rooms (set rooms))]
    (swap! bookings
           #(reduce
             (fn [m room-id]
               (assoc-conj m room-id booking))
             %
             rooms))
    (swap! bookings-self conj booking-self)))

(defn- remove-repeats
  "The UI prevents clashing bookings, but repeats can still clash"
  [events]
  (loop [[event & todo] events
         to-check (set events)
         done ()]
    (if event
      (if (:repeat-instance? event)
        (if (some #(time/overlap? % event) to-check)
          (recur todo (disj to-check event) done)
          (recur todo to-check (conj done event)))
        (recur todo to-check (conj done event)))
      done)))

(defn- remove-repeats-shared
  "The UI prevents clashing bookings, but repeats can still clash"
  [events]
  (loop [[event & todo] events
         to-check (set events)
         done ()]
    (if event
      (if (:repeat-instance? event)
        (if (some #(time/overlap-shared? % event) to-check)
          (recur todo (disj to-check event) done)
          (recur todo to-check (conj done event)))
        (recur todo to-check (conj done event)))
      done)))

(defn get-bookings-db
  ([date-str tz locked? room-id] (get-bookings-db date-str tz locked? room-id 0))
  ([date-str tz locked? room-id i]
   (let [before-f (time/filter-before-bookings date-str tz locked?)
         drop-f (time/filter-drop-bookings date-str tz locked?)]
     (or
      (->> room-id
           (get @bookings)
           (filter before-f)
           (mapcat
            (fn [event]
              (->> event
                   repeat/generate
                   (drop-while drop-f)
                   (take-while before-f))))
           remove-repeats
           (map (room-services-assocer room-id))
           (sort-by :start)
           not-empty)
      (do
        (assert (zero? i) (prn date-str tz locked? room-id))
        (swap! bookings conj-random-bookings date-str tz)
        (recur date-str tz locked? room-id 1))))))

(defn get-bookings-month [year month tz]
  (let [before-f (time/filter-before-month year month tz)
        drop-f (time/filter-drop-month year month tz)]
    (->> @bookings-self
         (filter before-f)
         (mapcat
          (fn [event]
            (->> event
                 repeat/generate
                 (drop-while drop-f)
                 (take-while before-f))))
         remove-repeats-shared)))

(defn- get-bookingss [date-str tz locked? companies]
  (mapcat #(get-bookings-db date-str tz locked? %) companies))

(defn- get-bookings-all* [date-str tz locked? room-id]
  (let [before-f (time/filter-before-bookings date-str tz locked?)]
    (->> room-id
         (get @bookings)
         (mapcat
          (fn [event]
            (if (before-f event)
              (->> event
                   repeat/generate
                   (take-while before-f))
              [event])))
         (map (room-services-assocer room-id)))))

(defn- get-bookings-all [date-str tz locked? room-ids]
  (mapcat #(get-bookings-all* date-str tz locked? %) room-ids))

(defn booking-limits [locked? room-id start-date hour minute tz]
  (let [bookings (->> locked? keys (get-bookingss start-date tz locked?))]
    [(time/latest-start start-date hour minute tz bookings locked?)
     (time/earliest-beginning start-date hour minute tz bookings locked?)]))

(defn booking-limits-multiday [locked? room-id start-date hour minute tz]
  (let [bookings (->> locked? keys (get-bookings-all start-date tz locked?))]
    [(time/latest-start-multiday start-date hour minute tz bookings locked?)
     (time/earliest-beginning-multiday start-date hour minute tz bookings locked?)]))

(defn other-bookings
  "we need to take our bookings out of the other bookings"
  [locked? room-id start-date tz]
  (->> (dissoc locked? room-id)
       keys
       (get-bookingss start-date tz locked?)
       (time/other-bookings start-date tz locked? (get-bookings-db start-date tz locked? room-id))))
