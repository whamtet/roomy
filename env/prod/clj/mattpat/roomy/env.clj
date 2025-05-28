(ns mattpat.roomy.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init       (fn []
                   (log/info "\n-=[roomy starting]=-"))
   :start      (fn []
                   (log/info "\n-=[roomy started successfully]=-"))
   :stop       (fn []
                   (shutdown-agents)
                   (log/info "\n-=[roomy has shut down successfully]=-"))
   :middleware (fn [handler _] handler)
   :opts       {:profile :prod}})

(def dev? false)
(def prod? true)

(defn src-floor [floor-image]
  (format "/floors/floor%s.jpg" floor-image))
