(ns mattpat.roomy.web.controllers.event)

;; make some random events!
(def users ["Mary" "Jane" "Joe" "Stewart"])

(def events ["Strategy Meeting"
             "All Hands"
             "Sales"
             "Tech Audit"
             "Sales (1)"
             "Sales (2)"])

(defn- format-users [s]
  (->> users
       shuffle
       (take (count (re-seq #"%s" s)))
       (apply format s)))

(defn random-event []
  (rand-nth events))
