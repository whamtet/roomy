(ns mattpat.roomy.web.views.home
    (:require
      [data.timezones :as timezones]
      [simpleui.core :as simpleui]
      [mattpat.roomy.env :refer [dev?]]
      [mattpat.roomy.web.htmx :refer [page-htmx defcomponent]]
      [mattpat.roomy.web.controllers.user :as user]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.dropdown :as dropdown]
      [mattpat.roomy.web.views.icons :as icons]
      [mattpat.roomy.web.views.room-search :as room-search]
      [mattpat.roomy.web.views.room-search.services :as services]
      [mattpat.roomy.web.views.sidebar :as sidebar]
      [mattpat.roomy.web.views.tz :as tz]))

(defn- logged-in? [req]
  (-> req :session :tz boolean))

(defn main-dropdown [user_name]
  [:div.absolute.top-1.right-1.flex.items-center
   (dropdown/dropdown
    (str "Welcome " user_name)
    [[:a {:href "/config"}
      [:div.p-2 "Config"]]
     [:div.p-2 {:class "cursor-pointer"
                :hx-post "/api/logout"}
      "Logout"]])])

(defcomponent panel [req]
  (sidebar/sidebar
   (:uri req)
   ;; body
   [:div.flex
    ;; left panel
    [:div.p-2.w-96
      [:h4.mb-1 "Date"]
      ;; datepicker
      [:div.inline-flex.border.rounded-sm.p-1
       [:input#datepicker.room-search.room-search2.lock-info {:name "start-date"}]
       [:span.cursor-pointer {:onclick "$('#datepicker').focus()"}
        icons/calendar]]
     [:hr.border-t.my-3]
     [:p#tz-disp.my-2 (timezones/id->disp tz)]
     (tz/search-modal req)
     [:hr.border-t.my-3]
     (services/service-panel req)]
    ;; center panel
    [:div.w-full.p-2
     (room-search/room-search req)
     ]]))

(defcomponent ^:endpoint home [req ^:trim new-tz]
  (cond

   new-tz
   (user/new-session req new-tz)

   (and user_name tz)
   (panel req)

   user_name
   [:form {:hx-post "home"}
    [:h1.text-center.mt-8.mb-4 "Roomy"]
    [:h4.text-center.mb-4
     "Roomy is a demo "
     [:a.text-clj-blue {:href "https://simpleui.io"} "SimpleUI"]
     " calendar webapp."]
    [:div.flex.justify-center
     [:div {:class "w-1/2 border rounded-lg p-2"}
      [:div.my-1.p-1 "Timezone"]
      (tz/search req)
      (if (simpleui/post? req)
        [:div#warning.mt-1.mb-3
         (components/warning "Select Timezone")]
        [:div#warning.hidden])
      [:div.mt-1
       (components/submit "Begin")]]]]

   :else
   [:div
    [:h1.text-center.mt-8.mb-16 "Roomy"]
    [:div.flex.justify-center
     [:a {:href "/oauth"}
      [:div {:class "ms-btn flex p-2"}
       [:img {:class "mr-2"
              :src "/ms.svg"}]
       "Sign in with Microsoft"]]]]))

(defn ui-routes [_]
  (simpleui/make-routes
   ""
   (fn [req]
     (let [logged-in? (logged-in? req)]
       (page-htmx
        {:datepicker? logged-in?
         :hyperscript? logged-in?
         :si-stack (when logged-in? ["room-id" "start-date" "tab-index"
                                     "hour" "minute" "hour2" "minute2"
                                     "multiday"
                                     ;; repeat types
                                     "interval" "limit-type" "end-date" "days" "date-pattern" "repeat-stored"
                                     ])
         :js (when logged-in? ["/multiday.js" "/booking.js" "/contenteditable.js"])}
        (home req))))))
