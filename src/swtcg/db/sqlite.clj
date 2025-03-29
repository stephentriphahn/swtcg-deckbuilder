(ns swtcg.db.sqlite
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [hugsql.core :as hugsql]
   [swtcg.db.db :as db]
   [swtcg.log :as log]))

(defn parse-int [s]
  (when (and s (not-empty s))
    (try
      (Integer/parseInt s)
      (catch Exception e
        (log/warn :card-number-parse-error {:value s})
        -1))))

(defn process-card [card]
  (try
    (-> card
        (update :cost parse-int)
        (update :speed parse-int)
        (update :power parse-int)
        (update :health parse-int)
        (update :number parse-int)
        (update :usage #(if (empty? %) nil %))
        (update :script #(if (empty? %) nil %))
      ;; associng to add sql field names, ie set -> set_name and imagefile to image_file
        (assoc :set_code (:set card))
        (assoc :image_file (:imagefile card)))
    (catch Exception e
      (log/error :process-card-error {:card card} e))))

(defn read-tsv [filename]
  (log/info :reading-tsv-file {:filename filename})
  (with-open [reader (io/reader filename)]
    (let [rows (csv/read-csv reader :separator \tab)
          headers (map (comp keyword string/lower-case) (first rows))
          data (rest rows)]
      (mapv #(zipmap headers %) data))))

(defn read-cards
  [filename]
  (map process-card (read-tsv filename)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; load into sqlite

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

(defn- create-card-db
  [db]
  (let [_ (enable-foreign-keys! db)]
    (reify db/CardDatabase
      (get-card-by-id [this id]
        (search-cards db {:card_id id}))
      (list-cards [this opts]
        (search-cards db opts))

      (add-deck [this deck]
        (insert-deck! db deck)
        (get-deck-by-name db {:name (:name deck)}))
      (get-deck-by-id [this deck-id]
        (get-deck-by-id db {:deck_id deck-id}))
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
  (not-empty [:foo])
  (map :name (read-cards "resources/public/ESB.txt"))

  (def db {:dbname "cards.db" :dbtype "sqlite"})
  (def cdb (create-card-db db))
  (create-cards-table db)
  (create-decks-table db)
  (create-cards-to-deck-table db)
  (remove-card-from-deck! db {:deck_id 1 :card_id 1})
  (def test-deck {:name "testA" :side "L" :owner "admin" :format "standard"})
  (mapv (partial insert-card! db) (read-cards "resources/public/ESB.txt"))
  (db/list-cards cdb {:side "D" :set_code "ANH"})

  (db/add-deck cdb test-deck)
  (db/add-card-to-deck cdb 2 1 4)
  (db/get-deck-cards cdb 2)
  (db/get-deck-by-id cdb 2)
  (db/delete-deck cdb 2)
  #_())
