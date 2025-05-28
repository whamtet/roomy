(ns mattpat.roomy.web.views.confirm
    (:require
      [clojure.string :as string]
      [simpleui.core :as simpleui]
      [mattpat.roomy.time :as time]
      [mattpat.roomy.web.htmx :refer [page-htmx defcomponent]]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.icons :as icons]
      [mattpat.roomy.web.views.sidebar :as sidebar]
      [mattpat.roomy.web.views.table :as table]))

(def now (time/now-local))

(defn- alter-time [offsets]
  (->> offsets
       (partition 2)
       (reduce
        (fn [t [type offset]]
          ((case type
                 :day time/+day
                 :hour time/+hour
                 :min time/+min) t offset))
        now)))
(defn- repeat-pair [& offsets]
  {:start (alter-time (drop-last 2 offsets))
   :end (alter-time offsets)})

(defn- make-repeats [offset]
  (->> [:day 5]
       alter-time
       time/range-day
       (map
        (fn [t]
          {:start t :end (time/+hour t 1)}))
       (drop offset)
       (take 10)))

(def sample-data
  [{:subject "CEO Meeting"
    :repeats [(repeat-pair :hour 1)]
    :rooms ["Room A" "Room B"]}
   {:subject "Grease Client"
    :repeats [(repeat-pair :day 3 :hour -2 :hour 1)]
    :rooms ["Ensuite"]}
   {:subject "Eternal meeting from Hell"
    :repeats (make-repeats 0)
    :repeat-msg "Repeats daily"
    :rooms ["Hong Kong A" "Hong Kong B"]}
   {:subject "Final Meeting"
    :repeats [(repeat-pair :day 10 :hour 3 :hour 2)]
    :rooms ["Annex"]}])

(defn- respond-button []
  [:span {:onclick "alert('todo')"}
   (components/button "Respond")])

(defn- extra-rows [offset rooms]
  (->> offset
       make-repeats
       (map
        (fn [{:keys [start end]}]
          (table/tr
            [""
             (time/format-full start)
             (time/format-full end)
             rooms
             (respond-button)])))))

(defcomponent ^:endpoint confirm [req ^:long offset rooms]
  (if offset
    (extra-rows offset rooms)
    (sidebar/sidebar
     (:uri req)
     [:div
      [:h2.mb-6 "Pending Meetings"]
      (table/table
       ["Subject" "Start" "End" "Rooms"]
       (for [{:keys [subject repeats repeat-msg rooms]} sample-data]
         (let [[{:keys [start end]}] repeats
               rooms (string/join ", " rooms)]
           (if repeat-msg
             [[:span.flex.items-center
               subject
               [:span {:class "ml-2 cursor-pointer"
                       :hx-get "confirm"
                       :hx-swap "afterend"
                       :hx-target "closest tr"
                       :hx-vals {:subject subject :rooms rooms :offset 1}
                       :hx-headers {:skip-oob true}} icons/plus-circle]]
              (time/format-full start)
              (str (time/format-full end) "; " repeat-msg)
              rooms
              (respond-button)]
             [subject
              (time/format-full start)
              (time/format-full end)
              rooms
              (respond-button)]))))])))

(defn ui-routes [_]
  (simpleui/make-routes
   ""
   (fn [req]
     (page-htmx
      {:hyperscript? true}
      (confirm req)))))
