(ns mattpat.roomy.web.views.table
    (:require
      [mattpat.roomy.util :as util]))

(defn- th [x]
  [:th {:class "px-3 py-3.5 text-left text-sm font-semibold text-gray-900"} x])

(defn- td1 [s]
  [:td {:class "whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900"} s])

(defn- td2 [s]
  [:td {:class "whitespace-nowrap px-3 py-4 text-sm text-gray-500"} s])

;; keep public
(defn tr [row]
  [:tr (util/map1 td1 td2 row)])

(defn tr-public [attr & cols]
  [:tr attr
   (util/map1 td1 td2 cols)])

(defn table-public [head rows]
  [:table {:class "min-w-full divide-y divide-gray-300"}
   [:thead
    [:tr (map th head)]]
   [:tbody {:class "divide-y divide-gray-200"}
    rows]])

(defn table [head rows]
  (table-public head (map tr rows)))
