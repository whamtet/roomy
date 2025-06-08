(ns mattpat.roomy.web.views.room-search.booking-details
    (:require
      [mattpat.roomy.time :as time]
      [mattpat.roomy.util :as util :refer [mk]]
      [mattpat.roomy.web.htmx :refer [defcomponent]]
      [mattpat.roomy.web.controllers.room :as room]
      [mattpat.roomy.web.controllers.user :as user]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.room-search.repeat :as repeat]))

[:div {:class "w-3/4"}]
(defn- modal [& args]
  (components/modal "w-3/4" [:div.p-2 args]))

(defmacro parse-time [sym]
  `(time/->zoned-date-time ~sym (:tz ~'session)))
(defn- parse-limit-type [limit-type]
  (if (and (string? limit-type)
           (not= "date" limit-type)
           (not= "forever" limit-type)
           (-> limit-type .trim not-empty))
    (Long/parseLong limit-type)
    limit-type))
;; endpoint only component
(defcomponent ^:endpoint booking-save [req title details command
                                       room-id t1 t2
                                       ;; repeat info
                                       repeat-stored ^:long interval limit-type end-date
                                       ^:json days
                                       date-pattern]
  (case command
        "title" (assoc session :title title)
        "details" (assoc session :details details)
        (do (room/insert-booking session
                                 (room/assoc-services locked? room-id)
                                 (parse-time t1) (parse-time t2)
                                 title details
                                 (mk :repeat-type repeat-stored
                                     interval
                                     :limit-type (parse-limit-type limit-type)
                                     end-date days date-pattern))
          (user/finish-booking req))))

(defcomponent ^:endpoint booking-details [req
                                          ^:long tab-index
                                          start-date
                                          ^:long hour
                                          ^:long minute
                                          ^:long hour2
                                          ^:long minute2
                                          multiday]
  booking-save
  (let [[t1 t2] (case tab-index
                      0 [(format "%s %02d:%02d" start-date hour minute)
                         (format "%s %02d:%02d" start-date hour2 minute2)]
                      1 (.split multiday ","))
        duration-disp
        (case tab-index
              0 (format "%s from %02d:%02d to %02d:%02d" start-date hour minute hour2 minute2)
              1 (format "From %s to %s" t1 t2))]
    (modal
     [:div {:hx-get "booking-modal"
            :hx-include ".booking-info"
            :si-clear [:hour :minute :hour2 :minute2 :multiday]
            :hx-target "#modal"}
      (components/button "Back")]
     [:form {:id "booking-save"
             :hx-post "booking-save"
             :hx-include ".booking-info"}
      (components/hiddens
        "t1" t1
        "t2" t2)
      [:input {:class "mt-2 w-full p-1.5 border rounded-md text-lg"
               :name "title"
               :hx-post "booking-save:title"
               :hx-trigger "keyup changed delay:0.5s"
               :value (:title session)
               :required true
               :placeholder "Add a title"}]
      [:div.p-2.flex.items-center
       [:div.mr-2.text-lg duration-disp]
       (repeat/repeat-select req)]
      [:textarea {:class "w-full p-1.5 border rounded-md text-lg"
                  :name "details"
                  :id "booking-details"
                  :hx-post "booking-save:details"
                  :hx-trigger "keyup changed delay:0.5s"
                  :placeholder "Details..."
                  :rows 6}
       (:details session)]
      (components/submit-hidden "book-submit")
      [:div {:onclick "submitBooking()"} (components/button "Book")]
      ])))
