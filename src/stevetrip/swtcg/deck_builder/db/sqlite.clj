(ns stevetrip.swtcg.deck-builder.db.sqlite
  (:require
   [cheshire.core :as json]
   [hugsql.core :as hugsql]
   [hugsql.adapter.next-jdbc :as adapter]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [stevetrip.swtcg.deck-builder.db.db :as db]
   [stevetrip.swtcg.deck-builder.db.connection :as conn]
   [stevetrip.swtcg.deck-builder.web.error :as error]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; hug initialization

;; Tell HugSQL to use next.jdbc adapter
;; This is needed now that I'm using an actual connection and not a map
(hugsql/set-adapter! (adapter/hugsql-adapter-next-jdbc
                      {:builder-fn rs/as-unqualified-kebab-maps}))

(hugsql/def-db-fns "stevetrip/swtcg/deck_builder/db/sql/cards.sql")
(hugsql/def-db-fns "stevetrip/swtcg/deck_builder/db/sql/decks.sql")
(hugsql/def-db-fns "stevetrip/swtcg/deck_builder/db/sql/packs.sql")
(hugsql/def-sqlvec-fns "stevetrip/swtcg/deck_builder/db/sql/decks.sql")
(hugsql/def-sqlvec-fns "stevetrip/swtcg/deck_builder/db/sql/cards.sql")

(declare search-cards)
(declare get-card-by-id)
(declare insert-deck!)
(declare insert-card-to-deck!)
(declare get-deck-by-id)
(declare get-decks)
(declare get-deck-by-name)
(declare get-deck-cards)
(declare delete-deck!)
(declare enable-foreign-keys!)
(declare remove-card-from-deck!)
(declare remove-all-cards-from-deck!)
(declare insert-card!)
(declare get-cards-by-set-and-rarity)
(declare insert-pack-opening!)
(declare get-pack-opening-by-id)
(declare upsert-collection-card!)
(declare get-collection)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; implementations

(defn- add-deck*
  [db deck]
  (try
    (insert-deck! db (assoc deck :deck-id (str (random-uuid))))
    (catch org.sqlite.SQLiteException e
      (if (re-find #"(?i)unique" (ex-message e))
        (throw
         (error/conflict "A deck with that name already exists."
                         {:name (:name deck)}))
        (throw e)))))

(defn- open-pack*
  "Atomically logs a pack opening and credits its cards to the owner's collection."
  [db {:keys [owner set-code cards]}]
  (jdbc/with-transaction [tx db]
    (let [opening-id (str (random-uuid))
          row (insert-pack-opening! tx {:opening-id opening-id
                                         :owner owner
                                         :set-code set-code
                                         :card-ids (json/generate-string cards)})]
      (doseq [card-id cards]
        (upsert-collection-card! tx {:owner owner :card-id card-id}))
      ;; :card-ids on `row` is still the raw JSON string from the INSERT ... RETURNING —
      ;; leave it as-is so it goes through `pack-opening->response`'s parse exactly once,
      ;; same as a plain get-pack-opening-by-id read.
      row)))

(defn- pack-opening->response
  [row]
  (some-> row (update :card-ids json/parse-string)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; API

(defn- list-cards*
  [db opts]
  (search-cards db opts))

(defrecord SqliteCardDatabase [db]
  db/CardDatabase
  (get-card-by-id [this id]
    (get-card-by-id db {:card-id id}))
  (list-cards [this opts]
    (list-cards* db opts))

  (add-deck [this deck]
    (add-deck* db deck))

  (get-deck-by-id [this deck-id]
    (get-deck-by-id db {:deck-id deck-id}))

  (list-decks [this]
    (get-decks db))

  (delete-deck [this deck-id]
    (delete-deck! db {:deck-id deck-id})
    ;; FIXME this is a hack because cascade delete not working in sqlite
    (remove-all-cards-from-deck! db {:deck-id deck-id}))

  (get-deck-cards [this deck-id]
    (get-deck-cards db {:deck-id deck-id}))
  (add-card-to-deck [this deck-id card-id quantity]
    (insert-card-to-deck! db {:deck-id deck-id :card-id card-id :quantity quantity}))
  (remove-card-from-deck [this deck-id card-id]
    (remove-card-from-deck! db {:deck-id deck-id :card-id card-id}))

  (list-cards-by-set-and-rarity [this set-code rarity]
    (get-cards-by-set-and-rarity db {:set-code set-code :rarity rarity}))
  (record-pack-opening [this opening]
    (pack-opening->response (open-pack* db opening)))
  (get-pack-opening-by-id [this opening-id]
    (pack-opening->response (get-pack-opening-by-id db {:opening-id opening-id})))
  (get-collection [this owner]
    (get-collection db {:owner owner})))

(defmethod db/create-database :sqlite
  [connection]
  (->SqliteCardDatabase (conn/get-db connection)))

(defrecord SqliteConnection [datasource db-path]
  conn/ConnectionProvider
  (get-db [_] datasource)
  (db-type [_] :sqlite)
  (close [_]
    ;; next.jdbc handles closing connections for us, keep around for potential future pools
    nil))

(defmethod conn/connect :sqlite
  [conn-str]
  (let [{:keys [path]} (conn/parse-connection-string conn-str)
        jdbc-url (str "jdbc:sqlite:" path)
        datasource (jdbc/get-datasource {:jdbcUrl jdbc-url})]

    ;; configure sqlite here
    (jdbc/execute! datasource ["PRAGMA foreign_keys = ON"])
    (jdbc/execute! datasource ["PRAGMA journal_mode = WAL"])

    (->SqliteConnection datasource path)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; dev
(comment
  (def connection (conn/connect "sqlite://cards.db"))
  (def db (db/create-database connection))
  db
  (:datasource connection)
  (db/get-deck-cards db 1)
  (db/list-cards db {:search "critical" :side "L" :set_code "AOTC"})
  (get-card-by-id db {:card_id 1})
  #_())
