(ns swtcg.tools.load-cards
  (:require [hugsql.core :as hugsql]
            [swtcg.log :as log]
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
          [headers & data] (map (comp keyword string/lower-case) (first rows))]
      (mapv #(zipmap headers %) data))))

(defn read-cards
  [filename]
  (map process-card (read-tsv filename)))

(hugsql/def-db-fns "swtcg/db/sql/cards.sql")

(declare insert-card!)
(declare get-all-loaded-sets)
(defonce original-sets #{"AOTC" "SR" "ANH" "BOY" "ESB" "RAS" "JG" "ROTJ" "PM" "ROTS"})

(defn load-cards-cli
  "Call with a comma separated list of set names to add."
  [{:keys [sets dbname dbtype]
    :or {dbtype "sqlite" dbname "cards.db"}}]
  (let [sets-to-add (if sets (string/split sets #",") original-sets)
        existing-sets (set (map :set_code (get-all-loaded-sets {:dbname dbname :dbtype dbtype})))
        insert (partial insert-card! {:dbname dbname :dbtype dbtype})]
    (mapv
     (fn [set_code]
       (if-let [s (existing-sets set_code)]
         (log/info "skipping set, already loaded" {:set_code s})
         (mapv insert (read-cards (str "resources/public/sets/" set_code ".txt")))))
     sets-to-add)))

(comment
  (load-cards-cli {:dbname "cards.db" :dbtype "sqlite" :sets "BOE,BOH"})
  #_())
