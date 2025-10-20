(ns swtcg.db.sqlite
  (:require
   [hugsql.core :as hugsql]
   [hugsql.adapter.next-jdbc :as adapter]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [swtcg.db.db :as db]
   [swtcg.db.connection :as conn]
   [swtcg.web.error :as error]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; hug initialization

;; Tell HugSQL to use next.jdbc adapter
;; This is needed now that I'm using an actual connection and not a map
(hugsql/set-adapter! (adapter/hugsql-adapter-next-jdbc
                      {:builder-fn rs/as-unqualified-kebab-maps}))

(hugsql/def-db-fns "swtcg/db/sql/cards.sql")
(hugsql/def-db-fns "swtcg/db/sql/decks.sql")
(hugsql/def-sqlvec-fns "swtcg/db/sql/decks.sql")
(hugsql/def-sqlvec-fns "swtcg/db/sql/cards.sql")

(declare search-cards)
(declare get-card-by-id)
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; implementations

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; API

(defrecord SqliteCardDatabase [db]
  db/CardDatabase
  (get-card-by-id [this id]
    (get-card-by-id db {:card-id id}))
  (list-cards [this opts]
    (search-cards db opts))

  (add-deck [this deck]
    (add-deck* db deck))

  (get-deck-by-id [this deck-id]
    (get-deck-by-id db {:deck-id deck-id}))

  (delete-deck [this deck-id]
    (delete-deck! db {:deck-id deck-id})
    ;; FIXME this is a hack because cascade delete not working in sqlite
    (remove-all-cards-from-deck! db {:deck-id deck-id}))

  (get-deck-cards [this deck-id]
    (get-deck-cards db {:deck-id deck-id}))
  (add-card-to-deck [this deck-id card-id quantity]
    (insert-card-to-deck! db {:deck-id deck-id :card-id card-id :quantity quantity}))
  (remove-card-from-deck [this deck-id card-id]
    (remove-card-from-deck! db {:deck-id deck-id :card-id card-id})))

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
  (db/list-cards db {:side "D" :cost 5 :set_code "AOTC"})
  (get-card-by-id db {:card_id 1})
  #_())
