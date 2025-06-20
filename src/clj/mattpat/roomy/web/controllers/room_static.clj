(ns mattpat.roomy.web.controllers.room-static
    (:require
      [mattpat.roomy.util :as util]))

(defn- room->q [description building]
  (.toLowerCase
    (str description " " building)))
(defn- user->q [{:keys [first-name last-name]}]
  (.toLowerCase
    (str first-name " " last-name)))

(defn- clean-user [i m]
  (-> m
      (util/transform-in
       [:first-name :name :first]
       [:last-name :name :last]
       [:src :picture :thumbnail])
      (assoc :id (str "user" i))
      (util/assoc-f :q user->q)))
(def rooms
  (->> "static/rooms.csv"
       util/slurp-csv
       (map-indexed (fn [i [description building floor setup setup-time teardown-time capacity tz]]
                      {:id (str i)
                       :description description
                       :building building
                       :floor (Long/parseLong floor)
                       :setup setup
                       :setup-time (Long/parseLong setup-time)
                       :teardown-time (Long/parseLong teardown-time)
                       :capacity (Long/parseLong capacity)
                       :tz tz
                       :q (room->q description building)}))))

(def resources
  (->> "static/resources.csv"
       util/slurp-csv
       (map-indexed (fn [i [description min-quantity allow-instructions quantity-available]]
                      {:resource-id i
                       :description description
                       :min-quantity (Long/parseLong min-quantity)
                       :allow-instructions (= "TRUE" allow-instructions)
                       :quantity-available (Long/parseLong quantity-available)}))))

(def users
  (map-indexed clean-user
               (util/slurp-edn "static/users.edn")))

(def id->room (util/key-by :id rooms))
(def id->user (util/key-by :id users))
(def num-room-ids (-> id->room count (* 0.5) long))
(def room-ids (->> id->room keys shuffle (take num-room-ids)))
(def random-room? (set room-ids))

(defn random-allocation
  "map of room-ids to list of user-ids"
  []
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

(def services ["Food Services"
               "IT Requirements"
               "Attendees"
               "Presentation Materials"
               "Video Conference"
               "Setup Notes"
               "Food Services"
               "Special Services"])

(util/defm room-id->services [_]
  (->> services
       shuffle
       (take (rand-int 4))
       (map (fn [service]
              [{:description service}
               (->> resources
                    shuffle
                    (take (inc (rand-int 2))))]))
       (into {})))

(def all-buildings
  (->> rooms (keep :building) distinct sort))
(def all-setups
  (->> rooms (keep :setup) distinct sort))

(def building->floors
  (util/group-by-map
   :building
   #(->> % (map :floor) distinct sort)
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
       (take 6)))

(defn get-locked [locked?]
  (->> locked?
       (sort-by
        (fn [[id]]
          (if (.startsWith id "user") 0 1)))
       (keep
        (fn [[id m]]
          (when (:setup-time m)
            (-> id id->bookable (merge m)))))))
