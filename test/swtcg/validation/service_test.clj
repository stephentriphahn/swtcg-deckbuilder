(ns swtcg.validation.service-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [swtcg.validation.service :as svc]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Test Data Helpers

(defn make-deck
  "Create a test deck with default values."
  [& {:keys [deck-id name owner format side]
      :or {deck-id 1
           name "Test Deck"
           owner "test-user"
           format "standard"
           side "D"}}]
  {:deck-id deck-id
   :name name
   :owner owner
   :format format
   :side side})

(defn make-card
  "Create a test card with default values."
  [& {:keys [card-id name type side quantity power]
      :or {card-id 1
           name "Test Card"
           type "Character"
           side "D"
           quantity 1
           power 4}}]
  {:card-id card-id
   :name name
   :type type
   :side side
   :quantity quantity
   :power power})

(defn make-valid-deck-cards
  "Create a valid set of 60 cards with proper distribution.
  Returns 12 Character, 12 Space, 12 Ground, and 24 other cards."
  []
  (concat
   ;; 12 Character cards
   (for [i (range 1 13)]
     (make-card :card-id i :name (str "Character " i) :type "Character" :quantity 1))
   ;; 12 Space cards
   (for [i (range 13 25)]
     (make-card :card-id i :name (str "Space " i) :type "Space" :quantity 1))
   ;; 12 Ground cards
   (for [i (range 25 37)]
     (make-card :card-id i :name (str "Ground " i) :type "Ground" :quantity 1))
   ;; 24 Mission cards to reach 60
   (for [i (range 37 61)]
     (make-card :card-id i :name (str "Mission " i) :type "Mission" :quantity 1))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Tests

(deftest test-valid-deck
  (testing "Valid deck with 60 cards and proper distribution"
    (let [deck (make-deck :side "D")
          cards (make-valid-deck-cards)
          result (svc/validate-deck deck cards)]
      (is (:valid? result)
          "Deck should be valid")
      (is (empty? (:violations result))
          "Should have no violations")
      (is (empty? (:warnings result))
          "Should have no warnings"))))

(deftest test-deck-size-validation
  (testing "Deck with wrong number of cards"
    (testing "Too few cards"
      (let [deck (make-deck)
            cards [(make-card :quantity 30)]
            result (svc/validate-deck deck cards)]
        (is (not (:valid? result))
            "Deck with 30 cards should be invalid")
        (is (some #(= :deck-size (:rule %)) (:violations result))
            "Should have deck-size violation")))

    (testing "Too many cards"
      (let [deck (make-deck)
            cards [(make-card :quantity 70)]
            result (svc/validate-deck deck cards)]
        (is (not (:valid? result))
            "Deck with 70 cards should be invalid")
        (is (some #(= :deck-size (:rule %)) (:violations result))
            "Should have deck-size violation")))))

(deftest test-card-quantity-limit
  (testing "Card quantity exceeds limit of 4"
    (let [deck (make-deck)
          cards [(make-card :card-id 1 :quantity 5 :type "Character")
                 (make-card :card-id 2 :quantity 4 :type "Space")
                 (make-card :card-id 3 :quantity 51 :type "Ground")]
          result (svc/validate-deck deck cards)]
      (is (not (:valid? result))
          "Deck with cards over quantity 4 should be invalid")
      (is (some #(= :card-quantity (:rule %)) (:violations result))
          "Should have card-quantity violation")
      ;; Should flag both card 1 (qty 5) and card 3 (qty 51)
      (is (>= (count (filter #(= :card-quantity (:rule %)) (:violations result))) 2)
          "Should have violation for each card exceeding limit"))))

(deftest test-card-side-validation
  (testing "Cards must match deck side or be neutral"
    (testing "Light card in Dark deck"
      (let [deck (make-deck :side "D")
            cards (concat
                   (make-valid-deck-cards)
                   [(make-card :card-id 100 :name "Light Infiltrator" :side "L" :quantity 1)])
            ;; Remove one card to keep total at 60
            cards-60 (drop 1 cards)
            result (svc/validate-deck deck cards-60)]
        (is (not (:valid? result))
            "Dark deck with Light card should be invalid")
        (is (some #(= :card-side (:rule %)) (:violations result))
            "Should have card-side violation")))

    (testing "Dark card in Light deck"
      (let [deck (make-deck :side "L")
            cards (map #(assoc % :side "L") (make-valid-deck-cards))
            ;; Add one Dark card
            cards-with-dark (concat (drop-last cards)
                                    [(make-card :card-id 100 :name "Dark Agent" :side "D" :quantity 1)])
            result (svc/validate-deck deck cards-with-dark)]
        (is (not (:valid? result))
            "Light deck with Dark card should be invalid")
        (is (some #(= :card-side (:rule %)) (:violations result))
            "Should have card-side violation")))

    (testing "Neutral cards allowed in any deck"
      (let [deck (make-deck :side "D")
            cards (concat
                   (take 59 (make-valid-deck-cards))
                   [(make-card :card-id 100 :name "Neutral Trader" :side "N" :quantity 1)])
            result (svc/validate-deck deck cards)]
        (is (:valid? result)
            "Neutral cards should be allowed in any deck")))))

(deftest test-minimum-character-cards
  (testing "Deck must have at least 12 Character cards"
    (let [deck (make-deck)
          cards (concat
                 ;; Only 10 Character cards
                 (for [i (range 1 11)]
                   (make-card :card-id i :name (str "Character " i) :type "Character" :quantity 1))
                 ;; 12 Space cards
                 (for [i (range 11 23)]
                   (make-card :card-id i :name (str "Space " i) :type "Space" :quantity 1))
                 ;; 12 Ground cards
                 (for [i (range 23 35)]
                   (make-card :card-id i :name (str "Ground " i) :type "Ground" :quantity 1))
                 ;; 26 Mission cards to reach 60
                 (for [i (range 35 61)]
                   (make-card :card-id i :name (str "Mission " i) :type "Mission" :quantity 1)))
          result (svc/validate-deck deck cards)]
      (is (not (:valid? result))
          "Deck with only 10 Character cards should be invalid")
      (is (some #(= :min-characters (:rule %)) (:violations result))
          "Should have min-characters violation"))))

(deftest test-minimum-space-cards
  (testing "Deck must have at least 12 Space cards"
    (let [deck (make-deck)
          cards (concat
                 ;; 12 Character cards
                 (for [i (range 1 13)]
                   (make-card :card-id i :name (str "Character " i) :type "Character" :quantity 1))
                 ;; Only 8 Space cards
                 (for [i (range 13 21)]
                   (make-card :card-id i :name (str "Space " i) :type "Space" :quantity 1))
                 ;; 12 Ground cards
                 (for [i (range 21 33)]
                   (make-card :card-id i :name (str "Ground " i) :type "Ground" :quantity 1))
                 ;; 28 Mission cards to reach 60
                 (for [i (range 33 61)]
                   (make-card :card-id i :name (str "Mission " i) :type "Mission" :quantity 1)))
          result (svc/validate-deck deck cards)]
      (is (not (:valid? result))
          "Deck with only 8 Space cards should be invalid")
      (is (some #(= :min-space (:rule %)) (:violations result))
          "Should have min-space violation"))))

(deftest test-minimum-ground-cards
  (testing "Deck must have at least 12 Ground cards"
    (let [deck (make-deck)
          cards (concat
                 ;; 12 Character cards
                 (for [i (range 1 13)]
                   (make-card :card-id i :name (str "Character " i) :type "Character" :quantity 1))
                 ;; 12 Space cards
                 (for [i (range 13 25)]
                   (make-card :card-id i :name (str "Space " i) :type "Space" :quantity 1))
                 ;; Only 6 Ground cards
                 (for [i (range 25 31)]
                   (make-card :card-id i :name (str "Ground " i) :type "Ground" :quantity 1))
                 ;; 30 Mission cards to reach 60
                 (for [i (range 31 61)]
                   (make-card :card-id i :name (str "Mission " i) :type "Mission" :quantity 1)))
          result (svc/validate-deck deck cards)]
      (is (not (:valid? result))
          "Deck with only 6 Ground cards should be invalid")
      (is (some #(= :min-ground (:rule %)) (:violations result))
          "Should have min-ground violation"))))

(deftest test-multiple-violations
  (testing "Deck with multiple violations reports all of them"
    (let [deck (make-deck :side "D")
          cards [(make-card :card-id 1 :name "Overloaded" :type "Character" :side "D" :quantity 10)
                 (make-card :card-id 2 :name "Wrong Side" :type "Character" :side "L" :quantity 4)
                 (make-card :card-id 3 :name "Space Card" :type "Space" :side "D" :quantity 1)]
          result (svc/validate-deck deck cards)]
      (is (not (:valid? result))
          "Deck with multiple violations should be invalid")
      ;; Should have: wrong deck size, card quantity violation, wrong side, missing minimums
      (is (>= (count (:violations result)) 4)
          "Should report multiple violations"))))

(deftest test-validation-result-structure
  (testing "Validation result has correct structure"
    (let [deck (make-deck)
          cards (make-valid-deck-cards)
          result (svc/validate-deck deck cards)]
      (is (contains? result :valid?)
          "Result should have :valid? key")
      (is (contains? result :violations)
          "Result should have :violations key")
      (is (contains? result :warnings)
          "Result should have :warnings key")
      (is (boolean? (:valid? result))
          ":valid? should be a boolean")
      (is (seqable? (:violations result))
          ":violations should be a collection")
      (is (seqable? (:warnings result))
          ":warnings should be a collection")))

  (testing "Violation structure includes rule and message"
    (let [deck (make-deck)
          cards [(make-card :quantity 30)]
          result (svc/validate-deck deck cards)
          violation (first (:violations result))]
      (is (contains? violation :rule)
          "Violation should have :rule key")
      (is (contains? violation :message)
          "Violation should have :message key")
      (is (keyword? (:rule violation))
          ":rule should be a keyword")
      (is (string? (:message violation))
          ":message should be a string"))))

(deftest test-edge-cases
  (testing "Empty deck"
    (let [deck (make-deck)
          cards []
          result (svc/validate-deck deck cards)]
      (is (not (:valid? result))
          "Empty deck should be invalid")))

  (testing "Deck with maximum allowed quantity (4) for each card"
    (let [deck (make-deck)
          cards (concat
                 ;; 3 Character cards, 4 each = 12
                 (for [i (range 1 4)]
                   (make-card :card-id i :type "Character" :quantity 4))
                 ;; 3 Space cards, 4 each = 12
                 (for [i (range 4 7)]
                   (make-card :card-id i :type "Space" :quantity 4))
                 ;; 3 Ground cards, 4 each = 12
                 (for [i (range 7 10)]
                   (make-card :card-id i :type "Ground" :quantity 4))
                 ;; 6 Mission cards, 4 each = 24
                 (for [i (range 10 16)]
                   (make-card :card-id i :type "Mission" :quantity 4)))
          result (svc/validate-deck deck cards)]
      (is (:valid? result)
          "Deck with all cards at maximum quantity should be valid"))))
