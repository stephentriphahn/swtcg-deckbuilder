(ns swtcg.validation.rules
  {:clj-kondo/ignore [:unresolved-symbol :unused-binding]}
  (:require [clara.rules :refer [defrule defquery defsession insert!]]
            [clara.rules.accumulators :as acc]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; helpers
(defn side-name [side]
  (case side
    "L" "Light"
    "D" "Dark"
    "N" "Neutral"
    side))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; facts 

(defrecord Deck [deck-id name owner format side])
(defrecord Card [card-id type side name quantity])
(defrecord Violation [rule-id severity message])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Rules 

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

;; Rule: Card must be same side as deck (or neutral)
(defrule card-match-deck-side
  "Card side must be the same side as the deck (or neutral)"
  [Deck (= ?deck-side side)]
  [?card <- Card (not= side ?deck-side) (not= side "N")]
  =>
  (insert! (->Violation
            :card-side
            :error
            (str "Card '" (:name ?card) "' is "
                 (side-name (:side ?card))
                 " side, but this is a "
                 (side-name ?deck-side)
                 " side deck"))))

(defrule minimum-character-cards
  "Deck must have at least 12 character cards"
  [?total <- (acc/sum :quantity) :from [Card (= type "Character")]]
  [:test (< ?total 12)]
  =>
  (insert! (->Violation
            :min-characters
            :error
            (str "Deck must contain at least 12 Character cards (currently has " ?total ")"))))
(defrule minimum-space-cards
  "Deck must have at least 12 space cards"
  [?total <- (acc/sum :quantity) :from [Card (= type "Space")]]
  [:test (< ?total 12)]
  =>
  (insert! (->Violation
            :min-space
            :error
            (str "Deck must contain at least 12 Space cards (currently has " ?total ")"))))
(defrule minimum-ground-cards
  "Deck must have at least 12 ground cards"
  [?total <- (acc/sum :quantity) :from [Card (= type "Ground")]]
  [:test (< ?total 12)]
  =>
  (insert! (->Violation
            :min-ground
            :error
            (str "Deck must contain at least 12 Ground cards (currently has " ?total ")"))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; queries

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
