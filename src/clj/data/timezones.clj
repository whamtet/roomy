(ns data.timezones
  (:require
    [mattpat.roomy.util :as util])
  (:import
    (java.time
      ZoneId
      Instant)
    java.util.Locale
    java.time.format.TextStyle))

(def now (Instant/now))
(defn offset [id] (-> id ZoneId/of .getRules (.getOffset now)))

(defn display-name [id]
  (when-let [city (second (re-find #"/(.+)" id))]
    (format "%s (%s %s)"
            (-> city (.replace "_" " "))
            (.getDisplayName (ZoneId/of id) TextStyle/FULL Locale/US)
            (offset id))))

(defn short-name [id]
  (.getDisplayName (ZoneId/of id) TextStyle/SHORT_STANDALONE Locale/US))

(defn tz [id]
  (let [display (display-name id)]
    {:id id
     :display display
     :short (short-name id)
     :q (.toLowerCase display)}))

(def sorted-zones
  (->> (ZoneId/getAvailableZoneIds)
       (remove #(or (.startsWith % "Etc") (.startsWith % "SystemV") (.startsWith % "Antarctica")))
       (util/distinct-by display-name)
       (sort-by offset)
       (map tz)))

(def default-zones
  (map tz
       ["Asia/Hong_Kong"
        "Asia/Dubai"
        "Europe/London"
        "America/New_York"]))

(defn search [q]
  (let [q (and q (-> q .trim .toLowerCase))]
    (if (empty? q)
      default-zones
      (->> sorted-zones
           (filter #(-> % :q (.contains q)))
           (take 5)))))

(def id->disp
  (util/zipmap-by :id :display sorted-zones))
(def id->short
  (util/zipmap-by :id :short sorted-zones))
