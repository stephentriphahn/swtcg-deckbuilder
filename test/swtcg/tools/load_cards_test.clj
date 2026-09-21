(ns swtcg.tools.load-cards-test
  (:require [clojure.test :refer [deftest is]]
            [swtcg.tools.load-cards :as sut]))

(deftest card-id-is-deterministic-short-hex
  (is (= (sut/card-id "ROTS001_Anakin_Skywalker_M_HD")
         (sut/card-id "ROTS001_Anakin_Skywalker_M_HD")))
  (is (re-matches #"[0-9a-f]{12}" (sut/card-id "ROTS001_Anakin_Skywalker_M_HD")))
  (is (not= (sut/card-id "a") (sut/card-id "b"))))
