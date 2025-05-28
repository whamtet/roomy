(ns mattpat.roomy.web.views.accordion
    (:require
      [mattpat.roomy.util :as util]
      [mattpat.roomy.web.views.icons :as icons]))

[:div.hidden.float-right]
(defn- arrows [j up?]
  (if up?
    (list [:span {:class (str "float-right chevron-" j)} icons/chevron-up]
          [:span {:class (str "float-right hidden chevron-" j)} icons/chevron-down])
    (list [:span {:class (str "float-right hidden chevron-" j)} icons/chevron-up]
          [:span {:class (str "float-right chevron-" j)} icons/chevron-down])))

(defn- onclick [j]
  (format "on click toggle .hidden on .chevron-%s toggle .fold-expanded on next .fold" j))

[:div.border.rounded-t-lg.cursor-pointer.p-2]
(defn accordion [i items]
  (->> items
       (map-indexed
        (fn [j [title body]]
          [:div
           [:div {:class (util/cond-class "border cursor-pointer p-2"
                                          (= 0 j) "rounded-t-lg")
                  :_ (onclick j)}
            title
            (arrows j (= i j))]
           (if (= i j)
             [:div.border.fold.fold-expanded body]
             [:div.border.fold body])
           ]))))
