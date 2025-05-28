(ns mattpat.roomy.env
  (:require
    [clojure.tools.logging :as log]
    [mattpat.roomy.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[roomy starting using the development or test profile]=-"))
   :start      (fn []
                 (log/info "\n-=[roomy started successfully using the development or test profile]=-"))
   :stop       (fn []
                 (log/info "\n-=[roomy has shut down successfully]=-"))
   :middleware wrap-dev
   :opts       {:profile       :dev
                :persist-data? true}})

(def dev? true)
(def prod? false)

(defn src-floor [floor-image]
  (format "http://localhost:8000/floor%s.jpg" floor-image))
