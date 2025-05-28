(ns mattpat.roomy.web.views.calendar-page
    (:require
      [simpleui.core :as simpleui]
      [mattpat.roomy.web.htmx :refer [page-htmx defcomponent]]
      [mattpat.roomy.web.views.calendar :as calendar]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.sidebar :as sidebar]))

(defcomponent ^:endpoint calendar [req ^:long year ^:long month]
  (if year
    [:div {:hx-target "this"}
     (calendar/calendar year month tz (:week-start session "Sunday"))]
    (sidebar/sidebar
     (:uri req)
     [:div {:hx-target "this"}
      (calendar/calendar tz (:week-start session "Sunday"))])))

(defn ui-routes [_]
  (simpleui/make-routes
   ""
   (fn [req]
     (page-htmx
      {:hyperscript? true}
      (calendar req)))))
