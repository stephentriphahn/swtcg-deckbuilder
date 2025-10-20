(ns swtcg.validation.rules
  {:clj-kondo/ignore [:unresolved-symbol :unused-binding]}
  (:require [clara.rules :refer [defrule defquery defsession insert!]]
            [clara.rules.accumulators :as acc]))

;; Facts
(defrecord Deck [deck-id name owner format side])
(defrecord Card [card-id type side quantity])
(defrecord Violation [rule-id severity message])

;; Rule: Deck must have exactly 60 cards
(defrule deck-size-must-be-60
  "A deck must contain exactly 60 cards"
  [?total <- (acc/sum :quantity) :from [Card]]
  [:test (not= ?total 60)]
  =>
  (insert! (->Violation
            :deck-size
            :error
            (str "Deck must contain exactly 60 cards (currently has " ?total ")"))))

;; Rule: No more than 4 of any card
(defrule card-quantity-limit
  "No card can have quantity greater than 4"
  [Deck (= ?deck-id deck-id)]
  [?card <- Card  (> quantity 4)]
  =>
  (insert! (->Violation
            :card-quantity
            :error
            (str "Card " (:card-id ?card) " exceeds maximum quantity of 4 "
                 "(has " (:quantity ?card) ")"))))

;; Query to get all violations
(defquery get-violations
  "Get all validation violations"
  []
  [?violation <- Violation])

;; Session
(defsession validation-session 'swtcg.validation.rules)

(comment
  (def deck {:deck-id 1 :name "foo" :owner "foo" :format "standard" :side "D"})
  (def cards [{:card-id 1 :type :character :side "D" :quantity 60}])
  #_())
