(ns mattpat.roomy.web.controllers.room-static
    (:require
      [data.timezones :as timezones]
      [mattpat.roomy.util :as util]))

(def building-id->tz-id
  (util/collmap
   [352] "Europe/Warsaw",
   [21 22] "Asia/Tokyo",
   [15 14 1767 1486] "Europe/Brussels",
   [-10] "America/Boise",
   [1669] "Europe/Bucharest",
   [12] "Asia/Muscat",
   [2 17 13 1485 1978] "Europe/London",
   [6 7 2326] "US/Pacific",
   [8] "America/Chicago",
   [23 2250] "Asia/Calcutta",
   [19] "Australia/Sydney",
   [11] "Brazil/East",
   [1 315 1620 1383 1417] "America/New_York",
   [20 24 25 313] "Asia/Hong_Kong"))

(def banned-room? #{"(all)" "(other)"})

(defn- room->q [{:keys [description building floor]}]
  (.toLowerCase
    (str description " " building " " floor)))
(defn- user->q [{:keys [first-name last-name]}]
  (.toLowerCase
    (str first-name " " last-name)))

(def setup-types {})

(def building-id->categories {})

(defn- clean-resource [m]
  (when (:active m)
        (-> m
            (util/transform-in
             [:resource-id :id]
             [:category-id :categoryId]
             [:description :description]
             [:min-quantity :minQuantity]
             [:allow-instructions :hideSpecialInstructions]
             [:quantity-available :quantityAvailable])
            (update :allow-instructions not))))

(def category-id->resources {})

(defn- clean-user [i m]
  (-> m
      (util/transform-in
       [:first-name :name :first]
       [:last-name :name :last]
       [:src :picture :thumbnail])
      (assoc :id (str "user" i))
      (util/assoc-f :q user->q)))

(def directions ["North" "East" "South" "West" "SE"])
(defn- adjust-building [building]
  (if-let [x (and building (re-find #"\d" building))]
    (->> x Long/parseLong (+ -2) directions (.replace building x))
    building))

(def rooms
  (->> "static/rooms.csv"
       util/slurp-csv
       (map-indexed (fn [i [description building floor setup setup-time teardown-time capacity tz]]
                      {:id (str i)
                       :description description
                       :building (adjust-building building)
                       :floor floor
                       :setup setup
                       :setup-time (Long/parseLong setup-time)
                       :teardown-time (Long/parseLong teardown-time)
                       :capacity (Long/parseLong capacity)
                       :tz tz}))))

(def users [])

(def id->room (util/key-by :id rooms))
(def id->user (util/key-by :id users))
(def room-ids (keys id->room))

(defn random-allocation []
  (let [user-ids (shuffle (map :id users))
        init-map (->> user-ids (map list) (zipmap room-ids))]
    (->> user-ids
         (drop (count init-map))
         (reduce
          (fn [m user-id]
            (update m (rand-nth room-ids) conj user-id))
          init-map))))

(def id->bookable
  (util/key-by :id (concat rooms users)))

(defn room-id->services [room-id]
  (->> room-id
       id->room
       :building-id
       building-id->categories
       (map (fn [category]
              [category
               (-> category :id category-id->resources)]))
       (into {})))

(def all-buildings
  (->> rooms (keep :building) distinct sort))
(def all-setups
  (->> rooms (keep :setup) distinct sort))

(defn- distinct-floors [s]
  (vals
   (reduce
    (fn [m {:keys [floor floor-image]}]
      (if (m floor)
        (if (get-in m [floor :floor-image])
          m
          (assoc-in m [floor :floor-image] floor-image))
        (assoc m floor {:floor floor :floor-image floor-image})))
    {}
    s)))

(def building->floors
  (util/group-by-map
   :building
   (fn [buildings]
     (->> buildings distinct-floors (sort-by :floor)))
   rooms))

(def floor->rooms
  (util/group-by-map
    :floor-image
   (fn [rooms]
     (->> rooms (map #(select-keys % [:id :x :y :description :width :height]))))
   rooms))

(defn- search-rooms* [q]
  (if q
    (let [q (.toLowerCase q)]
      (filter #(-> % :q (.contains q)) rooms))
    rooms))
(defn- search-users [q]
  (let [q (.toLowerCase q)]
    (filter #(-> % :q (.contains q)) users)))

(defn search-rooms** [q building floor capacity setup]
  (when (or q building floor)
        (cond->> (search-rooms* q)
                 capacity (filter #(let [c (:capacity %)]
                                    (or (not (pos? c)) (<= capacity c))))
                 setup (filter #(-> % :setup (= setup)))
                 building (filter #(-> % :building (= building)))
                 floor (filter #(-> % :floor (= floor)))
                 (some-> q .trim not-empty) (util/interleave-all (search-users q))
                 )))

(defn keep-locked [locked?]
  (keep (fn [[id {:keys [setup-time]}]]
          (when setup-time (id->bookable id)))
        locked?))
(defn search-all [q building floor locked? capacity setup]
  (->> (search-rooms** q building floor capacity setup)
       (concat (keep-locked locked?))
       distinct
       (take 8)))

(defn get-locked [locked?]
  (->> locked?
       (sort-by
        (fn [[id]]
          (if (.startsWith id "user") 0 1)))
       (keep
        (fn [[id m]]
          (when (:setup-time m)
            (-> id id->bookable (merge m)))))))
