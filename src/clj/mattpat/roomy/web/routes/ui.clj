(ns mattpat.roomy.web.routes.ui
  (:require
   [mattpat.roomy.web.middleware.cors :as cors]
   [mattpat.roomy.web.middleware.exception :as exception]
   [mattpat.roomy.web.middleware.formats :as formats]
   [mattpat.roomy.web.views.config :as config]
   [mattpat.roomy.web.views.calendar-page :as calendar-page]
   [mattpat.roomy.web.views.home :as home]
   [mattpat.roomy.web.views.task-pane :as task-pane]
   [integrant.core :as ig]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]))

(defn route-data [cors?]
  {:muuntaja   formats/instance
   :middleware
   (if cors?
     [cors/wrap-cors
      parameters/parameters-middleware
      muuntaja/format-response-middleware
      exception/wrap-exception]
     [;; Default middleware for ui
     ;; query-params & form-params
       parameters/parameters-middleware
       ;; encoding response body
       muuntaja/format-response-middleware
       ;; exception handling
       exception/wrap-exception])})

(derive :reitit.routes/ui :reitit/routes)

(defmethod ig/init-key :reitit.routes/ui
  [_ opts]
  [["" (route-data false) (home/ui-routes opts)]
   ["/calendar" (route-data false) (calendar-page/ui-routes opts)]
   ["/config" (route-data false) (config/ui-routes opts)]
   ["/taskpane" (route-data true) (task-pane/ui-routes opts)]])
