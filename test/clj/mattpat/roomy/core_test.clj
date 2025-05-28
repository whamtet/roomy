(ns mattpat.roomy.core-test
  (:require
    [mattpat.roomy.test-utils :as utils]
    [integrant.core :as ig]
    [clojure.test :refer :all]))



(deftest test1
  (testing "should load system"
    (= 1 1)))