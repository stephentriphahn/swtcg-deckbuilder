(ns stevetrip.swtcg.deck-builder.web.schema)

(def CardType
  [:enum
   "Character"
   "Space"
   "Ground"
   "Battle"
   "Equipment"
   "Location"
   "Mission"])

(def Side [:enum "L" "D" "N"])

(def Rarity [:enum "C" "U" "R" "P"])

(def CardResponse
  [:map
   [:card-id :string]
   [:name :string]
   [:type CardType]
   [:side Side]
   [:subtype {:optional true} [:maybe :string]]
   [:set-code :string]
   [:number [:maybe :int]] ;; may be null for promo cards
   [:rarity Rarity]  ; Common, Uncommon, Rare, etc.
   [:cost {:optional true} [:maybe :int]]
   [:power {:optional true} [:maybe :int]]
   [:health {:optional true} [:maybe :int]]
   [:speed {:optional true} [:maybe :int]]
   [:text {:optional true} [:maybe :string]]
   [:script {:optional true} [:maybe :string]]
   [:usage {:optional true} [:maybe :string]]
   [:classification {:optional true} [:maybe :string]]
   [:image-file {:optional true} [:maybe :string]]])

(def AddCardToDeckRequest
  [:map
   [:card-id {:optional true} [:string {:min 1}]] ;; optional, use from path params
   [:quantity [:int {:min 1 :max 4}]]])

(def AddCardsToDeckRequest
  [:vector AddCardToDeckRequest])

(def CreateDeckRequest
  [:map
   [:name [:string {:min 1}]]
   [:owner [:string {:min 1}]]
   [:format [:string {:min 1}]]
   [:side [:enum "L" "D"]]])

(def ValidationViolation
  [:map
   [:rule :keyword]
   [:message :string]])

(def ValidationStatus
  [:map
   [:valid? :boolean]
   [:violations [:vector ValidationViolation]]
   [:warnings [:vector ValidationViolation]]])

(def DeckSummary
  [:map
   [:deck-id :string]
   [:name [:string {:min 1}]]
   [:owner [:string {:min 1}]]
   [:format [:string {:min 1}]]
   [:side [:enum "L" "D"]]])

(def ListDecksResponse
  [:vector DeckSummary])

(def DeckCardResponse
  [:map
   [:deck-id :string]
   [:card-id :string]
   [:quantity int?]])

(def DeckResponse
  [:map
   [:deck-id :string]
   [:name [:string {:min 1}]]
   [:owner [:string {:min 1}]]
   [:format [:string {:min 1}]]
   [:cards [:vector AddCardToDeckRequest]]
   [:validation ValidationStatus]
   [:side [:enum "L" "D"]]])

(def ListQueryParams
  [:map
   [:search {:optional true} :string]
   [:side {:optional true} [:enum "L" "D" "N"]]
   [:type {:optional true} :string]
   [:set_code {:optional true} :string]
   [:limit {:optional true} [:int {:min 1 :max 100}]]
   [:skip {:optional true} [:int {:min 0}]]])

(def ListCardsResponse
  [:map
   [:cards [:vector CardResponse]]])
