(ns mattpat.roomy.web.views.room-search.resource-details
    (:require
      [clojure.string :as string]
      [clojure.walk :as walk]
      [mattpat.roomy.env :refer [dev?]]
      [mattpat.roomy.util :as util :refer [mk assoc-mk]]
      [mattpat.roomy.web.controllers.builder :as builder]
      [mattpat.roomy.web.controllers.room :as room]
      [mattpat.roomy.web.htmx :refer [defcomponent]]
      [mattpat.roomy.web.views.accordion :as accordion]
      [mattpat.roomy.web.views.components :as components]
      [mattpat.roomy.web.views.icons :as icons]
      [simpleui.core :as simpleui]))

(defn half-width? [{:keys [type half-width]}]
  (or half-width (= type "checkbox")))

(defn map-pairs
  "maps f1 onto pairs where possible and f2 onto singles"
  [f1 f2 [a b & rest]]
  (cond
   (and (half-width? a) (half-width? b))
   (cons
    (f1 a b) (map-pairs f1 f2 rest))
   (half-width? a)
   (cons
    (f1 a nil) (map-pairs f1 f2 (conj rest b)))
   a
   (cons
    (f2 a) (map-pairs f1 f2 (conj rest b)))))

(defn- render-field-col [accordion-i
                         {:keys [field type required uuid description
                                 inline room-id-service resource-id]} data]
  (let [required (and required (not inline))]
    (case type
          "checkbox"
          [:div {:class "w-1/2 flex items-baseline my-4"}
           [:input {:class "mx-2"
                    :type "checkbox"
                    :name (format "checkbox:%s:%s" uuid accordion-i)
                    :hx-post (when inline "single-resource")
                    :hx-vals (mk room-id-service resource-id uuid type)
                    :checked (get data uuid)}]
           field
           [:span.text-gray-700.ml-2 description]]
          [:div {:class "w-1/2"}
           [:div.p-2 field]
           [:input.p-2.w-full
            {:type type
             :hx-post (when inline "single-resource")
             :hx-vals (mk room-id-service resource-id uuid type)
             :name uuid
             :value (get data uuid)
             :required required
             :placeholder field}]])))

(defn- render-field-half [i col1 col2 data]
  [:div.flex.items-stretch
   (render-field-col i col1 data)
   (when col2 (render-field-col i col2 data))])

;; classes generated in associated js
[:div {:class "editable-link cursor-pointer relative text-blue-400
absolute invisible z-10 p-2 bg-white border"}]
(defn- render-field-full [{:keys [field description type
                                  required uuid inline room-id-service resource-id]} data]
  (let [required (and required (not inline))]
    [:div.w-full
     [:div.mt-2 field (when required [:span.text-red-500 " *"])]
     [:div
      (when description
            [:p.text-gray-600.my-2 description])
      (case type
            "textarea"
            [:div
             [:textarea {:class "relative opacity-0"
                         :style {:width "1px"
                                 :height "1px"
                                 :top "-30px"
                                 :left "5px"}
                         :tabindex "-1"
                         :id (str uuid "-data")
                         :name uuid
                         :onkeydown (format "focusId('%s')" uuid)
                         :hx-post (when inline "single-resource")
                         :hx-vals (mk room-id-service resource-id uuid)
                         :required required}]
             [:div {:class "w-full border p-2 rounded-sm min-h-72 -mt-6"
                    :id uuid
                    :contenteditable "true"
                    :onblur (format "formatContentEditable('%s')" uuid)}
              (get data uuid)]
             [:script (format "formatContentEditable('%s')" uuid)]]
            "time range"
            [:div.flex.items-center.mt-4.mb-6
             [:b.mx-2 "From: "]
             [:input {:type "datetime-local"
                      :name uuid
                      :value (get-in data [uuid 0])
                      :required required
                      :hx-post (when inline "single-resource")
                      :hx-vals (mk room-id-service resource-id uuid :tr-index 0)
                      :placeholder field}]
             [:b.mx-4 "To :"]
             [:input {:type "datetime-local"
                      :name uuid
                      :value (get-in data [uuid 1])
                      :required required
                      :hx-post (when inline "single-resource")
                      :hx-vals (mk room-id-service resource-id uuid :tr-index 1)
                      :placeholder field}]]
            ;; else
            [:input.p-2.w-full
             {:type type
              :name uuid
              :value (get data uuid)
              :required required
              :hx-post (when inline "single-resource")
              :hx-vals (mk room-id-service resource-id uuid type)
              :placeholder field}])]]))

(defn- assoc-time-range [v i x]
  (assoc (or v [nil nil]) i x))

(defn- update-single [x params uuid type tr-index]
  (let [x (or x [{}])]
    (if tr-index
      (->> uuid
           params
           (update-in x [0 uuid] assoc-time-range tr-index))
      (if-let [v (params uuid)]
        (->> v ((builder/field-parser type)) (assoc-in x [0 uuid]))
        (update x 0 dissoc uuid)))))

(defcomponent ^:endpoint single-resource [req
                                          fields
                                          room-id-service
                                          ^:long resource-id
                                          uuid
                                          type
                                          ^:long tr-index]
  (if top-level?
    (update-in
     session
     [:locked? room-id-service :resources resource-id :details]
     update-single
     (walk/stringify-keys params)
     uuid
     type
     tr-index)
    (let [data (get-in locked? [room-id-service :resources resource-id :details 0])]
      [:div
       (->> fields
            (map #(assoc-mk % :inline true room-id-service resource-id))
            (map-pairs #(render-field-half %1 %2 data)
                       #(render-field-full % data)))])))

(defn- accordion-pair [i fields data hx-vals]
  (let [title (or
               (->> fields
                    (filter :title-line)
                    (map #(-> % :uuid data))
                    (string/join " ")
                    not-empty)
               (format "Record <%s>" (inc i)))]
    (list
     [:div.inline-flex.items-center
      [:span.mr-2 title]
      (when-not (:last? data)
                [:span.text-gray-500
                 {:hx-delete "resource-details"
                  :hx-confirm (format "Delete %s?" title)
                  :hx-vals (assoc hx-vals :i i)}
                 icons/trash])]
     (map-pairs #(render-field-half i %1 %2 data)
                #(render-field-full % data)
                fields))))

(defmacro when-update [session body]
  `(if (or (simpleui/post? ~'req) (simpleui/delete? ~'req))
    {:session ~session
     :body ~body}
    ~body))

[:div {:class "w-3/4"}]
(defcomponent ^:endpoint resource-details [req
                                           description
                                           ^:long resource-id
                                           room-id-service
                                           ^:boolean add
                                           ^:long data-count
                                           ^:long i]
  (let [session (cond
                 (simpleui/post? req)
                 (->> params
                      walk/stringify-keys
                      (builder/session-array data-count resource-id)
                      (assoc-in session [:locked? room-id-service :resources resource-id :details]))
                 (simpleui/delete? req)
                 (update-in session [:locked? room-id-service :resources resource-id :details] builder/session-delete i)
                 :else
                 session)]
    (if (and (simpleui/post? req) (not add))
      ;; go back to service-modal
      {:session session
       :body [:div#modal {:hx-get "service-modal"
                          :hx-vals {:room-id-service room-id-service}
                          :hx-include ".booking-info"
                          :hx-target "#modal"
                          :hx-trigger "load"}]}
      (let [fields (builder/fields resource-id)
            data (as-> (get-in session [:locked? room-id-service :resources resource-id :details]) $
                       (remove empty? $)
                       (concat $ [{:last? true}]))
            data-count (count data)
            hx-vals (mk room-id-service resource-id description data-count)]
        (when-update
         session
         (components/modal-scroll
          "w-3/4"
          [:div.p-2
           [:div.flex.items-baseline
            [:h3.mr-2 description]
            [:h5 "For room " (room/id->room-title room-id-service)]]
           [:form {:hx-post "resource-details"
                   :hx-vals hx-vals
                   :hx-target "#modal"}
            (components/hiddens "#add" nil)
            (if (= 1 data-count)
              (map-pairs #(render-field-half 0 %1 %2 (first data))
                         #(render-field-full % (first data))
                         fields)
              (->> data
                   (map-indexed #(accordion-pair %1 fields %2 hx-vals))
                   (accordion/accordion (dec data-count))))
            [:div.flex.items-center.mt-2
             ;; back without saving
             [:div {:class "mr-2"
                    :hx-get "service-modal"
                    :hx-confirm (when-not dev? "Back without saving?")
                    :hx-vals {:room-id-service room-id-service}
                    :hx-include ".booking-info"
                    :hx-target "#modal"}
              (components/button-light "Back")]
             ;; save
             [:div.mr-2
              (components/submit "regular-save" "Save")]
             [:div {:onclick "$('#add').value = 'true'; $('#regular-save').click();"}
              (components/button "Save and Add Another")]]]]))))))
