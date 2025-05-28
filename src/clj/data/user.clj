(ns data.user
    (:require
      [mattpat.roomy.util :as util]))

(defn random-user [i]
  (prn 'user i)
  (->
   (slurp "https://randomuser.me/api/")
   util/read-str
   :results
   first
   (select-keys [:name :picture])))

(defn spit-randoms []
  (->> 1000
       range
       (mapv random-user)
       pr-str
       (spit "resources/static/users.edn")))
