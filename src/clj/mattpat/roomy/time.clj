(ns mattpat.roomy.time
    (:require
      [java-time.api :as jt]
      [mattpat.roomy.util :as util])
  (:import
    java.time.format.TextStyle
    java.util.Locale))

(defn +min [a b]
  (jt/plus a (jt/minutes b)))
(defn -min [a b]
  (jt/minus a (jt/minutes b)))
(defn +day [a b]
  (jt/plus a (jt/days b)))
(defn +hour [a b]
  (jt/plus a (jt/hours b)))
(defn +month [a b]
  (jt/plus a (jt/months b)))

(def format-date "MM/dd/yyyy")
(def format-date-hour "MM/dd/yyyy HH")
(def format-date-time "MM/dd/yyyy HH:mm")
(def format-input "HH:mm")

(defn format-recurrence [s]
  (if (string? s)
    (let [[mm dd yyyy] (.split s "/")]
      (str yyyy "-" mm "-" dd))
    (jt/format "yyyy-MM-dd" s)))

(defn format-full [s]
  (jt/format format-date-time s))

(defn ->local-date [s]
  (jt/local-date format-date s))

(defn day-of-month [s]
  (-> s ->local-date .getDayOfMonth))
(defn day-of-week [s]
  (-> s ->local-date .getDayOfWeek (.getDisplayName TextStyle/FULL Locale/ENGLISH)))
(defn month [s]
  (-> s ->local-date .getMonth (.getDisplayName TextStyle/FULL Locale/ENGLISH)))
(defn year [s]
  (-> s ->local-date .getYear))

(defn <-local-date [s]
  (jt/format format-date s))

(defn format-time [hours minutes]
  (format "%02d:%02d" hours minutes))

(defn update-local-date [s f & args]
  (as-> (->local-date s) x
        (apply f x args)
        (<-local-date x)))

(defn incd [s]
  (-> s
      ->local-date
      (+day 1)
      <-local-date))

(defn ->local-date-time
  ([date-str]
   (if (.contains date-str " ")
     (jt/local-date-time format-date-time date-str)
     (->local-date-time date-str 0)))
  ([date-str hour]
   (jt/local-date-time format-date-hour (format "%s %02d" date-str hour)))
  ([date-str hour minute]
   (jt/local-date-time format-date-time (format "%s %02d:%02d" date-str hour minute))))

(defn ->zoned-date-time [date-str tz]
  (jt/zoned-date-time (->local-date-time date-str) tz))

(defn past? [date-str hour minute tz]
  (-> (->local-date-time date-str hour minute)
      (jt/zoned-date-time tz)
      (jt/< (jt/zoned-date-time))))

(defn past-next?
  "used by calendar display"
  [date-str hour minute tz offset]
  (-> (->local-date-time date-str hour minute)
      (jt/zoned-date-time tz)
      (+min offset)
      (jt/< (jt/zoned-date-time))))

(defn date-diff [t1 t2]
  (.getDays
    (jt/period (jt/local-date t1) (jt/local-date t2))))

(defn get-local
  "displaying timezones"
  [date-str hour id1 id2]
  (let [local1 (->local-date-time date-str hour)
        local2 (-> local1 (jt/zoned-date-time id1) (jt/local-date-time id2))
        date-diff (date-diff local1 local2)]
    (if (zero? date-diff)
      [(.getHour local2)]
      [(.getHour local2) date-diff])))

(defn- prebuffer [{:keys [id start-setup]} locked?]
  (-min start-setup (get-in locked? [id :teardown-time] 0)))
(defn- postbuffer [{:keys [id end-teardown]} locked?]
  (+min end-teardown (get-in locked? [id :setup-time] 0)))

#_
(defn filter-day [date-str tz locked?]
  (let [start (jt/zoned-date-time (->local-date-time date-str) tz)
        end (+day start 1)]
    #(and (jt/< start (postbuffer % locked?)) (jt/< (prebuffer % locked?) end))))

(defn filter-before-bookings [date-str tz locked?]
  (let [end (-> date-str
                ->local-date-time
                (jt/zoned-date-time tz)
                (+day 1))]
    #(jt/< (prebuffer % locked?) end)))

(defn filter-drop-bookings [date-str tz locked?]
  (let [start (-> date-str
                  ->local-date-time
                  (jt/zoned-date-time tz))]
    #(jt/<= (postbuffer % locked?) start)))

(defn filter-before [date-str hour minute tz]
  (let [t (jt/zoned-date-time (->local-date-time date-str hour minute) tz)]
    #(jt/<= (:postbuffer %) t)))
(defn filter-after [date-str hour minute tz]
  (let [t (jt/zoned-date-time (->local-date-time date-str hour minute) tz)]
    #(jt/<= t (:prebuffer %))))

(defn- now [tz]
  (-> (jt/zoned-date-time)
      (jt/zoned-date-time tz)
      (jt/truncate-to :minutes)
      (+min 1)))

(defn- js5 [d]
  [(.getYear d)
   (dec (.getMonthValue d))
   (.getDayOfMonth d)
   (.getHour d)
   (.getMinute d)])

(defn- d-mod [d] (mod (.getMinute d) 5))
(defn- js5-down
  "outputs js array"
  [d]
  (-> d
      (-min (d-mod d))
      js5))
(defn- js5-up
  "outputs js array"
  [d]
  (js5
   (let [mod (d-mod d)]
     (if (zero? mod)
       d
       (+min d (- 5 mod))))))

(defn- trunc5-down [x]
  (- x (mod x 5)))
(defn- trunc5-up [x]
  (let [mod (mod x 5)]
    (if (pos? mod)
      (+ x 5 (- mod))
      x)))

(defn latest-start-multiday
  "latest start before date-str hour minute"
  [date-str hour minute tz events locked?]
  (let [now {:postbuffer (now tz) :now? true}
        {:keys [title teardown? postbuffer now?]}
        (->> events
             (map #(assoc % :postbuffer (postbuffer % locked?)))
             (filter (filter-before date-str hour minute tz))
             (concat [now])
             (util/max-by :postbuffer))]
    {:now? now?
     :previous-title title
     :min-time (jt/format format-input postbuffer)
     :min-date (jt/format format-date postbuffer)
     :min-js (js5-up postbuffer)
     :teardown? teardown?}))

(defn latest-start
  "latest start before date-str hour minute"
  [date-str hour minute tz events locked?]
  (let [start-of-day {:postbuffer (-> date-str ->local-date-time (jt/zoned-date-time tz))}
        now {:postbuffer (now tz) :now? true}
        {:keys [now? title teardown? postbuffer]}
        (->> events
             (map #(assoc % :postbuffer (postbuffer % locked?)))
             (filter (filter-before date-str hour minute tz))
             (concat [start-of-day now])
             (util/max-by :postbuffer))]
    {:now? now?
     :previous-title title
     :min-time (jt/format format-input postbuffer)
     :h1 (.getHour postbuffer)
     :m1 (-> postbuffer .getMinute trunc5-up)
     :teardown? teardown?}))

(defn earliest-beginning-multiday
  "earliest beginning after date-str hour minute"
  [date-str hour minute tz events locked?]
  (when-let [{:keys [title setup? prebuffer]}
             (->> events
                  (map #(assoc % :prebuffer (prebuffer % locked?)))
                  (filter (filter-after date-str hour minute tz))
                  (util/min-by :prebuffer))]
    {:next-title title
     :max-time (jt/format format-input prebuffer)
     :max-date (jt/format format-date prebuffer)
     :max-js (js5-down prebuffer)
     :setup? setup?}))

(defn earliest-beginning
  "earliest beginning after date-str hour minute"
  [date-str hour minute tz events locked?]
  (let [end-of-day {:prebuffer (-> date-str ->local-date-time (jt/zoned-date-time tz) (+day 1))}
        {:keys [title setup? prebuffer]}
        (->> events
             (map #(assoc % :prebuffer (prebuffer % locked?)))
             (filter (filter-after date-str hour minute tz))
             (concat [end-of-day])
             (util/min-by :prebuffer))]
    {:next-title title
     :max-time (jt/format format-input prebuffer)
     :h2 (when title (.getHour prebuffer))
     :m2 (when title (-> prebuffer .getMinute trunc5-down))
     :setup? setup?}))

(defn random-time
  "ZonedDateTime on date-str at minutes after midnight"
  [date-str minutes tz]
  (-> date-str
      ->local-date-time
      (+min minutes)
      (jt/zoned-date-time tz)))

(defn- minutes-between [t1 t2]
  (jt/time-between t1 t2 :minutes))

(defn min-daily-frequency
  "s is from multiday selector e.g. 06/04/2025 15:00,06/11/2025 15:00.
  used to calculate "
  [s]
  (let [[a b] (.split s ",")]
    (inc
     (jt/time-between
      (->local-date-time a)
      (->local-date-time b)
      :days))))

(defn- booking-offsets*
  "[minutes from midnight, duration in minutes]"
  [start end date-str tz]
  [(-> date-str
       ->local-date-time
       (jt/zoned-date-time tz)
       (minutes-between start))
   (minutes-between start end)])
(defmacro bo [a b]
  `(booking-offsets* ~a ~b ~'date-str ~'tz))

(defn booking-offset
  "for display"
  [{:keys [start end]} date-str tz]
  (bo start end))
(defn setup-offset
  "for display"
  [event date-str tz locked?]
  (bo (prebuffer event locked?) (:start event)))
(defn teardown-offset
  "for display"
  [event date-str tz locked?]
  (bo (:end event) (postbuffer event locked?)))

(defn- fuse-overlaps*
  "fuse bookings together (for display)"
  [[head & rest]]
  (reduce
   (fn [[[a b] & rest :as done] [c d :as next]]
     (if (jt/<= c b)
       (conj rest [a d])
       (conj done next)))
   (list head)
   rest))
(defn- fuse-overlaps [s]
  (when (not-empty s)
        (reverse (fuse-overlaps* s))))

(defn- subtract-booking
  "cut our booking out of their booking"
  [[their-start their-end] [our-start our-end]]
  (if (and (jt/< our-start their-end) (jt/< their-start our-end))
    ;; we're overlapping them
    (cond
      (and (jt/< their-start our-start) (jt/< our-end their-end))
      ;; we're inside them
      [[their-start our-start] [our-end their-end]]
      (and (jt/< our-start their-start) (jt/< their-end our-end))
      ;; they're inside us
      nil
      (jt/< our-start their-start)
      ;; we overlap their start
      [[our-end their-end]]
      :else
      ;; we overlap their end
      [[their-start our-start]])
    [[their-start their-end]]))

(defn- remove-bookings
  "n^2 cut our-bookings out of bookings"
  [our-bookings bookings]
  (reduce
   (fn [bookings our-booking]
     (mapcat #(subtract-booking % our-booking) bookings))
   bookings
   our-bookings))

(defn other-bookings
  "cut our-bookings out of bookings (from other)"
  [date-str tz locked? our-bookings bookings]
  (let [booking-pair (fn [booking]
                       [(prebuffer booking locked?)
                        (postbuffer booking locked?)])
        fuse #(->> % (map booking-pair) (sort-by first) fuse-overlaps)]
    (->> bookings
         fuse
         (remove-bookings (fuse our-bookings))
         (map (fn [[start end]] (bo start end))))))

(defn- format-diff
  "show time on calendar"
  [t date-str tz]
  (let [t1 (jt/zoned-date-time (->local-date-time date-str) tz)
        t2 (jt/zoned-date-time t tz)
        s1 (jt/format "HH:mm" t2)
        date-diff (date-diff t1 t2)]
    (cond
     (neg? date-diff) (format "%s (%s)" s1 date-diff)
     (zero? date-diff) s1
     (pos? date-diff) (format "%s (+%s)" s1 date-diff))))

(defn format-booking
  "show start and end on calendar"
  [{:keys [start end]} date-str tz]
  [(format-diff start date-str tz)
   (format-diff end date-str tz)])
