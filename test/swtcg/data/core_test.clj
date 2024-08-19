(ns swtcg.data.core-test
  (:require [swtcg.data.core :as sut]
            [clojure.test :as t]))

(t/deftest create-headers-test
  (t/testing "splits headers on tab"
    (let [s "h1\th2\th3"
          r (sut/create-headers s)]
      (t/is (= r [:h1 :h2 :h3]))
      (t/is (seq? r))
      (t/is (= 3 (count r)))))
  (t/testing "lower cases and keywordizes"
    (let [s "H1\tH2AbC\th3aBc"
          r (sut/create-headers s)]
      (t/is (= r [:h1 :h2abc :h3abc])))))
