(ns stevetrip.swtcg.deck-builder.tools.load-cards
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as adapter]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [stevetrip.swtcg.deck-builder.db.migratus :as migratus]
            [stevetrip.swtcg.deck-builder.log :as log]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn parse-int [s]
  (when (and s (not-empty s))
    (try
      (Integer/parseInt s)
      (catch Exception e
        ;; some cards have * indicating a dynamic value
        (log/warn :card-number-parse-error {:value s})
        -1))))

(defn card-id
  "Deterministic short id for a card: the first 12 hex chars of the SHA-1 of its
  image file name. Stable across database rebuilds, so decks keep pointing at
  the same cards. A collision fails the insert (primary key)."
  [image-file]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-1")
                        (.getBytes ^String image-file "UTF-8"))]
    (subs (apply str (map #(format "%02x" %) digest)) 0 12)))

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
      ;; associng to add sql param names, ie set -> set-code and imagefile to image-file
        (assoc :set-code (:set card))
        (assoc :image-file (:imagefile card))
        (assoc :card-id (card-id (:imagefile card))))
    (catch Exception e
      (log/error :process-card-error {:card card} e))))

(defn read-tsv [filename]
  (log/info :reading-tsv-file {:filename filename})
  ;; Windows-1252, not the default UTF-8: at least one set's raw bytes contain a 0x96
  ;; (Windows-1252 EN DASH) inside Subtype/Type text, which isn't valid UTF-8 on its own
  ;; and would otherwise be silently decoded as the Unicode replacement character.
  ;; Windows-1252 is a superset of ISO-8859-1 everywhere else, so it's the strictly safer
  ;; choice of the two for whatever else these TSVs contain.
  (with-open [reader (io/reader filename :encoding "windows-1252")]
    ;; card text contains literal double quotes, so disable csv quoting
    (let [[header & data] (csv/read-csv reader :separator \tab :quote \u0001)
          headers (map (comp keyword string/lower-case) header)]
      (mapv #(zipmap headers %) data))))

(defn read-cards
  [filename]
  (map process-card (read-tsv filename)))

(hugsql/def-db-fns "stevetrip/swtcg/deck_builder/db/sql/cards.sql"
                   {:adapter (adapter/hugsql-adapter-next-jdbc
                              {:builder-fn rs/as-unqualified-kebab-maps})})

(declare insert-card!)
(declare get-all-loaded-sets)
(defonce original-sets #{"AOTC" "SR" "ANH" "BOY" "ESB" "RAS" "JG" "ROTJ" "PM" "ROTS"})

(defn load-cards-cli
  "Migrates the database (creating it if needed), then loads card sets.
  Options: :sets comma separated set codes (default: original sets),
  :dbname (default cards.db). Sets already in the database are skipped."
  [{:keys [sets dbname]
    :or {dbname "cards.db"}}]
  (migratus/migrate! (str "sqlite://" dbname))
  (let [sets-to-add (if sets (string/split (str sets) #",") original-sets)
        ds (jdbc/get-datasource {:dbtype "sqlite" :dbname dbname})
        existing-sets (set (map :set-code (get-all-loaded-sets ds)))]
    (jdbc/with-transaction [tx ds]
      (doseq [set-code sets-to-add]
        (if (existing-sets set-code)
          (log/info :skipping-set-already-loaded {:set-code set-code})
          (let [cards (read-cards (str "resources/public/sets/" set-code ".txt"))]
            (run! #(insert-card! tx %) cards)
            (log/info :loaded-set {:set-code set-code :cards (count cards)})))))))

(comment
  (load-cards-cli {:dbname "cards.db" :sets "BOE,BOH"})
  #_())
