(ns mattpat.roomy.web.views.room-search.booking-modal
    (:require
      [mattpat.roomy.time :as time]
      [mattpat.roomy.util :as util :refer [format-js$ mk]]
      [mattpat.roomy.web.controllers.room :as room]
      [mattpat.roomy.web.htmx :refer [defcomponent]]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.room-search.booking-details :as booking-details]
      [mattpat.roomy.web.views.room-search.booking-multiday :as booking-multiday]
      [mattpat.roomy.web.views.room-search.services :as services]
      [mattpat.roomy.web.views.room-search.time-select :as time-select]
      [mattpat.roomy.web.views.tabs :as tabs]))

[:div {:class "w-3/4"}]
(defn- modal [tab-index & args]
  (components/modal "w-3/4" [:div.p-2 (tabs/tabs tab-index args)]))

(defn- today-book [req
                   locked?
                   room-id
                   start-date
                   hour
                   minute
                   hour2
                   minute2
                   tz
                   tf?]
  (let [[{:keys [previous-title min-time teardown? now?] :as previous}
         {:keys [next-title max-time setup?] :as next}]
        (room/booking-limits locked? room-id start-date hour minute tz)
        default-time (time/format-time hour minute)
        previous-title (not-empty (str previous-title (if teardown? " teardown" "")))
        next-title (not-empty (str next-title (if setup? " setup" "")))
        title (if now?
                (cond
                 (and (> (count locked?) 1) next-title)
                 (format-js$ "Timeslot from now until ${max-time}")
                 next-title
                 (format-js$ "Availability from now until '${next-title}' at ${max-time}")
                 :else
                 "Availability from now until midnight")
                (cond
                 (and (> (count locked?) 1) (or previous-title next-title))
                 (format-js$ "Timeslot from ${min-time} to ${max-time}")
                 (and previous-title next-title)
                 (format-js$ "Availability between '${previous-title}' ending at ${min-time} and '${next-title}' at ${max-time}")
                 previous-title
                 (format-js$ "Availability between '${previous-title}' ending at ${min-time} and midnight")
                 next-title
                 (format-js$ "Availability between midnight and '${next-title}' at ${max-time}")
                 :else
                 (format-js$ "Availability on ${start-date} (all day)")))
        locked-details (room/get-locked locked?)]
    [:div.p-2
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
      [:div.p-2
       ;; title
       [:div.flex.mb-2
        [:span.text-gray-800.mr-2 title]
        (components/qtip "Close this modal to select a different timeslot.")]
       [:div.flex
        ;; time selectors
        [:div.pr-4.mr-4.border-r
         [:div.flex.items-center
          [:span.w-16.text-lg.mr-4 "From:"]
          (time-select/time-input "" previous hour minute next tf?)]
         [:div.flex.items-center.mt-2
          [:span.w-16.text-lg.mr-4 "To:"]
          (time-select/time-input "2" previous hour2 minute2 next tf?)]]
        ;; proceed
        [:div.relative
         [:span.absolute.bottom-0
          [:button {:id "proceed-single"
                    :type "button"
                    :class "bg-kkr-purple disabled:bg-gray-400 py-1.5 px-3 rounded-lg text-white"
                    :hx-get "booking-details"
                    :disabled (= [hour minute] [hour2 minute2])
                    :hx-include ".booking-info"
                    :si-set [:hour :minute :hour2 :minute2]
                    :si-set-class "booking-info"
                    :hx-target "#modal"}
           "Proceed"]]]
        ]]]]))

(defcomponent ^:endpoint booking-modal [req
                                        start-date
                                        ^:long hour
                                        ^:long minute
                                        ^:long hour2
                                        ^:long minute2
                                        ^:long tab-index
                                        room-id
                                        multiday]
  services/service-modal ;; because its referenced within an ordinary function
  booking-details/booking-details
  (let [locked? (room/assoc-services locked? room-id)
        hour2 (or hour2 hour)
        minute2 (or minute2 minute)]
    (modal
     tab-index
     (str "Book " start-date)
     (today-book req locked? room-id start-date hour minute hour2 minute2 tz tf)
     "Multiday Booking"
     (booking-multiday/multiday-book
      req
      locked?
      room-id
      start-date
      hour
      minute
      tz
      tf
      multiday))))
