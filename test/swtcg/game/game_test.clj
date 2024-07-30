(ns swtcg.game.game-test
  (:require [swtcg.game.game :as sut]
            [clojure.test :refer [testing deftest is]]))

(deftest init-player-test
  (testing "initializes player and draws 7 cards"
    (let [actual (sut/init-player (range 10))]
      (is (= 7 (count (:hand actual))))
      (is (= 3 (count (:deck actual))))
      (is (= 8 (:force actual))))))
