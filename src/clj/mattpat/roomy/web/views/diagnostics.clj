(ns mattpat.roomy.web.views.diagnostics
    (:require
      [simpleui.core :as simpleui]
      [simpleui.response :as response]
      [mattpat.roomy.web.htmx :refer [page-htmx defcomponent]]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.icons :as icons]
      [mattpat.roomy.web.views.sidebar :as sidebar]
      [mattpat.roomy.web.views.table :as table]))

(def options ["Kafka subscriptions" "Unhandled events"])

(defn- tab-lg [selected option]
  [:option {:selected (= selected option)} option])
(defn- tab-sm [selected option]
  (if (= selected option)
    [:a {:class "whitespace-nowrap border-b-2 border-indigo-500 px-1 py-4 text-sm font-medium text-indigo-600"
         :href "#"} option]
    [:a {:class "whitespace-nowrap border-b-2 border-transparent px-1 py-4 text-sm font-medium text-gray-500 hover:border-gray-300 hover:text-gray-700"
         :href "#"
         :hx-get "diagnostics"
         :hx-vals {:selected option}} option]))

(defn tabs-ui [selected]
  [:div.mb-3
   [:div {:class "sm:hidden"}
    [:select {:class "block w-full rounded-md border-gray-300 py-2 pl-3 pr-10 text-base focus:border-indigo-500 focus:outline-none focus:ring-indigo-500 sm:text-sm"}
     (map #(tab-lg selected %) options)]]
   [:div {:class "hidden sm:block"}
    [:div {:class "border-b border-gray-200"}
     [:nav {:class "-mb-px flex space-x-8"}
      (map #(tab-sm selected %) options)]]]])

(def inactive "🔴")
(def active "🟢")

(defcomponent ^:endpoint subscriptions [req subscription-id]
  (if top-level?
    (do
      ;(ms-graph/delete-subscription-safe graph subscription-id)
      response/hx-refresh)
    (table/table
     ["Expiration" "Resource" "Current"]
     #_
     (for [{:keys [expirationDateTime resource current? id]} (event-subscriber/all-subscriptions node graph)]
       [[:span.flex.items-center
         [:a.mr-2 {:href "#"
                   :hx-delete "subscriptions"
                   :hx-confirm "Really delete?"
                   :hx-vals {:subscription-id id}} icons/trash]
         expirationDateTime]
        (-> resource (.split "&") first)
        (if current? active inactive)]))))

(defcomponent ^:endpoint diagnostics [req selected]
  [:div {:hx-target "this"}
   (tabs-ui selected)
   (case selected
         "Kafka subscriptions" (subscriptions req)
         nil)])

(defn ui-routes [{:keys [graph node]}]
  (simpleui/make-routes
   ""
   [graph node]
   (fn [req]
     (page-htmx
      {}
      (sidebar/sidebar
       (:uri req)
       (diagnostics
        (assoc req :graph graph :node node)
        (first options)))))))
