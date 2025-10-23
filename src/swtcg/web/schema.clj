(ns swtcg.web.schema)

(def CardType
  [:enum
   "Character"
   "Space"
   "Ground"
   "Battle"
   "Mission"])

(def Side [:enum "L" "D" "N"])

(def Rarity [:enum "C" "U" "R" "P"])

(def Card
  [:map
   [:card-id :int]
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

(def AddCardToDeck
  [:map
   [:card-id int?]
   [:quantity [:int {:min 1 :max 4}]]])

(def CreateDeckRequest
  [:map
   [:name [:string {:min 1}]]
   [:owner [:string {:min 1}]]
   [:format [:string {:min 1}]]
   [:side [:enum "L" "D"]]])

(def ListCardsResponse
  [:map
   [:cards [:vector Card]]])
