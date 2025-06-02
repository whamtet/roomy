(ns mattpat.roomy.web.htmx
  (:require
   [mattpat.roomy.env :refer [dev?]]
   [mattpat.roomy.web.resource-cache :as resource-cache]
   [simpleui.core :as simpleui]
   [simpleui.render :as render]
   [ring.util.http-response :as http-response]
   [hiccup.core :as h]))

(defn- html5 [content]
  (->> content
       h/html
       (format "<!DOCTYPE html> %s")))

(defn page [& content]
  (-> content
      html5
      http-response/ok
      (http-response/content-type "text/html")))

(defn- unminify [^String s]
  (if dev?
    (.replace s ".min" "")
    s))

(defn- scripts [{:keys [js hyperscript?]}]
  (cond-> (map resource-cache/cache-suffix js)
          hyperscript? (conj (unminify "https://unpkg.com/hyperscript.org@0.9.12/dist/_hyperscript.min.js"))))

(defn page-htmx [{:keys [datepicker? si-stack] :as opts} & body]
  (page
   [:html {:class "h-full bg-white"}
    [:head
     [:meta {:charset "UTF-8"}]
     [:title "Roomy"]
     [:meta {:property "og:title" :content "Roomy SimpleUI Demo"}]
     [:meta {:property "og:type" :content "website"}]
     [:meta {:property "og:url" :content "https://roomy.simpleui.io/"}]
     [:meta {:property "og:image" :content "https://roomy.simpleui.io/icon.svg"}]
     [:link {:rel "icon" :href "/icon.svg"}]
     ;; fonts
     [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
     [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin true}]
     [:link {:href "https://fonts.googleapis.com/css2?family=ABeeZee&display=swap" :rel "stylesheet"}]
     (when datepicker?
           [:link {:href (resource-cache/cache-suffix "/datepicker.css") :rel "stylesheet"}])
     ;; tailwind
     [:link {:rel "stylesheet" :href (resource-cache/cache-suffix "/output.css")}]]
    [:body.h-full
     [:div#modal.hidden]
     (render/walk-attrs body)
     (for [id si-stack]
       [:div {:id id :style "display: none;"}])
     [:script {:src (unminify "https://unpkg.com/htmx.org@1.9.5/dist/htmx.min.js")}]
     [:script "htmx.config.defaultSwapStyle = 'outerHTML';"]
     [:script {:src (resource-cache/cache-suffix "/common.js")}]
     (when datepicker?
           [:script {:src (resource-cache/cache-suffix "/datepicker.min.js")}])
     (map
      (fn [src]
        [:script {:src src}])
      (scripts opts))]]))

(defmacro defcomponent
  [name [req :as args] & body]
  (if-let [sym (simpleui/symbol-or-as req)]
    `(simpleui/defcomponent ~name ~args
      (let [{:keys [~'session ~'db ~'node ~'graph ~'event-bus]} ~sym
            {:keys [~'user_name ~'tz ~'locked? ~'tf]} ~'session
            ~'user_name (or ~'user_name "Devy")]
        ~@body))
    (throw (Exception. "req ill defined"))))
