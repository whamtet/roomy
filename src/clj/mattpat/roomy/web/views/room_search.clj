(ns mattpat.roomy.web.views.room-search
    (:require
      [clojure.string :as string]
      [data.timezones :as timezones]
      [mattpat.roomy.time :as time]
      [mattpat.roomy.util :as util]
      [mattpat.roomy.web.controllers.room :as room]
      [mattpat.roomy.web.htmx :refer [defcomponent]]
      [mattpat.roomy.web.views.room-search.booking-modal :as booking-modal]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.icons :as icons]
      [mattpat.roomy.web.views.room-search.services :as services]
      [simpleui.core :as simpleui]))

(def col-width 120)
(def col-width-style "120px")

(defn- primary-string [hour tz twenty-four?]
  (if twenty-four?
    (format "%02d:00 %s" hour (timezones/id->short tz))
    (format "%s %s" (util/mod12-suffix-short hour) (timezones/id->short tz))))
(defn- other-strings [start-date hour tz1 tz2 twenty-four?]
  (let [[hour offset] (time/get-local start-date hour tz1 tz2)]
    (if twenty-four?
      (if offset
        (format "%02d:00 %s (%s%s)"
                hour
                (timezones/id->short tz2)
                (if (pos? offset) "+" "")
                offset)
        (format "%02d:00 %s"
                hour
                (timezones/id->short tz2)))
      (if offset
        (format "%s %s (%s%s)"
                (util/mod12-suffix-short hour)
                (timezones/id->short tz2)
                (if (pos? offset) "+" "")
                offset)
        (format "%s %s"
                (util/mod12-suffix-short hour)
                (timezones/id->short tz2))))))

(declare room-book)
(defn- bind [t]
  (if t
    (-> t (max 0) (min 60))
    0))
(defn- result-sort [locked?]
  (fn [{:keys [id]}]
    (if (locked? id)
      (if (.startsWith id "user") 0 0.5)
      1)))
(defn- merge-locked [locked? results]
  (for [result results]
    (->> result :id locked? (merge result))))
(defcomponent ^:endpoint lock [req
                               ^:edn results
                               start-date
                               room-id
                               ^:long-option new-setup
                               ^:long-option new-teardown
                               command]
  (cond
   top-level?
   (let [locked? (case command
                       "new-setup" (assoc-in locked? [room-id :setup-time] (bind new-setup))
                       "new-teardown" (assoc-in locked? [room-id :teardown-time] (bind new-teardown))
                       "remove" (dissoc locked? room-id)
                       (room/assoc-services locked? room-id))
         session (assoc session :locked? locked?)
         req (assoc req :session session)
         results (->> results
                      (sort-by (result-sort locked?))
                      (merge-locked locked?))]
     {:session session
      :body (list
             [:div#modal.hidden] ;; make modal disappear
             (->> results (take (count locked?)) (services/service-panel req))
             (room-book req results start-date))})
   (contains? locked? room-id)
   nil
   :else
   [:span {:class "cursor-pointer"
           :hx-post "lock"
           :hx-vals {:room-id room-id}
           :hx-target "#room-book"
           :hx-include ".lock-info"}
    icons/plus-circle]))

[:div {:class "w-[95px] text-gray-500 text-gray-700"}]
(defcomponent headers [req start-date results]
  [:div.flex.items-center
   [:div {:class "text-gray-500"
          :style {:width col-width-style}}
    start-date]
   (for [{:keys [id description first-name last-name]} results]
     [:div {:class "flex items-center"
            :style {:width col-width-style}}
      [:div {:class (if (locked? id)
                      "w-[90px] text-gray-700"
                      "w-[90px] text-gray-500")} (or description
                                                     (str first-name " " last-name))]
      (lock req results nil id nil nil nil)])])

(defn- timeline [start-date [primary-tz & tzs] twenty-four?]
  [:div {:class "flex flex-col"
         :style {:width col-width-style}}
   (map
    (fn [hour]
      [:div {:class "p-1 border h-[100px]"}
       [:div (primary-string hour primary-tz twenty-four?)]
       (for [tz tzs]
         [:div.text-gray-500.text-sm (other-strings start-date hour primary-tz tz twenty-four?)])])
    (range 24))])

(defn scale-time [t]
  (->> t (* 100/60) double (format "%.3fpx")))
(defn scale-top [t]
  (->> t (* 100/60) double inc (format "%.3fpx")))

(defn cross-booking-line [start-date room-id other-bookings]
  [:div {:class ""
         :id (str "cb-" (.replace start-date "/" "-") "-" room-id)}
   (map
    (fn [[minutes duration]]
      [:div {:class "bg-kkr-purple absolute opacity-50 border-b border-b-kkr-purple"
             :style {:top (scale-top minutes)
                     :width col-width-style
                     :height (scale-time duration)}}])
    other-bookings)])

[:div {:class "border h-[100px] border-black border-b-red-500 border-b-2"}]
[:div {:class "h-[25px] text-xs text-gray-300 border-t cursor-pointer
bg-slate-50"}]
(defn- booking-line [start-date tz {:keys [id description]} locked?]
  (let [other-bookings (room/other-bookings locked? id start-date tz)
        our-bookings (room/get-bookings-db start-date tz locked? id)
        endings (->> our-bookings
                     (map #(time/teardown-offset % start-date tz locked?))
                     (concat other-bookings)
                     (map (fn [[start duration]] (+ start duration))))]
    [:div {:class "flex flex-col relative overflow-hidden"
           :style {:width col-width-style}}
     ;; grid
     (map
      (fn [hour]
        [:div {:class "border h-[100px]"}
         (map
          (fn [subhour]
            (let [user? (.startsWith id "user")
                  past? (time/past? start-date hour (* subhour 15) tz)
                  past-next? (time/past-next? start-date hour (* subhour 15) tz 15)
                  past-previous? (time/past-next? start-date hour (* subhour 15) tz -15)
                  tval (+ (* 60 hour) (* 15 subhour))
                  intermediate (some #(when (< tval % (+ tval 15)) (mod % 60)) endings)]
              [:div {:class (cond-> "h-[25px] text-xs text-gray-300"
                              (or past? user?) (str " bg-slate-50")
                              (not (or past? user?)) (str " cursor-pointer")
                              (and past? (not past-next?)) (str " border-b-red-500 border-b-2")
                              (and (pos? subhour) (or past? (not past-previous?))) (str " border-t"))
                     :title (if past? "Past time" "Click to book")
                     :hx-get (when-not (or past? user?) "booking-modal")
                     :si-set [:room-id :start-date :tab-index]
                     :si-set-class "booking-info"
                     :si-clear [:hour :minute :hour2 :minute2]
                     :hx-vals {:room-id id
                               :tab-index 0
                               :start-date start-date
                               :hour hour
                               :minute (or intermediate (* subhour 15))}
                     :hx-target "#modal"}
               (* subhour 15)]))
          (range 4))])
      (range 24))
     ;; cross bookings
     (cross-booking-line start-date id other-bookings)
     ;; existing bookings
     (map
      (fn [{:keys [title repeat-instance?] :as booking}]
        (let [[minutes duration] (time/booking-offset booking start-date tz)
              [start end] (time/format-booking booking start-date tz)

              [setup-minutes setup-duration] (time/setup-offset booking start-date tz locked?)
              [teardown-minutes teardown-duration] (time/teardown-offset booking start-date tz locked?)
              attendees-disp (->> booking :attendees (map room/id->user-disp) (string/join ", "))]
          (list
           (when (pos? setup-duration)
             [:div {:class "bg-kkr-purple absolute text-sm text-white opacity-50"
                    :style {:top (scale-top setup-minutes)
                            :height (scale-time setup-duration)
                            :width col-width-style}
                    :title "Setup"}])
           [:div {:class "bg-kkr-purple absolute text-sm text-white border-b border-b-kkr-purple"
                  :style {:top (scale-top minutes)
                          :height (scale-time duration)
                          :width col-width-style}
                  :title attendees-disp}
            [:div title (when repeat-instance? " (Repeat)")]
            [:div start " to"]
            [:div end]]
           (when (pos? teardown-duration)
             [:div {:class "bg-kkr-purple absolute text-sm text-white opacity-50"
                    :style {:top (scale-top teardown-minutes)
                            :height (scale-time teardown-duration)
                            :width col-width-style}
                    :title "Teardown"}]))))
      our-bookings)]))

(defcomponent ^:endpoint room-book [req ^:edn results start-date]
  (let [tzs (-> (keep :tz results) (conj tz) distinct (->> (take 4)))]
    [:div {:id (when-not top-level? "room-book")}
     (when-not top-level?
               [:input#room-info.lock-info {:type "hidden"
                                            :name "results"
                                            :value (pr-str results)}])
     (headers req start-date results)
     [:div.flex
      (timeline start-date tzs tf)
      (map #(booking-line start-date tz % locked?) results)]
     [:div {:class "border border-black"
            :style {:width (-> results count inc (* col-width) (str "px"))}
            :hx-post "room-book"
            :hx-vals {:start-date (time/incd start-date)}
            :hx-include "#room-info"
            :hx-trigger "revealed"
            :hx-target "#loading"}]
     [:div#loading.p-10 "Loading..."]]))

(defn- building-filter [q-building buildings]
  [:select#building-filter {:class "room-search room-search2 border rounded-md mr-2"
                            :name "q-building"
                            :hx-get "room-search"
                            :hx-target "#room-book"
                            :hx-include ".room-search"}
   [:option {:value ""} "Filter building..."]
   (map
    (fn [building]
      [:option {:value building
                :selected (= building q-building)} building])
    buildings)])

(def floor-disp ["1st Floor"
                 "2nd Floor"
                 "3rd Floor"
                 "4th Floor"
                 "5th Floor"])

[:div.w-8.h-8]
(defn- floor-filter [q-floor floors]
  [:span#floor-filter.flex.items-stretch
   [:select {:class (if floors "room-search2 border rounded-md" "hidden")
             :name "q-floor"
             :hx-get "room-search"
             :hx-target "#room-book"
             :hx-include ".room-search"}
    [:option {:value ""} "Filter floor..."]
    (map
     (fn [floor]
       [:option {:value floor
                 :selected (= floor q-floor)} (floor-disp floor)])
     floors)]])

(defn- setup-filter [q-setup setups]
   [:select#setup-filter
    {:class "room-search room-search2 border rounded-md"
     :name "q-setup"
     :hx-get "room-search"
     :hx-target "#room-book"
     :hx-include ".room-search2"}
    [:option {:value ""} "Filter setup..."]
    (map
     (fn [setup]
       [:option {:value setup
                 :selected (= setup q-setup)} setup])
     setups)])

(defn- main-q [q]
  [:input#main-q {:class "room-search room-search2 p-2 border rounded-md w-96 mr-2"
                  :type "text"
                  :name "q"
                  :value q
                  :hx-get "room-search"
                  :hx-target "#room-book"
                  :hx-trigger "keyup changed delay:0.3s"
                  :hx-include ".room-search2"
                  :placeholder "Search rooms and people..."}])

(defn- capacity-q [capacity]
  [:input#capacity-q {:class "room-search room-search2 p-2 border rounded-md w-72 mr-2"
                      :type "number"
                      :min 1
                      :name "capacity"
                      :value capacity
                      :hx-get "room-search"
                      :hx-target "#room-book"
                      :hx-trigger "keyup changed delay:0.3s"
                      :hx-include ".room-search2"
                      :placeholder "Capacity (optional)"}])

(defmacro update-tz [body]
  `(let [~'tz (or ~'new-tz ~'tz)
         ~'session (assoc ~'session :tz ~'tz)
         ~'req (assoc ~'req :session ~'session)
         resp# ~body]
    (if ~'new-tz
      {:body (list
              [:p#tz-disp.my-2 (timezones/id->disp ~'tz)]
              resp#)
       :session ~'session}
      resp#)))

(defn- conjn [s x]
  (if x (conj s x) s))
(defcomponent ^:endpoint room-search [req
                                      ^:trim q
                                      ^:trim q-building
                                      ^:long-option q-floor
                                      ^:trim q-setup
                                      start-date
                                      ^:long-option capacity
                                      new-tz]
  booking-modal/booking-modal
  (update-tz
   (let [results (room/search-all q q-building q-floor locked? capacity q-setup)
         rooms (remove #(-> % :id (.startsWith "user")) results)
         buildings (if q
                     (-> (map :building rooms) (conjn q-building) distinct sort)
                     room/all-buildings)
         setups (if q
                  (-> (map :setup rooms) (conjn q-setup) distinct sort)
                  room/all-setups)
         floors (when q-building (room/building->floors q-building))]
     (if (-> req (get-in [:headers "hx-target"]) (not= "room-search") (and top-level?))
       (list
        (building-filter q-building buildings)
        (floor-filter q-floor floors)
        (setup-filter q-setup setups)
        (room-book req results start-date))
       [:div#room-search
        ;; trigger here on date change
        [:div#date-trigger {:hx-get "room-search"
                            :hx-include ".room-search2"
                            :hx-target "#room-search"}]
        ;; search bar
        [:div.flex.mb-2
         (main-q q)
         (building-filter q-building buildings)
         (floor-filter q-floor floors)]
        ;; secondary search bar
        [:div.flex.mb-5
         (capacity-q capacity)
         (setup-filter q-setup setups)]
        (if start-date
          (room-book req results start-date)
          [:div#room-book])]))))
