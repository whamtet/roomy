(ns mattpat.roomy.web.views.dropdown)

(defn- button [label]
  [:button {:type "button"
            :class "bg-kkr-purple p-2 rounded-lg text-white caret"
            :_ "on click halt the event then toggle .hidden on the next <div />"}
   label])

(defn dropdown [label other-items]
  [:div.p-1
   (button label)
   [:div {:class "drop hidden rounded-lg border p-1 m-1 absolute right-0 top-9.5 w-48"}
    (for [item other-items]
      [:div {:class "hover:bg-slate-100"}
       item])]])

(defn button-up [label]
  [:div
   [:button {:type "button"
             :class "p-1.5 w-32 caret-up"
             :_ "on click halt the event then toggle .hidden on the previous .drop"}
    [:span.mr-2 label]]])
