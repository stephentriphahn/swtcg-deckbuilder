(ns swtcg.data.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [swtcg.data.card :as card]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helpers to initialize memory-db data from file system

(def num-fields #{:number :cost :health :power :speed})

(defn create-path
  [set-name]
  (str "/Users/stephentriphahn/development/SWTCG-LACKEY/starwars/sets/" set-name ".txt"))

(def sets-to-load #{"AOTC" "SR" "BOY" "ANH" "ESB" "ROTS" "ROTJ" "PM" "RAS"})

(defn split-tab
  [header-string]
  (str/split header-string #"\t"))

(defn create-headers
  [header-string]
  (->> header-string
       split-tab
       (map str/lower-case)
       (map keyword)))

(defn normalize-int
  [x]
  (when-not (str/blank? x)
    (try
      (Integer/parseInt x)
      (catch Exception e
        (println (.getMessage e))))))

(defn parse-int-fields
  [number-fields card]
  (reduce #(update %1 %2 normalize-int) card number-fields))

(defn normalize
  [{:keys [set number] :as row}]
  (-> row
      (assoc :id (str set number))
      (update :type (comp keyword str/lower-case))))

(defn- row->card
  [headers row]
  (let [card (->> row split-tab (zipmap headers) (parse-int-fields card/num-fields) normalize)]
    [(:id card) card]))

(defn- load-file!
  [db path]
  (with-open [rdr (io/reader path)]
    (let [[fields & r] (line-seq rdr)
          headers (create-headers fields)
          cards (into {} (mapv #(row->card headers %) r))]
      (swap! db update :cards merge cards))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;  databse connections

;; TODO figure out a better way to conceal this.  Useful for dev to have it global
(def memory-db (atom {:cards {} :decks {}}))

(defmulti connect #(first (clojure.string/split % #"://")))

(defmethod connect "memory"
  [connect-string]
  {:card-repo (card/new-memory-repo memory-db)})

(comment
  (def test-db (atom {:cards {} :decks {}}))
  (reset! test-db {:cards {} :decks {}})
  (run! (partial load-file! memory-db) (map create-path sets-to-load))
  @test-db
  #_())
