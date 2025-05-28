(ns mattpat.roomy.web.views.sidebar
    (:require
      [clojure.java.io :as io]
      [mattpat.roomy.util :as util :refer [defm-dev]]
      [mattpat.roomy.web.views.icons :as icons]))

(defm-dev fragments []
  (-> "templates/sidebar.html" io/resource slurp (.split "%s")))

(def options [{:href "/"
               :text "Home"
               :icon icons/squares-plus}
              {:href "/calendar/"
               :text "Calendar"
               :icon icons/calendar}
              {:href "/confirm/"
               :text "Review pending"
               :icon icons/flag
               :yellow? true}
              {:href "/trash/"
               :text "Trash"
               :icon icons/trash}
              {:href "/diagnostics/"
               :text "Fix"
               :icon icons/fix}
              {:href "/builder/"
               :text "UI Builder"
               :icon icons/shop}
              {:href "/config/"
               :text "Settings"
               :icon icons/gears}
              {:href "/api/logout"
               :text "Logout"
               :icon icons/logout}])

(defn- sidebar-mobile [current-url {:keys [href text icon yellow?]}]
  [:li
   (cond
    (and current-url (= current-url href))
    [:a {:href href :class "group flex gap-x-3 rounded-md p-2 text-sm font-semibold leading-6 text-white"}
     icon
     text]
    yellow?
    [:a {:href href :class "group flex gap-x-3 rounded-md p-2 text-sm font-semibold leading-6 text-kkr-yellow hover:text-white"}
     icon
     text]
    :else
    [:a {:href href :class "group flex gap-x-3 rounded-md p-2 text-sm font-semibold leading-6 text-gray-400 hover:text-white"}
     icon
     text])])

(defn- sidebar-desktop [current-url {:keys [href text icon yellow?]}]
  [:li
   (cond
    (and current-url (= current-url href))
    [:a {:href href :class "group flex gap-x-3 rounded-md bg-kkr-tertiary p-3 text-sm font-semibold leading-6 text-white"}
     icon]
    yellow?
    [:a {:href href :class "group flex gap-x-3 rounded-md p-3 text-sm font-semibold leading-6 text-kkr-yellow hover:bg-kkr-tertiary hover:text-white"}
     icon]
    :else
    [:a {:href href :class "group flex gap-x-3 rounded-md p-3 text-sm font-semibold leading-6 text-gray-400 hover:bg-kkr-tertiary hover:text-white"}
     icon])])

(defn sidebar [current-url & body]
  (util/interleave-all
   (fragments)
   [(map #(sidebar-mobile current-url %) options)
    (map #(sidebar-desktop current-url %) options)
    body]))
