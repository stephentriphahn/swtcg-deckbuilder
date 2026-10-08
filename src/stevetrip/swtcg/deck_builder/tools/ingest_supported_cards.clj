(ns stevetrip.swtcg.deck-builder.tools.ingest-supported-cards
  "Syncs cards.is_supported from swtcg-game-engine's resources/supported-cards.edn - a card is
  marked supported iff it appears in that file's `:supported` list (every printed keyword and
  ability on it is implemented by the engine today; see that file's own `:meaning` for the full
  set of categories, e.g. `:excluded` cards are a stricter, separate thing and are never marked
  supported here). Idempotent: resets every row to unsupported, then marks only the ids the file
  lists, so a card the engine has regressed on going forward is fixed too."
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as adapter]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [stevetrip.swtcg.deck-builder.log :as log]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(hugsql/def-db-fns "stevetrip/swtcg/deck_builder/db/sql/cards.sql"
                   {:adapter (adapter/hugsql-adapter-next-jdbc
                              {:builder-fn rs/as-unqualified-kebab-maps})})

(declare reset-all-supported!)
(declare mark-cards-supported!)

(def default-path "../swtcg-game-engine/resources/supported-cards.edn")

(defn read-supported-ids
  "Pulls just the card-ids out of the file's `:supported` list - the other top-level lists
  (`:excluded`, and whatever `:counts` implies about unsupported/partial cards) are deliberately
  not touched by this ingestion; see this namespace's docstring."
  [path]
  (let [{:keys [supported]} (edn/read-string (slurp path))]
    (mapv :card-id supported)))

(defn ingest-supported-cards-cli
  "Options: :dbname (default cards.db), :path (default ../swtcg-game-engine/resources/supported-cards.edn,
  i.e. run from this project's root)."
  [{:keys [dbname path]
    :or {dbname "cards.db"
         path default-path}}]
  (let [ds (jdbc/get-datasource {:dbtype "sqlite" :dbname dbname})
        ids (read-supported-ids path)
        existing (set (map :card-id (jdbc/execute! ds ["SELECT card_id FROM cards"]
                                                     {:builder-fn rs/as-unqualified-kebab-maps})))
        missing (remove existing ids)]
    (when (seq missing)
      (log/warn :supported-card-ids-not-in-db
                {:count (count missing) :card-ids (vec missing)}))
    (jdbc/with-transaction [tx ds]
      (reset-all-supported! tx)
      (when (seq ids)
        (mark-cards-supported! tx {:card-ids ids})))
    (log/info :ingested-supported-cards
              {:path path
               :listed (count ids)
               :matched (- (count ids) (count missing))
               :missing (count missing)})
    {:listed (count ids) :matched (- (count ids) (count missing)) :missing (count missing)}))

(comment
  (ingest-supported-cards-cli {:dbname "cards.db"})
  #_())
