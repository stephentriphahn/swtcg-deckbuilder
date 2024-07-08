(ns swtcg.data.card
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def num-fields #{:number :cost :health :power :speed})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; memory impl helpers

(defonce db (atom []))

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
    (Integer/parseInt x)))

(defn parse-int-fields
  [number-fields card]
  (reduce #(update %1 %2 normalize-int) card number-fields))

(defn- row->card
  [headers row]
  (->> row split-tab (zipmap headers) (parse-int-fields num-fields)))

(defn- load-file!
  [path]
  (with-open [rdr (io/reader path)]
    (let [[fields & r] (line-seq rdr)
          headers (create-headers fields)
          cards (doall (map #(row->card headers %) r))]
      (swap! db concat cards))))

(def kw-to-comparator {:lt < :gt > :lte <= :gte >=})

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
  [connect-protocol]
  (new-memory-db))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; development

(comment
  (run! load-file! [aotc-file anh-file]) ;; pre-load data
  (def test-params {:speed {:gt 50} :cost {:lte 5}})
  (filter (build-filter-fn test-params) @db)
  #_())
