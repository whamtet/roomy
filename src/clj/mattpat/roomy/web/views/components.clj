(ns mattpat.roomy.web.views.components
    (:require
      [mattpat.roomy.util :as util]
      [mattpat.roomy.web.views.icons :as icons]))

;; keep this for entire body!
[:div.h-full.bg-white]

(defn input
  ([type {:keys [label name value autocomplete asterisk disabled required]}]
   [:div
    [:div.p-1 label (when asterisk [:span.text-red-500 " *"])]
    [:div
     [:input.w-full.p-2
      {:type type
       :name name
       :value value
       :autocomplete autocomplete
       :disabled disabled
       :required required
       :placeholder label}]]])
  ([type label name]
   (input type {:label label :name name :required true}))
  ([type label name value]
   (input type {:label label :name name :value value :required true}))
  ([type label name value asterisk]
   (input type {:label label :name name :value value :asterisk asterisk :required asterisk}))
  ([type label name value asterisk disabled]
   (input type {:label label :name name :value value :asterisk asterisk :disabled disabled :required true})))

(def text (partial input "text"))
(def email (partial input "email"))
(def number (partial input "number"))

(defn submit
  ([label]
   [:input {:type "submit"
            :class "bg-kkr-purple p-1.5 rounded-lg text-white"
            :value label}])
  ([id label]
   [:input {:type "submit"
            :id id
            :class "bg-kkr-purple p-1.5 rounded-lg text-white"
            :value label}]))

(defn submit-hidden [id]
  [:input.hidden {:type "submit" :id id}])

(defn button [label]
  [:button {:type "button"
            :class "bg-kkr-purple py-1.5 px-3 rounded-lg text-white"}
   label])

(defn button-light [label]
  [:button {:type "button"
            :class "py-1.5 px-3 rounded-lg border"}
   label])

(defn modal [width & contents]
  [:div#modal {:class "fixed left-0 top-0 w-full h-full
  z-10"
               :style {:background-color "rgba(0,0,0,0.4)"}
               :_ "on click if target.id === 'modal' add .hidden"}
   [:div {:class (str "mx-auto border rounded-lg bg-white " width)
          :style {:max-height "94vh"
                  :margin-top "3vh"
                  :margin-bottom "3vh"}}
    contents]])

(defn modal-scroll [width & contents]
  [:div#modal {:class "fixed left-0 top-0 w-full h-full
  z-10"
               :style {:background-color "rgba(0,0,0,0.4)"}
               :_ "on click if target.id === 'modal' add .hidden"}
   [:div {:class (str "mx-auto border rounded-lg bg-white overflow-y-auto overflow-x-clip " width)
          :style {:max-height "94vh"
                  :margin-top "3vh"
                  :margin-bottom "3vh"}}
    contents]])

(defn warning [msg]
  [:span {:class "bg-red-600 p-2 rounded-lg text-white"} msg])

(defn warning-div [msg]
  [:div {:class "bg-red-600 p-2 rounded-lg text-white"} msg])

(defn qtip [msg]
  [:div.tooltip.relative.text-gray-600 icons/qmark
   [:span.tooltiptext.invisible.absolute.w-48.border.p-2.rounded-md.bg-white.z-10 msg]])

(def hidden-matcher #"(#)?([^\.]+)\.?(.+)?")
(defn- hiddens* [args]
  `(list
    ~@(for [[k-str v] args]
       (let [[_ id? k class] (re-find hidden-matcher k-str)]
         [:input (cond-> {:type "hidden" :name k}
                         id? (assoc :id k)
                         class (assoc :class class)
                         (not= nil v) (assoc :value v))]))))
(defmacro hiddens [& args]
  (hiddens* (partition 2 args)))

(defn- defset [sym]
  `(format ~(-> sym name (.replace "-" "_") (str " = new Set(%s);")) (util/write-str ~sym)))
(defmacro defsets [& syms]
  `[:script ~@(map defset syms)])
