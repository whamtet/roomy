(ns mattpat.roomy.web.views.room-search.floor-modal
    (:require
      [mattpat.roomy.env :refer [src-floor]]
      [mattpat.roomy.util :as util]
      [mattpat.roomy.web.htmx :refer [defcomponent]]
      [mattpat.roomy.web.controllers.room :as room]
      [mattpat.roomy.web.views.components :as components]))

[:div {:class "w-3/4"}]
(defn- modal [body]
  (components/modal-scroll "w-3/4" body))

(defn- style [x y]
  {:left (str x "px")
   :top (str y "px")
   :width "18px"
   :height "18px"})

(defcomponent ^:endpoint floor-modal [req
                                      ^:long floor-image]
  (let [[{:keys [width height]} :as rooms] (room/floor->rooms floor-image)]
    (modal
     [:div.p2 {:style {:height (-> height (+ 10) (str "px"))}}
      [:div {:class "relative mx-auto"
             :style {:width (str width "px")}}
       [:img {:class "absolute left-0 top-0"
              :src (src-floor floor-image)}]
       (for [{:keys [id x y description]} rooms]
         [:div {:class "absolute border cursor-pointer"
                :style (style x y)
                :hx-post "lock"
                :hx-vals {:room-id id}
                :hx-target "#room-book"
                :hx-include ".lock-info"}
          description])]])))
