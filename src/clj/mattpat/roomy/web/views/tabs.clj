(ns mattpat.roomy.web.views.tabs
    (:require
      [clojure.string :as string]))

(defn- onclick [i]
  (string/join ["on click take .selected from .tab "
                "add .hidden to .tab-content "
                "remove .hidden from #content" i
                " tabIndex(" i ")"]))

(defn tabs [default-tab args]
  [:div
   [:div.tab-list
    (->> args
         (take-nth 2)
         (map-indexed
          (fn [i x]
            [:a {:class (if (= default-tab i) "tab selected" "tab")
                 :_ (onclick i)}
             x])))]
   (->> args
        rest
        (take-nth 2)
        (map-indexed
         (fn [i content]
           [:div {:class (if (= default-tab i) "tab-content" "hidden tab-content")
                  :id (str "content" i)}
            content])))])
