(ns mattpat.roomy.web.views.room-search.booking-multiday
    (:require
      [clojure.string :as string]
      [mattpat.roomy.time :as time]
      [mattpat.roomy.util :as util :refer [format-js$]]
      [mattpat.roomy.web.controllers.room :as room]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.room-search.services :as services]))

(defn js-date [js-triple]
  (if js-triple
    (apply format "new Date(%s, %s, %s)" js-triple)
    false))
(defn js-datetime [js-time]
  (if js-time
    (apply format "new Date(%s, %s, %s, %s, %s)" js-time)
    false))
(defn multiday [s]
  (let [[date-str time] (.split s " ")
        [month date year] (.split date-str "/")
        [hour minute] (.split time ":")
        month (-> month Long/parseLong dec)]
    (format "new Date(%s, %s, %s, %s, %s)" year month date hour minute)))
(defn multidays [s]
  (->> (.split s ",")
       (map multiday)
       (string/join ", ")))

(defn multiday-book [req
                     locked?
                     room-id
                     start-date
                     hour
                     minute
                     tz
                     tf?
                     multiday]
  (let [[{:keys [previous-title min-time min-date min-js teardown? now?] :as previous}
         {:keys [next-title max-time max-date max-js setup?] :as next}]
        (room/booking-limits-multiday locked? room-id start-date hour minute tz)
        default-time (time/format-time hour minute)
        previous-title (not-empty (str previous-title (if teardown? " teardown" "")))
        next-title (not-empty (str next-title (if setup? " setup" "")))
        title (if now?
                (cond
                 (and (> (count locked?) 1) next-title)
                 (format-js$ "Availability from now until ${max-date}")
                 next-title
                 (format-js$ "Availability from now until '${next-title}' on ${max-date} at ${max-time}")
                 :else
                 "Availability from now onwards")
                ;; else previous title must exist
                (cond
                 (and (> (count locked?) 1) next-title)
                 (format-js$ "Timeslot from ${min-date} until ${max-date}")
                 next-title
                 (format-js$ "Availability from '${previous-title}' ending on ${min-date} at ${min-time} until '${next-title}' on ${max-date} at ${max-time}")
                 :else
                 (format-js$ "Availability from '${previous-title}' ending on ${min-date} at ${min-time} onwards")))
        locked-details (room/get-locked locked?)]
    [:div.p-2
     (if (= min-date max-date)
       (format "Multiday bookings not available on %s, choose a different day." min-date)
       [:div.flex
        ;; room locks
        [:div.flex.flex-col.border-r
         (when (util/plural? locked-details)
               [:div.mt-1.text-lg.text-center "Multibooking"])
         (for [{:keys [id description]} locked-details
               :when description]
           [:div.mt-1.flex.justify-between.items-center.mr-2
            [:span.mr-1 description]
            (services/service-modal req id)])]
        ;; time selectors
        [:div.flex.flex-col.p-2
         [:div.flex.mb-2
          [:span.text-gray-800.mr-2 title]
          (components/qtip "Close this modal to select a different time slot.")]
         [:div.flex
          [:input#multiday-input.booking-info {:type "hidden" :name "multiday"}]
          [:script
           (util/format-js "multiday = new Datepicker('#multiday-input', %s);"
                           {:min (js-datetime min-js)
                            :max (js-datetime max-js)
                            :inline true
                            :ranged true
                            :time true
                            :defaultTime [hour minute]
                            :twentyFourHours tf?
                            :serialize "serializeMultiday"
                            :onChange "onChangeMultiday"})
           (when (not-empty multiday)
                 (format "multiday.selectRange(%s);" (multidays multiday)))]
          [:div.p-2
           [:div.mb-2.multi-message "Drag on the dates to select a range."]
           [:div#time-cross.multi-message.hidden.mb-2
            (components/warning-div (format "Select multiple days."))]
           [:div
            [:button {:id "proceed-multiple"
                      :type "button"
                      :class "bg-kkr-purple disabled:bg-gray-400 py-1.5 px-3 rounded-lg text-white"
                      :hx-get "booking-details"
                      :disabled true
                      :hx-include ".booking-info"
                      :si-set [:hour :minute :hour2 :minute2 :multiday]
                      :si-set-class "booking-info"
                      :hx-target "#modal"}
             "Proceed"]]
           ]]]])]))
