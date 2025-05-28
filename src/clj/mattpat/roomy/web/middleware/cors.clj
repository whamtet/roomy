(ns mattpat.roomy.web.middleware.cors)

(def cors-headers
  {"Access-Control-Allow-Origin" "*"
   "Access-Control-Allow-Methods" "POST, GET, OPTIONS, DELETE"
   "Access-Control-Allow-Headers" "*"
   "Access-Control-Expose-Headers" "*"})

(defn wrap-cors [handler]
  (fn [req]
    (if (-> req :request-method (= :options))
      {:status 200
       :headers cors-headers
       :body ""}
      (-> req
          handler
          (update :headers merge cors-headers)))))
