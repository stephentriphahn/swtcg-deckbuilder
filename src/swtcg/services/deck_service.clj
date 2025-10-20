(ns swtcg.services.deck-service
  (:require
   [swtcg.web.error :as error]
   [swtcg.validation.service :as validation-service]
   [swtcg.db.db :as swtcg-db]))

(defn- get-and-validate-cards
  [db deck-id deck]
  (let [cards (swtcg-db/get-deck-cards db deck-id)
        validation (validation-service/validate-deck deck cards)
        deck-cards (map #(select-keys % [:card-id :quantity]) cards)]
    (-> deck
        (assoc :validation validation)
        (assoc :cards deck-cards))))

(defn get-deck
  [db deck-id]
  (if-let [deck (swtcg-db/get-deck-by-id db deck-id)]
    (get-and-validate-cards db deck-id deck)
    (throw (error/not-found {:deck-id deck-id}))))
