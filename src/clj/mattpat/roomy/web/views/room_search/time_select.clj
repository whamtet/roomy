(ns mattpat.roomy.web.views.room-search.time-select
    (:require
      [mattpat.roomy.util :as util :refer [format-js$]]
      [mattpat.roomy.web.views.components :as components]))

(defn- lt [a b]
  (and a b (< a b)))

(defmacro js-str [a b]
  `(str ~a (format-js$ ~b)))

(defn- format2 [s] (format "%02d" s))

(defn- update-hour [suffix h1 m1 h2 m2 tf?]
  (let [h-disp (str "#hour" suffix)
        m-disp (str "#minute" suffix)
        m-select (format "select[name=hour%s]" suffix)
        value-f (if tf? "" "mod12")
        ampm (str "#ampm" suffix)]
    (cond-> (format-js$ "$('${h-disp}').innerHTML = ${value-f}(event.target.value);")
            (not tf?)
            (js-str "$('${ampm}').innerHTML = Number(event.target.value) < 12 ? ' AM' : ' PM';")
            (or h1 h2) ;; enable all minute selectors (before disable)
            (js-str "enable('${m-select} .minute');
            const v = Number(event.target.value);")
            h1 ;; disable minute-lo
            (js-str "if (v === ${h1}) {
            disable('${m-select} .minute-lo');
            maxInnerHTML('${m-disp}', ${m1});
            maxValue('${m-select}', ${m1});
            }")
            h2 ;; disable minute-hi
            (js-str "if (v === ${h2}) {
            disable('${m-select} .minute-hi');
            minInnerHTML('${m-disp}', ${m2});
            minValue('${m-select}', ${m2});
            }")
            true ;; always check crossing
            (str "checkCrossing();"))))

(defn- update-minute [suffix]
  (let [m-disp (str "#minute" suffix)]
    (format-js$ "$('${m-disp}').innerHTML = event.target.value; checkCrossing();")))

(defn- time-select [suffix h m {:keys [h1 m1]} {:keys [h2 m2]} tf?]
  [:div.flex
   [:span.relative
    [:span {:id (str "hour" suffix)}
     ((if tf? format2 util/mod12) h)]
    [:select {:class "absolute top-0 left-0 opacity-0 cursor-pointer booking-info"
              :name (str "hour" suffix)
              :onchange (update-hour suffix h1 m1 h2 m2 tf?)}
     (for [hour (range 24)]
       [:option {:disabled (or (lt hour h1) (lt h2 hour))
                 :selected (= hour h)
                 :value (format2 hour)}
        (if tf? hour (util/mod12-suffix hour))])]
    " : "
    [:span.relative
     [:span {:id (str "minute" suffix)} (format2 m)]
     [:select {:class "absolute top-0 left-0 opacity-0 cursor-pointer booking-info"
               :name (str "minute" suffix)
               :onchange (update-minute suffix)}
      (for [minute (range 0 60 5)]
        [:option {:class (cond
                          (lt minute m1) "minute minute-lo"
                          (lt m2 minute) "minute minute-hi")
                  :disabled (or
                             (and (= h1 h) (lt minute m1))
                             (and (= h h2) (lt m2 minute)))
                  :selected (= minute m)
                  :value (format2 minute)} minute])]]
    (when-not tf?
              [:span {:id (str "ampm" suffix)}
               (if (< h 12) " AM" " PM")])]])

(defn time-input [suffix previous h m next tf?]
  [:div.flex.items-center
   (time-select suffix h m previous next tf?)
   (when (not-empty suffix)
         [:span#crossing.hidden.ml-2 (components/warning "Make To after From")])])
