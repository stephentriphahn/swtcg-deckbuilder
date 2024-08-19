(ns swtcg.data.card-test
  (:require [swtcg.data.card :as sut]
            [clojure.test :as t]))

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
