(ns swtcg.data.sqlite
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [hugsql.core :as hugsql]))

(defn parse-int [s]
  (when (and s (not-empty s))
    (Integer/parseInt s)))

(defn process-card [card]
  (-> card
      (update :Cost parse-int)
      (update :Speed parse-int)
      (update :Power parse-int)
      (update :Health parse-int)
      (update :Number parse-int)
      (update :Usage #(if (empty? %) nil %))
      (update :Script #(if (empty? %) nil %))))

(defn read-tsv [filename]
  (with-open [reader (io/reader filename)]
    (let [rows (csv/read-csv reader :separator \tab)
          headers (map keyword (first rows))
          data (rest rows)]
      (mapv #(zipmap headers %) data))))

(defn read-cards
  [filename]
  (map process-card (read-tsv filename)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; load into sqlite

(hugsql/def-db-fns "swtcg/data/sql/cards.sql")
(hugsql/def-db-fns "swtcg/data/sql/decks.sql")
(hugsql/def-sqlvec-fns "swtcg/data/sql/decks.sql")
(hugsql/def-sqlvec-fns "swtcg/data/sql/cards.sql")

(def db {:dbtype "sqlite" :dbname "cards.db"})

(comment
  (not-empty [:foo])
  (read-cards "resources/public/AOTC.txt")
  (insert-cards (read-cards "resources/public/AOTC.txt"))
  (create-card-table)
  (create-decks-table)
  (create-cards-to-deck-table)
  (search-cards-sqlvec {:side "D"})

  #_())
