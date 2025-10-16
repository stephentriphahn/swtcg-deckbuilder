(ns swtcg.web.schema)

(def AddCardToDeck
  [:map
   [:card-id int?]
   [:quantity [:int {:min 1 :max 4}]]])

(def CreateDeckRequest
  [:map
   [:name [:string {:min 1}]]
   [:owner [:string {:min 1}]]
   [:format [:string {:min 1}]]
   [:cards {:optional true} [:vector AddCardToDeck]]
   [:side [:enum "L" "D"]]]) ;; Light/Dark

