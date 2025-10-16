(ns swtcg.db.sqlite
  (:require
   [hugsql.core :as hugsql]
   [swtcg.db.db :as db]
   [swtcg.web.error :as error]))

(hugsql/def-db-fns "swtcg/db/sql/cards.sql")
(hugsql/def-db-fns "swtcg/db/sql/decks.sql")
(hugsql/def-sqlvec-fns "swtcg/db/sql/decks.sql")
(hugsql/def-sqlvec-fns "swtcg/db/sql/cards.sql")

(declare search-cards)
(declare insert-deck!)
(declare insert-card-to-deck!)
(declare get-deck-by-id)
(declare get-deck-by-name)
(declare get-deck-cards)
(declare delete-deck!)
(declare enable-foreign-keys!)
(declare remove-card-from-deck!)
(declare remove-all-cards-from-deck!)
(declare insert-card!)

(defn- add-deck*
  [db deck]
  (try
    (insert-deck! db deck)
    (catch org.sqlite.SQLiteException e
      (if (re-find #"(?i)unique" (ex-message e))
        (throw
         (error/conflict "A deck with that name already exists."
                         {:name (-> deck :body :name)}))
        (throw e)))))

(defn- create-card-db
  [db]
  (let [_ (enable-foreign-keys! db)]
    (reify db/CardDatabase
      (get-card-by-id [this id]
        (first (search-cards db {:card_id id})))
      (list-cards [this opts]
        (search-cards db opts))

      (add-deck [this deck]
        (add-deck* db deck))

      (get-deck-by-id [this deck-id]
        (first (get-deck-by-id db {:deck_id deck-id})))

      (delete-deck [this deck-id]
        (delete-deck! db {:deck_id deck-id})
        ;; FIXME this is a hack because cascade delete not working in sqlite
        (remove-all-cards-from-deck! db {:deck_id deck-id}))

      (get-deck-cards [this deck-id]
        (get-deck-cards db {:deck_id deck-id}))
      (add-card-to-deck [this deck-id card-id quantity]
        (insert-card-to-deck! db {:deck_id deck-id :card_id card-id :quantity quantity}))
      (remove-card-from-deck [this deck-id card-id]
        (remove-card-from-deck! db {:deck_id deck-id :card_id card-id})))))

(defmethod db/connect :sqlite
  [parsed-cs]
  (let [db (db/parsed-cs->jdbc-config parsed-cs)]
    (create-card-db db)))

(comment
  (def db-spec {:dbname "cards.db" :dbtype "sqlite"})
  (def cdb (create-card-db db-spec))
  (def sets-to-load #{"AOTC" "SR" "ANH" "BOY" "ESB" "RAS" "JG" "ROTJ" "PM" "ROTS"})
  (mapv #(mapv (partial insert-card! db) (read-cards (str "resources/public/sets" % ".txt"))) sets-to-load)
  (db/get-deck-cards cdb 1)
  (db/list-cards cdb {:side "D" :cost 5 :set_code "AOTC"})
  #_())
