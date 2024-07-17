(ns swtcg.web.handlers-test
  (:require [swtcg.web.handlers :as sut]
            [clojure.test :as t]))

(t/deftest normalize-opts-test
  (t/testing "parses integer fields into ints"
    (let [p {:power "2" :speed {:gt "1"}}
          actual (sut/normalize-opts p)]
      (t/is (= actual {:power 2 :speed {:gt 1}}))))
  (t/testing "does not parse non ints"
    (let [p {:foo "baz"}
          actual (sut/normalize-opts p)]
      (t/is (= actual p)))))
