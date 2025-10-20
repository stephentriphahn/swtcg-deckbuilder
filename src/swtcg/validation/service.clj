(ns swtcg.validation.service
  (:require
   [clara.rules :as rules]
   [swtcg.validation.rules :as vrules]))

(defn validate-deck
  "Validate a deck with its cards. Returns validation result."
  [deck cards]
  (let [deck-fact (vrules/map->Deck deck)
        card-facts (map vrules/map->Card cards)
        session (-> (rules/mk-session 'swtcg.validation.rules)
                    (rules/insert deck-fact)
                    (rules/insert-all card-facts)
                    (rules/fire-rules))
        violations (map :?violation (rules/query session vrules/get-violations))
        errors (filter #(= :error (:severity %)) violations)
        warnings (filter #(= :warning (:severity %)) violations)]

    {:valid? (empty? errors)
     :violations (map (fn [v] {:rule (:rule-id v)
                               :message (:message v)})
                      errors)
     :warnings (map (fn [v] {:rule (:rule-id v)
                             :message (:message v)})
                    warnings)}))

(comment
  (def deck {:deck-id 1 :name "foo" :owner "foo" :format "standard" :side "D"})
  (def cards [{:card-id 1 :name "bar" :power 4 :type :character :side "D" :quantity 6} {:card-id 4 :name "baz" :type :character :side "L" :quantity 4}])
  (validate-deck deck cards)
  #_())
