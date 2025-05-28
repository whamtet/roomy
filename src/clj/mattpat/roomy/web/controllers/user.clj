(ns mattpat.roomy.web.controllers.user
    (:require
      [simpleui.response :as response]))

(defn assoc-session [{:keys [session]} & assocs]
  (assoc
   response/hx-refresh
   :session (apply assoc session assocs)))

(defn assoc-session-direct [{:keys [session]} & assocs]
  (assoc
   (response/redirect "/")
   :session (apply assoc session assocs)))

(defn assoc-session-home [{:keys [session]} & assocs]
  (assoc
   (response/hx-redirect "/")
   :session (apply assoc session assocs)))

(defn apply-session [{:keys [session]} f & args]
  (assoc
    response/hx-refresh
   :session (apply f session args)))

(defn new-session [req tz]
  (assoc-session req :tz tz :locked? {}))

(defn dev-session [req]
  (assoc-session-direct req :user_name "Devy"))

(defn assoc-config [req tf? week-start]
  (assoc-session-home req :tf tf? :week-start week-start))

(defn finish-booking [req]
  (apply-session req #(-> % (dissoc :title :details) (assoc :locked? {}))))
