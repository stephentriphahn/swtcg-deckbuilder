(ns swtcg.services.deck-service
  (:require
   [swtcg.web.error :as error]
   [swtcg.db.db :as swtcg-db]))

(defn get-deck
  [db deck-id]
  (if-let [deck (swtcg-db/get-deck-by-id db deck-id)]
    ;; TODO add validation here?
    (->> (swtcg-db/get-deck-cards db deck-id)
         (map #(select-keys % [:card-id :quantity]))
         (assoc deck :cards))
    (throw (error/not-found {:deck-id deck-id}))))
