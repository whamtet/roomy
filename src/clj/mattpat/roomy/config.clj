(ns mattpat.roomy.config
  (:require
    [kit.config :as config]))

(defn system-config
  [options]
  (config/read-config
   (case (:profile options)
         :test "system-test.edn"
         "system.edn")
   options))
