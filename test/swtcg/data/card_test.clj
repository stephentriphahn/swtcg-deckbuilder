(ns swtcg.data.card-test
  (:require [swtcg.data.card :as sut]
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

(t/deftest build-filter-fn-test
  (t/testing "simple map uses equals"
    (let [params {:foo "bar"}
          f (sut/build-filter-fn params)
          r (filter f [{:foo "bar"} {:foo "baz"}])]
      (t/is (= [{:foo "bar"}] r))))
  (t/testing "it honors nested params with various comparitors"
    (let [params {:foo {:lte 2}}
          f (sut/build-filter-fn params)
          r (filter f [{:foo 3} {:foo 2} {:foo 1}])]
      (t/is (= [{:foo 2} {:foo 1}] r))))
  (t/testing "it honors both nested params with various comparitors and equals"
    (let [params {:foo {:gt 2} :bar "baz"}
          f (sut/build-filter-fn params)
          r (filter f [{:foo 3 :bar "baz"} {:foo 2 :bar "baz"} {:foo 3 :bar "quux"}])]
      (t/is (= [{:foo 3 :bar "baz"}] r)))))
