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

(defn- require-deck!
  [db deck-id]
  (or (swtcg-db/get-deck-by-id db deck-id)
      (throw (error/not-found {:deck-id deck-id}))))

(defn- require-card!
  [db card-id]
  (or (swtcg-db/get-card-by-id db card-id)
      (throw (error/not-found {:card-id card-id}))))

(defn get-deck
  [db deck-id]
  (get-and-validate-cards db deck-id (require-deck! db deck-id)))

(defn list-decks
  [db]
  (swtcg-db/list-decks db))

(defn delete-deck
  [db deck-id]
  (require-deck! db deck-id)
  (swtcg-db/delete-deck db deck-id))

(defn add-card
  "Adds (or updates the quantity of) a card in a deck."
  [db deck-id card-id quantity]
  (require-deck! db deck-id)
  (require-card! db card-id)
  (swtcg-db/add-card-to-deck db deck-id card-id quantity))

(defn add-cards
  "Adds several cards to a deck. All decks and cards are checked before any
  insert happens, so a bad id does not leave the deck partially updated."
  [db deck-id cards]
  (require-deck! db deck-id)
  (run! #(require-card! db (:card-id %)) cards)
  (doall (map #(swtcg-db/add-card-to-deck db deck-id (:card-id %) (:quantity %)) cards)))

(defn remove-card
  [db deck-id card-id]
  (require-deck! db deck-id)
  (when-not (some #(= card-id (:card-id %)) (swtcg-db/get-deck-cards db deck-id))
    (throw (error/not-found {:deck-id deck-id :card-id card-id})))
  (swtcg-db/remove-card-from-deck db deck-id card-id))
