(ns stevetrip.swtcg.deck-builder.tools.export-catalog
  "Exports the whole `cards` table out of cards.db into a single deterministic EDN
  file, for consumers (e.g. swtcg-game-engine) that want real card data without a
  code dependency on this project. See doc/catalog-export.md for the full spec;
  this namespace only does hygiene normalization, never card-meaning interpretation
  (type/subtype parsing, side mapping, etc. stay out of scope - see that doc's
  \"Not this script's job\")."
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as adapter]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [stevetrip.swtcg.deck-builder.log :as log]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(hugsql/def-db-fns "stevetrip/swtcg/deck_builder/db/sql/cards.sql"
                   {:adapter (adapter/hugsql-adapter-next-jdbc
                              {:builder-fn rs/as-unqualified-kebab-maps})})

(declare search-cards)

(def ^:private numeric-sentinel-fields
  "cost/speed/power/health share load-cards.clj's parse-int, which coerces an
  unparseable-but-non-blank value (\"X\", \"*\", a lone \" \") to -1. That's a real
  landmine for these four - they feed game arithmetic - so the export maps -1 back
  to nil here. `number` uses the same -1 sentinel but is purely cosmetic, never used
  in arithmetic, so it's deliberately left alone (not included in this set)."
  [:cost :speed :power :health])

(def ^:private valid-sides
  "The `cards` table's own CHECK (side IN ('L','D','N')) should already guarantee
  this, but original-sets never includes HELP (reminder-text rows, blank side) or
  SBS (side = \"Y\"), so this is a defensive filter in case a wider :sets load ever
  reaches this export."
  #{"L" "D" "N"})

(defn- trim-strings
  "Trims every string-valued field on a card row. Real data has trailing whitespace
  on `type` and other fields; untrimmed values would silently fail an exact-match
  `case`/`=` downstream."
  [card]
  (into {} (map (fn [[k v]] [k (if (string? v) (string/trim v) v)])) card))

(defn- nil-numeric-sentinels
  [card]
  (reduce (fn [card field]
            (update card field #(when (not= % -1) %)))
          card
          numeric-sentinel-fields))

(defn normalize-card
  "Hygiene only, not interpretation: trims strings, and turns the load-cards -1
  parse-failure sentinel into nil for the four stat fields that feed game
  arithmetic (`number`'s -1 sentinel is left alone - see doc/catalog-export.md)."
  [card]
  (-> card trim-strings nil-numeric-sentinels))

(defn- valid-side? [card]
  (contains? valid-sides (:side card)))

(defn export-edn-str
  "Renders normalized, side-filtered, card-id-sorted cards as one EDN form,
  {:cards [...]}, one card map per line so the file is easy to diff."
  [cards]
  (str "{:cards\n [" (string/join "\n  " (map pr-str cards)) "]}\n"))

(defn export-catalog-cli
  "Exports the whole cards table to an EDN file. Options: :dbname (default
  cards.db), :out-path (default resources/catalog-export.edn)."
  [{:keys [dbname out-path]
    :or {dbname "cards.db"
         out-path "resources/catalog-export.edn"}}]
  (let [ds (jdbc/get-datasource {:dbtype "sqlite" :dbname dbname})
        raw (search-cards ds {})
        normalized (map normalize-card raw)
        {valid true invalid false} (group-by valid-side? normalized)
        cards (sort-by :card-id valid)]
    (when (seq invalid)
      (log/warn :filtered-invalid-side-cards
                {:count (count invalid)
                 :sides (frequencies (map :side invalid))}))
    (io/make-parents out-path)
    (spit out-path (export-edn-str cards))
    (log/info :exported-catalog {:out-path out-path
                                  :cards (count cards)
                                  :filtered (count invalid)})
    cards))

(comment
  (export-catalog-cli {:dbname "cards.db"})
  #_())
