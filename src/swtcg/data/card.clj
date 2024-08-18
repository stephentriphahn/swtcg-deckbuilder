(ns swtcg.data.card
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def num-fields #{:number :cost :health :power :speed})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; memory impl helpers

(defonce db (atom []))

(defn create-path
  [set-name]
  (str "/Users/stephentriphahn/development/SWTCG-LACKEY/starwars/sets/" set-name ".txt"))

(def sets-to-load #{"AOTC" "SR" "BOY" "ANH" "ESB" "ROTS" "ROTJ" "PM" "RAS"})
(def aotc-file "/Users/stephentriphahn/development/SWTCG-LACKEY/starwars/sets/AOTC.txt")
(def anh-file "/Users/stephentriphahn/development/SWTCG-LACKEY/starwars/sets/ANH.txt")

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
  (->> row split-tab (zipmap headers) (parse-int-fields num-fields) normalize))

(defn- load-file!
  [path]
  (with-open [rdr (io/reader path)]
    (let [[fields & r] (line-seq rdr)
          headers (create-headers fields)
          cards (mapv #(row->card headers %) r)]
      (swap! db concat cards))))

(def kw-to-comparator {:eq = :ne (complement =) :lt < :gt > :lte <= :gte >=})

(defn- comparator-filter
  [field m]
  (let [v (first (vals m))
        c (kw-to-comparator (first (keys m)))]
    #(when (field %) (c (field %) v))))

(defn- param->filter
  [[field-kw v]]
  (cond
    (not (map? v)) #(= (field-kw %) v)
    (map? v) (comparator-filter field-kw v)
    :else (constantly true)))

(defn build-filter-fn
  [params]
  (apply every-pred (map param->filter params)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; connect and memory implementation

(defprotocol CardDatabase
  (list-all [this opts]))

(defn new-memory-db
  []
  (reify CardDatabase
    (list-all [_ opts]
      (let [filter-opts (dissoc opts :skip :limit)]
        (cond->> @db
          (not-empty filter-opts) (filter (build-filter-fn filter-opts))
          (:limit opts) (take (:limit opts)))))))

(defmulti connect #(first (clojure.string/split % #"://")))

(defmethod connect "memory"
  [connect-string]
  (new-memory-db))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; development

(comment
  (reset! db [])
  (run! load-file! (map create-path sets-to-load)) ;; pre-load data
  (def test-params {:speed {:gt 50} :side {:ne "N"} :cost {:lte 5}})
  (filter (build-filter-fn test-params) @db)
  (gensym)
  #_())
