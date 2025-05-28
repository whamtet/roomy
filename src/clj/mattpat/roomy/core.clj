(ns mattpat.roomy.core
  (:require
    [clojure.tools.logging :as log]
    [integrant.core :as ig]
    [mattpat.roomy.config :as config]
    [mattpat.roomy.env :refer [defaults]]

    ;; Edges
    [kit.edge.server.undertow]
    [mattpat.roomy.web.handler]
    ;; Routes
    [mattpat.roomy.web.routes.api]

    simpleui.config

    [mattpat.roomy.web.routes.ui])
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread ex]
      (log/error {:what :uncaught-exception
                  :exception ex
                  :where (str "Uncaught exception on" (.getName thread))}))))

(defonce system (atom nil))

(defn stop-app []
  ((or (:stop defaults) (fn [])))
  (some-> (deref system) (ig/halt!)))

(defn start-app [& [params]]
  ((or (:start params) (:start defaults) (fn [])))
  (->> (config/system-config (or (:opts params) (:opts defaults) {}))
       (ig/prep)
       (ig/init)
       (reset! system))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& _]
  (start-app))

;; last, and not least!
(simpleui.config/set-render-oob true)
(simpleui.config/set-render-safe false)
