(ns mattpat.roomy.web.views.room-search.repeat
    (:require
      [mattpat.roomy.time :as time]
      [mattpat.roomy.util :as util :refer [mk]]
      [mattpat.roomy.web.htmx :refer [defcomponent]]
      [mattpat.roomy.web.views.icons :as icons]
      [mattpat.roomy.web.views.components :as components]))

(defn- capitalize [^String s]
  (-> (.substring s 0 1)
      .toUpperCase
      (str (.substring s 1))))

(defn- min-daily-frequency [tab-index multiday]
  (if (zero? tab-index) ;; we're on a single day, so min-daily-frequency is 1
    1
    (time/min-daily-frequency multiday)))

(defn allowed-periods [freq]
  (cond-> ["Daily"]
          (< freq 2) (conj "Weekly")
          (< freq 29) (conj "Monthly")
          (< freq 366) (conj "Yearly")))

(defn- dropdown [down? repeat start-date min-daily-frequency]
  [:div#repeat-popup.relative.hidden
   [:div {:class "fixed left-0 top-0 w-full h-full z-20"
          :_ "on click add .hidden to #repeat-popup"}]
   [:div {:class "absolute top-0.5 z-30
         border rounded-md cursor-pointer
         bg-white w-32"}
    [:div {:class "p-2 hover:bg-slate-100"
           :hx-include ".booking-info"
           :hx-target "#modal"
           :hx-get "booking-details"}
     "Don't repeat"]
    (for [option (allowed-periods min-daily-frequency)]
      (let [matching? (-> option .toLowerCase (= repeat))]
        [:div {:class "p-2 hover:bg-slate-100 border-t"
               :hx-get (when (or down? (not matching?)) "repeat-modal")
               :_ (when (and (not down?) matching?) "on click add .hidden to #repeat-popup")
               :si-clear ["interval" "limit-type" "end-date" "days" "date-pattern" "repeat-stored"]
               :hx-vals {:repeat (.toLowerCase option)
                         :start-date start-date}
               :hx-target "#modal"
               :hx-include ".booking-info"}
         option]))]])

(defn- repeat-selector [down? repeat start-date min-daily-frequency]
  [:div.inline-block
   [:div {:class "flex border rounded-md p-2 cursor-pointer gap-1"
          :_ "on click remove .hidden from #repeat-popup"}
    (when down? icons/arrow-path-rounded-square)
    (if repeat (capitalize repeat) "Don't repeat")
    icons/chevron-down]
   (dropdown down? repeat start-date min-daily-frequency)])

(def default-days
  ["Monday"
   "Tuesday"
   "Wednesday"
   "Thursday"
   "Friday"
   "Saturday"
   "Sunday"])

(defn- weekly-swap [repeat start-date]
  [:div {:class "hidden"
         :id "swap"
         :hx-get "repeat-modal"
         :hx-vals {:repeat (case repeat "daily" "weekly" "weekly" "daily")
                   :start-date start-date}
         :hx-target "#modal"
         :hx-include ".booking-info"}])

[:div {:class "ml-2 border rounded-md booking-info"}]
[:div {:class "flex items-center text-lg gap-3"}]
[:div {:class "bg-gray-200 rounded-full w-8 h-8"}]
(defn daily-weekly [repeat start-date interval limit-type end-date day-selected? min-daily-frequency]
  (list
   (components/hiddens
    "#days.booking-info" nil)
   (weekly-swap repeat start-date)
   [:div {:class "flex items-center my-4 text-lg"}
    "From " start-date " repeat every"
    [:select {:class "border rounded-md mx-2 booking-info"
              :name "interval"
              :id "interval"
              :_ (if (= "daily" repeat)
                   "on change refreshSummary() if target.value > 1 then add .hidden to #day-selector else remove .hidden from #day-selector"
                   "on change refreshSummary()")}
     (for [i (range min-daily-frequency 100)]
       [:option {:value i
                 :selected (= i interval)} i])]
    (if (= repeat "daily")
      "days until" "weeks until")
    [:select
     {:class "border rounded-md ml-2 booking-info"
      :name "limit-type"
      :id "limit-type"
      :_ "on change refreshSummary() "}
     [:option {:value "forever"
               :selected (= limit-type "forever")} "forever"]
     [:option {:value "date"
               :selected (= limit-type "date")} "date"]
     (for [i (range 1 100)]
       [:option {:value i
                 :select (= limit-type (str i))} (str i " times")])]
    [:input {:class (cond-> "ml-2 border rounded-md booking-info"
                            (not= limit-type "date") (str " hidden"))
             :id "end-date"
             :name "end-date"}]
    [:script (util/format-json "addDatepicker('#end-date', %s, refreshSummary, %s)" end-date start-date)]]
   (when (= 1 min-daily-frequency)
         [:div#day-selector {:class (cond-> "flex items-center text-lg gap-3"
                                            (and (= "daily" repeat) interval (> interval 1)) (str " hidden"))}
          (for [day default-days]
            [:button {:class (cond-> "bg-gray-200 rounded-full w-8 h-8"
                                     (day-selected? day) (str " weekday"))
                      :type "button"
                      :onclick "clickDay(event)"
                      :value day}
             (.substring day 0 1)])])))

(defn- monthly-header [start-date interval limit-type end-date]
  [:div {:class "flex items-center my-4 text-lg"}
   "From " start-date " repeat every"
   [:select {:class "border rounded-md mx-2 booking-info"
             :name "interval"
             :id "interval"
             :onchange "refreshSummaryMonthly()"}
    (for [i (range 1 100)]
      [:option {:value i
                :selected (= i interval)} i])]
   "months until"
   [:select
    {:class "border rounded-md ml-2 booking-info"
     :name "limit-type"
     :id "limit-type"
     :_ "on change refreshSummaryMonthly() "}
    [:option {:value "forever"
              :selected (= limit-type "forever")} "forever"]
    [:option {:value "date"
              :selected (= limit-type "date")} "date"]
    (for [i (range 1 100)]
      [:option {:value i
                :select (= limit-type (str i))} (str i " times")])]
   [:input {:class (cond-> "ml-2 border rounded-md booking-info"
                           (not= limit-type "date") (str " hidden"))
            :id "end-date"
            :name "end-date"}]
   [:script (util/format-json "addDatepicker('#end-date', %s, refreshSummaryMonthly, %s)" end-date start-date)]])

(defn- yearly-header [start-date interval limit-type]
  (let [init-year (time/year start-date)]
    [:div {:class "flex items-center my-4 text-lg"}
     "From " start-date " repeat every year until"
     [:select
      {:class "border rounded-md ml-2 booking-info"
       :name "limit-type"
       :id "limit-type"
       :onchange "refreshSummaryYearly()"}
      [:option {:value "forever"
                :selected (= limit-type "forever")} "forever"]
      (for [year (range (inc init-year) (+ init-year 50))]
        (let [year-disp (.replace start-date (str init-year) (str year))]
          [:option {:value year-disp
                    :selected (= limit-type year-disp)} year-disp]))]]))

(def ordinals ["first" "second" "third" "fourth"])
(defn monthly-yearly [repeat start-date interval limit-type end-date date-pattern]
  (let [date (time/day-of-month start-date)
        week (-> date dec (/ 7) Math/floor long)
        weekday (time/day-of-week start-date)
        month (time/month start-date)
        monthly? (= repeat "monthly")
        onchange (if monthly? "refreshSummaryMonthly()" "refreshSummaryYearly()")]
    (list
     (if monthly?
       (monthly-header start-date interval limit-type end-date)
       (yearly-header start-date interval limit-type))
     (components/hiddens
      "#date" date
      "#week" (format "%s %s" (get ordinals week) weekday)
      "#weekday" weekday
      "#month" month)
     [:div.text-lg
      [:div
       [:input {:class "mr-2 booking-info date-pattern"
                :type "radio"
                :name "date-pattern"
                :value "date"
                :onchange onchange
                :checked (or (not date-pattern) (= "date" date-pattern))}]
       (format "On %s %s"
               (if monthly? "day" month)
               date)]
      (when (< week 4)
            [:div
             [:input {:class "mr-2 booking-info date-pattern"
                      :type "radio"
                      :name "date-pattern"
                      :value "week"
                      :onchange onchange
                      :checked (= "week" date-pattern)}]
             (format "On the %s %s %s"
                     (ordinals week)
                     weekday
                     (if monthly? "" (format "of %s" month)))])
      (when (> week 2)
            [:div
             [:input {:class "mr-2 booking-info date-pattern"
                      :type "radio"
                      :name "date-pattern"
                      :value "end-week"
                      :onchange onchange
                      :checked (= "end-week" date-pattern)}]
             (format "On the last %s %s"
                     weekday
                     (if monthly? "" (format "of %s" month)))])]
     (when-not monthly? [:script "refreshSummaryYearly();"])
     )))

[:div {:class "w-3/4"}]
(defcomponent ^:endpoint repeat-modal [req
                                       repeat
                                       start-date
                                       ^:long interval
                                       limit-type
                                       end-date
                                       ^:json days
                                       date-pattern
                                       ^:long tab-index
                                       multiday]
  (let [end-date (or end-date (time/update-local-date start-date time/+day 7))
        day-selected? (set (or days default-days))
        min-daily-frequency (min-daily-frequency tab-index multiday)]
    (components/modal "w-3/4"
                      [:div.p-2
                       ;; dropdown to change repeat type
                       [:div.flex.items-center
                        [:h2.mr-2 "Repeat"]
                        (repeat-selector false repeat start-date min-daily-frequency)]
                       [:form {:hx-post "booking-details"
                               :hx-include ".booking-info"
                               :hx-target "#modal"
                               :si-set ["interval" "limit-type" "end-date" "days" "date-pattern" "repeat-stored"]
                               :si-set-class "booking-info"}
                        (components/hiddens
                         "#repeat-stored" repeat)
                        (if (#{"daily" "weekly"} repeat)
                          (daily-weekly repeat start-date interval limit-type end-date day-selected? min-daily-frequency)
                          (monthly-yearly repeat start-date interval limit-type end-date date-pattern))
                        [:div#repeat-summary.my-3.text-gray-600]
                        [:div.flex
                         [:span {:class "mr-2"
                                 :hx-get "booking-details"
                                 :hx-include ".booking-info"
                                 :hx-target "#modal"}
                          (components/button-light "Cancel")]
                         (components/submit "Save")]
                        ]])))

(defcomponent ^:endpoint repeat-select [req
                                        repeat-stored
                                        start-date
                                        ^:long tab-index
                                        multiday]
  repeat-modal
  (repeat-selector
   true
   repeat-stored
   start-date
   (min-daily-frequency tab-index multiday)))
