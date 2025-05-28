(ns mattpat.roomy.web.views.task-pane
  (:require
    [java-time.api :as jt]
    [simpleui.core :as simpleui]
    [mattpat.roomy.web.htmx :refer [defcomponent]]))

(defn- set-start [hour minute]
  (format "setStart('%02d', '%02d')" hour minute))

(defcomponent ^:endpoint task-pane [req ^:json data]
              (let [db (:db req)
                    available-times []#_ (check/find-alternative-times db data)]
                [:div.p-2
                 (if (seq available-times)
                   [:div
                    [:div.text-xl "Select a time"]
                    [:div.p-6.grid.grid-cols-2
                     (for [[start _] available-times
                           :let [hour (.getHour start)
                                 minute (.getMinute start)]]
                       [:div
                        [:a {:class   "text-blue-600"
                             :href    "#"
                             :onclick (set-start hour minute)}
                         (format "%02d:%02d" hour minute)]])]]
                   [:div.text-xl "No clashes found"])]))

(defn ui-routes [_]
  (simpleui/make-routes-simple
   "https://roomy.simpleui.io"
   []
   task-pane))
