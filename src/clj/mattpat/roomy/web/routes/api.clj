(ns mattpat.roomy.web.routes.api
  (:require
    [mattpat.roomy.web.controllers.health :as health]
    [mattpat.roomy.web.middleware.cors :as cors]
    [mattpat.roomy.web.middleware.exception :as exception]
    [mattpat.roomy.web.middleware.formats :as formats]
    [integrant.core :as ig]
    [reitit.coercion.malli :as malli]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [reitit.swagger :as swagger]
    [simpleui.response :as response]))

(def route-data
  {:coercion   malli/coercion
   :muuntaja   formats/instance
   :swagger    {:id ::api}
   :middleware [cors/wrap-cors
                ;; query-params & form-params
                parameters/parameters-middleware
                  ;; content-negotiation
                muuntaja/format-negotiate-middleware
                  ;; encoding response body
                muuntaja/format-response-middleware
                  ;; exception handling
                coercion/coerce-exceptions-middleware
                  ;; decoding request body
                muuntaja/format-request-middleware
                  ;; coercing response bodys
                coercion/coerce-response-middleware
                  ;; coercing request parameters
                coercion/coerce-request-middleware
                  ;; exception handling
                exception/wrap-exception]})

;; please don't delete me
(defn- clean-req [req]
  (dissoc req :reitit.core/match :reitit.core/router :muuntaja/request :muuntaja/response
          :body-schema :db))

;; Routes
(defn api-routes [{:keys [metrics checks]}]
  [["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title "mattpat.roomy API"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/session"
    (fn [{:keys [session]}]
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (pr-str session)})]
   ["/exception"
    (fn [_]
      (throw (Exception. "oh no!")))]
   ["/logout"
    (fn [_]
      (assoc (response/redirect "/") :session {}))]
   ["/health"
    {:get health/healthcheck!}]])

(derive :reitit.routes/api :reitit/routes)

(defmethod ig/init-key :reitit.routes/api
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (fn [] [base-path route-data (api-routes opts)]))
