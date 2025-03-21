(ns swtcg.data.memory
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def num-fields #{:number :cost :health :power :speed})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; memory impl helpers

(defonce db (atom {}))

(defn create-load-path
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

(defn build-image-path
  [image-name set-name]
  (str "/public/setimages/" set-name "/" image-name ".jpg"))

(defn normalize
  [{:keys [set number] :as row}]
  (-> row
      (assoc :id (str set number))
      (update :type (comp keyword str/lower-case))
      (update :imagefile build-image-path (:set row))))

(defn- row->card
  [headers row]
  (let [card (->> row split-tab (zipmap headers) (parse-int-fields num-fields) normalize)]
    (vector (:id card) card)))

(defn- load-file!
  [path]
  (with-open [rdr (io/reader path)]
    (let [[fields & r] (line-seq rdr)
          headers (create-headers fields)
          cards (into {} (mapv #(row->card headers %) r))]
      (swap! db update :cards merge cards))))

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
;;; API
(defn get-card-by-id
  [id]
  (-> @db :cards (get id)))

(defn list-all
  [opts]
  (let [filter-opts (dissoc opts :skip :limit)
        cards-vec (-> @db :cards vals)]
    (cond->> cards-vec
      (not-empty filter-opts) (filter (build-filter-fn filter-opts))
      (:limit opts) (take (:limit opts))
      :always (map #(select-keys % [:id :imagefile :name])))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; development

(comment
  (reset! db {})
  (run! load-file! (map create-load-path sets-to-load)) ;; pre-load data
  (def test-params {:speed {:gt 50} :side {:ne "N"} :cost {:lte 5}})
  (->> @db
       :cards
       vals)
  (filter (build-filter-fn test-params) @db)
  (into {} [[:foo "bar"]])
  #_())
