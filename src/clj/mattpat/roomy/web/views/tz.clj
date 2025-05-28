(ns mattpat.roomy.web.views.tz
    (:require
      [data.timezones :as timezones]
      [mattpat.roomy.web.htmx :refer [defcomponent]]
      [mattpat.roomy.web.views.components :as components]))

(defn- set-vals [id display]
  (format "$m({'tz-disp': '%s', 'new-tz': '%s'}); $('#search-results').className = 'hidden';" display id))

[:div {:class "p-3 border-x border-b hover:bg-slate-100"}]
(defn- search-options [q direct-click?]
  (if (or q direct-click?)
    [:div#search-results
     [:input#new-tz {:type "hidden" :name "new-tz"}]
     (map-indexed
      (fn [i {:keys [id display]}]
        [:div {:class
               (if (zero? i)
                 "p-3 border hover:bg-slate-100"
                 "p-3 border-x border-b hover:bg-slate-100")
               :title id
               :hx-get (when direct-click? "room-search")
               :hx-include ".room-search2"
               :hx-target "#room-search"
               :hx-vals {:new-tz id}
               :_ (when direct-click? "on click add .hidden to #modal")
               :onclick (when-not direct-click? (set-vals id display))}
         display])
      (timezones/search q))]
    [:div#search-results]))

(defcomponent ^:endpoint search [req command q new-tz]
  (case command
    "q" (search-options q (boolean tz))
    [:div
     [:input {:class "w-full p-2 mb-1"
              :id "tz-disp"
              :type "text"
              :hx-get "search:q"
              :name "q"
              :hx-trigger "focus,keyup changed delay:0.3s"
              :hx-target "#search-results"
              :autocomplete "off"
              :placeholder "Search Timezones..."}]
     (search-options nil (boolean tz))]))

(defcomponent ^:endpoint search-modal [req]
  (if top-level?
    (components/modal "w-1/2" (search req))
    [:div {:class "mb-2"
           :hx-get "search-modal"
           :hx-target "#modal"}
     (components/button "Change...")]))
