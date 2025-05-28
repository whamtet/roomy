(ns mattpat.roomy.web.views.config
    (:require
      [simpleui.core :as simpleui]
      [mattpat.roomy.env :refer [dev?]]
      [mattpat.roomy.web.controllers.user :as user]
      [mattpat.roomy.web.htmx :refer [page-htmx defcomponent]]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.home :as home]
      [mattpat.roomy.web.views.sidebar :as sidebar]))

(defcomponent ^:endpoint config [req disp-type week-start]
  (cond
   disp-type
   (user/assoc-config req (= "24-hour" disp-type) week-start)
   :else
   (sidebar/sidebar
    (:uri req)
    [:div {:class "w-2/3 mx-auto text-lg"}
     [:form {:hx-post "config"}
      [:div
       [:input {:class "mr-2"
                :type "radio"
                :name "disp-type"
                :value "12-hour"
                :checked (not tf)}] "12 Hour Display"]
      [:div
       [:input {:class "mr-2"
                :type "radio"
                :name "disp-type"
                :value "24-hour"
                :checked tf}] "24 Hour Display"]
      [:hr.border-t.my-3]
      [:h3.mb-2 "Week Start"]
      [:div
       [:input {:class "mr-2"
                :type "radio"
                :name "week-start"
                :value "Sunday"
                :checked (-> session :week-start (not= "Monday"))}] "Sunday"]
      [:div
       [:input {:class "mr-2"
                :type "radio"
                :name "week-start"
                :value "Monday"
                :checked (-> session :week-start (= "Monday"))}] "Monday"]
      [:div.my-2
       (components/submit "Save")]]])))

(defn ui-routes [_]
  (simpleui/make-routes
   ""
   (fn [req]
     (page-htmx
      {:hyperscript? true}
      (config req)))))
