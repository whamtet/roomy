(ns mattpat.roomy.web.controllers.builder
  (:require
    [mattpat.roomy.util :as util]
    [simpleui.rt :as rt]))

(def types
  {"text" "Short Note"
   "number" "Likely Attendee Count"
   "email" "Contact Email"
   "checkbox" "Cleaning Required"
   "textarea" "Details"
   "time range" "Catering time"})
(def skip-required #{"checkbox"})
(def force-full-width #{"checkbox" "time range" "textarea"})
(def allow-title #{"text" "email" "time range"})

;; field
;; description
;; type
;; required
;; title-line
;; half-width

(defn- rand-boolean []
  (zero? (rand-int 2)))

(defn- random-field [[type field]]
  (cond-> {:type type :field field :uuid (str (random-uuid))}
    (not (skip-required type)) (assoc :required (rand-boolean))
    (not (force-full-width type)) (assoc :half-width (rand-boolean))
    (allow-title type) (assoc :title-line (rand-boolean))
    (rand-boolean) (assoc :description
                          (case type
                            "text" "One-liner info"
                            "number" "Include tentative attendees"
                            "email" "(optional)"
                            "checkbox" "Select if unsure"
                            "textarea" "Include all necessary information"
                            "time range" "Acceptable eating times"))))

(util/defm fields [_]
  (assert (number? _))
  (->> types
       seq
       shuffle
       (take (inc (rand-int 3)))
       (map random-field)))

(util/defm block-inline? [_]
  (assert (number? _))
  (zero? (rand-int 4)))

(defn field-parser [type]
  (case type
    "number" rt/parse-long
    "checkbox" rt/parse-boolean
    identity))

(defn- merge-checkboxes
  "checkboxes are split up, need to put them together at uuid"
  [params session-length]
  (->> params
       keys
       (filter #(.startsWith % "checkbox"))
       (map #(second (.split % ":")))
       distinct
       (reduce
        (fn [params uuid]
          (->> session-length
               range
               (map #(contains? params (str "checkbox:" uuid ":" %)))
               (assoc params uuid)))
        params)))

(defn- zip-session [vals f uuid session]
  (mapv #(assoc %1 uuid (f %2))
        session
        (if (or (seq? vals) (vector? vals)) vals [vals])))

(defn session-array
  "converts params ({uuid} [val0 val1]}) into
  [{uuid val0} {uuid val1}]"
  [n resource-id params]
  (prn n resource-id params)
  (let [params (merge-checkboxes params n)]
    (->> resource-id
         fields
         (reduce
          (fn [session {:keys [type uuid]}]
            (if-let [v (params uuid)]
              (cond-> v
                (= type "time range") (->> (partition 2) (map vec))
                true (zip-session (field-parser type) uuid session))
              session))
          (repeat n nil)))))

(defn session-delete [v i]
  (vec
   (concat (take i v) (drop (inc i) v))))
