(ns mattpat.roomy.web.resource-cache
    (:require
      [clojure.java.io :as io]
      [mattpat.roomy.util :as util]))

(util/defm-dev hash-resource [src]
  (->> src
       (str "public")
       io/resource
       slurp
       hash))

(defn cache-suffix [src]
  (->> src
       hash-resource
       (str src "?hash=")))
