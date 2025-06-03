(ns mattpat.roomy.web.views.room-search.services
    (:require
      [mattpat.roomy.util :as util :refer [mk]]
      [mattpat.roomy.web.controllers.builder :as builder]
      [mattpat.roomy.web.controllers.room :as room]
      [mattpat.roomy.web.htmx :refer [defcomponent]]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.icons :as icons]
      [mattpat.roomy.web.views.room-search.resource-details :as resource-details]
      [simpleui.core :as simpleui]
      [simpleui.rt :as rt]))

(defmacro when-top-level [new-session body]
  `(if ~'top-level?
    {:body ~body :session ~new-session}
    ~body))

(defn- field-length [{:keys [type half-width]}]
  (if (or (= "checkbox" type) half-width) 0.5 1))
(defcomponent ^:endpoint resource [req
                                   ;; update vals
                                   ^:boolean select
                                   ^:long quantity
                                   ;; from hx-vals on 'Services' button
                                   room-id-service
                                   ^:long resource-id
                                   description
                                   ^:boolean allow-instructions
                                   ^:long min-quantity
                                   ^:long quantity-available]
  resource-details/resource-details
  (let [quantity (if top-level?
                   (if quantity
                     (util/bind (max min-quantity 1) quantity (max quantity-available 1))
                     (if select
                       (util/bind 1 min-quantity (max quantity-available 1))
                       0 #_deselect))
                   (get-in locked? [room-id-service :resources resource-id :quantity] 0))
        checked? (pos? quantity)
        fields (builder/fields resource-id)
        field-size (->> fields (map field-length) (apply +))
        block-inline? (builder/block-inline? resource-id)
        inline? (and checked? (not block-inline?) (<= field-size 1))
        many-fields? (> field-size 1)]
    (when-top-level
     ;; add this to response via macro
     (if (pos? quantity)
       (assoc-in session [:locked? room-id-service :resources resource-id :quantity] quantity)
       (update session :locked? util/dissoc-in [room-id-service :resources resource-id]))
     (if inline?
       [:div {:class "w-full"
              :hx-target "this"}
        [:div {:class "flex items-center"}
         [:input {:class "m-2"
                  :type "checkbox"
                  :name "select"
                  :hx-post "resource"
                  :checked checked?
                  :hx-vals (mk room-id-service resource-id description allow-instructions
                               min-quantity quantity-available)}]
         [:span.m-2 description]]
        (simpleui/apply-component resource-details/single-resource req fields room-id-service resource-id)]
       [:div {:class "flex items-center w-1/2"
              :hx-target "this"}
        [:input {:class "m-2"
                 :type "checkbox"
                 :name "select"
                 :hx-post "resource"
                 :checked checked?
                 :hx-vals (mk room-id-service resource-id description allow-instructions
                              min-quantity quantity-available)}]
        [:span.m-2 description]
        (when (and checked? (or many-fields? block-inline?))
              [:span {:hx-get "resource-details"
                      :hx-vals (mk description resource-id room-id-service)
                      :hx-target "#modal"}
               (components/button "More...")])]))))

[:div {:class "w-3/4"}]
(defcomponent ^:endpoint service-modal [req
                                        room-id-service]
  (if top-level?
    (components/modal-scroll "w-3/4"
     [:div.p-2
      [:h2.m-2 "Services for " (room/id->room-title room-id-service)]
      [:div {:hx-get "booking-modal"
             :hx-include ".booking-info"
             :si-clear [:hour :minute :hour2 :minute2]
             :hx-target "#modal"}
       (components/button "Back")]
      (for [[{:keys [description]} resources] (room/room-id->services room-id-service)
            :when (not-empty resources)]
        [:div.border-t.mt-2
         [:h4.my-2 description]
         [:div {:class "flex flex-wrap"}
          (rt/map-indexed resource req resources)]])])
    [:span {:hx-get "service-modal"
            :hx-include ".booking-info"
            :hx-vals {:room-id-service room-id-service}
            :si-set [:hour :minute :hour2 :minute2]
            :si-set-class "booking-info"
            :hx-target "#modal"}
     (components/button "Services...")]))

;; this is on the main search page

(defcomponent service-panel [req results]
  service-modal
  [:div#service-panel
   ;; this is drawn independently of search results and updated as necessary
   (for [{:keys [id description first-name
                 setup-time teardown-time]} (or results (room/get-locked locked?))]
     [:div.mb-2
      ;; top row
      [:div.flex.items-center
       [:span {:class "text-gray-500 cursor-pointer mr-1"
               :hx-delete "lock:remove"
               :hx-vals {:room-id id}
               :hx-target "#room-book"
               :hx-include ".lock-info"}
        icons/minus-circle]
       [:span.mr-1 (or description first-name)]]
      #_
      (when setup-time
            [:div.flex.items-center.my-2
             [:span.mr-1 "Setup"]
             [:input {:class "p-1 border rounded-md mr-1 w-14"
                      :type "number"
                      :name "new-setup"
                      :value setup-time
                      :min 0
                      :max 60
                      :hx-post "lock:new-setup"
                      :hx-vals {:room-id id}
                      :hx-target "#room-book"
                      :hx-include ".lock-info"}]
             [:span.mr-1 "Teardown"]
             [:input {:class "p-1 border rounded-md w-14"
                      :type "number"
                      :name "new-teardown"
                      :value teardown-time
                      :min 0
                      :max 60
                      :hx-post "lock:new-teardown"
                      :hx-vals {:room-id id}
                      :hx-target "#room-book"
                      :hx-include ".lock-info"}]])
      ])])
