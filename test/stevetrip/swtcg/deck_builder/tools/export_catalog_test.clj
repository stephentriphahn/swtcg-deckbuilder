(ns stevetrip.swtcg.deck-builder.tools.export-catalog-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [stevetrip.swtcg.deck-builder.tools.export-catalog :as sut]))

(def sample-card
  {:card-id "abc123" :name "Test Card " :set-code "AOTC" :image-file "img"
   :side "L" :type "Ground/Character " :subtype "Character - Trait"
   :cost 6 :speed 20 :power 3 :health 5 :rarity "R" :number 1
   :usage nil :text "Some text" :script nil :classification "Clone"})

(deftest normalize-card-trims-strings
  (is (= "Test Card" (:name (sut/normalize-card sample-card))))
  (is (= "Ground/Character" (:type (sut/normalize-card sample-card)))))

(deftest normalize-card-leaves-valid-stats-alone
  (let [normalized (sut/normalize-card sample-card)]
    (is (= 6 (:cost normalized)))
    (is (= 20 (:speed normalized)))
    (is (= 3 (:power normalized)))
    (is (= 5 (:health normalized)))))

(deftest normalize-card-maps-stat-sentinel-to-nil
  (testing "cost/speed/power/health -1 (load-cards' parse-failure sentinel) becomes nil"
    (let [normalized (sut/normalize-card (assoc sample-card
                                                  :cost -1 :speed -1
                                                  :power -1 :health -1))]
      (is (nil? (:cost normalized)))
      (is (nil? (:speed normalized)))
      (is (nil? (:power normalized)))
      (is (nil? (:health normalized))))))

(deftest normalize-card-leaves-number-sentinel-alone
  (testing "number's -1 sentinel is cosmetic, never used in arithmetic - left as-is"
    (is (= -1 (:number (sut/normalize-card (assoc sample-card :number -1)))))))

(deftest normalize-card-leaves-nil-stats-alone
  (is (nil? (:cost (sut/normalize-card (assoc sample-card :cost nil))))))

(deftest export-edn-str-round-trips
  (let [cards [sample-card (assoc sample-card :card-id "zzz999")]
        written (sut/export-edn-str cards)
        read-back (edn/read-string written)]
    (is (= {:cards cards} read-back))))

(deftest export-edn-str-empty-cards
  (is (= {:cards []} (edn/read-string (sut/export-edn-str [])))))
