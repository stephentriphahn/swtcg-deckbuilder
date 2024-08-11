(ns swtcg.game.state-test
  (:require [swtcg.game.state :as sut]
            [clojure.test :refer [testing deftest is]]))

(deftest init-player-test
  (testing "initializes player and draws 7 cards"
    (let [actual (sut/init-player (for [i (range 10)] {:id i}))]
      (is (= 7 (count (:hand actual))))
      (is (= 3 (count (:deck actual))))
      (is (= 8 (:force actual))))))
