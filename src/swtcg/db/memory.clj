(ns swtcg.db.memory
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [swtcg.db.connection :as conn]
   [swtcg.db.db :as db]))

(def num-fields #{:number :cost :health :power :speed})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; memory impl helpers
(defn create-load-path
  [set-name]
  (str "/Users/stephentriphahn/development/SWTCG-LACKEY/starwars/sets/" set-name ".txt"))

(def sets-to-load #{"AOTC" "SR" "BOY" "ANH" "ESB" "ROTS" "ROTJ" "PM" "RAS"})

(defn split-tab
  [header-string]
  (string/split header-string #"\t"))

(defn create-headers
  [header-string]
  (->> header-string
       split-tab
       (map string/lower-case)
       (map keyword)))

(defn normalize-int
  [x]
  (when-not (string/blank? x)
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
      (assoc :card_id (str set number))
      (update :type (comp keyword string/lower-case))
      (update :imagefile build-image-path (:set row))))

(defn- row->card
  [headers row]
  (let [card (->> row split-tab (zipmap headers) (parse-int-fields num-fields) normalize)]
    (vector (:card_id card) card)))

(defn- load-file!
  [db path]
  (with-open [rdr (io/reader path)]
    (let [[fields & r] (line-seq rdr)
          headers (create-headers fields)
          cards (into {} (mapv #(row->card headers %) r))]
      (swap! db update :cards merge cards))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; filter list

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

(defn list-cards*
  [state opts]
  (let [filter-opts (dissoc opts :skip :limit)
        cards-vec (-> @state :cards vals)]
    (cond->> cards-vec
      (not-empty filter-opts) (filter (build-filter-fn filter-opts))
      (:limit opts) (take (:limit opts))
      :always (map #(select-keys % [:id :imagefile :name])))))

(defrecord MemoryCardDatabase [state]
  db/CardDatabase
  (get-card-by-id [_ id]
    (-> @state :cards (get id)))
  (list-cards [_ opts]
    (list-cards* state opts)))

(defrecord MemoryConnection [state]
  conn/ConnectionProvider
  (get-db [_] state)
  (db-type [_] :memory)  ; Simple!
  (close [_]
    (reset! state {:decks {} :cards {} :cards-to-deck {}})))

(defmethod conn/connect :memory
  [_conn-str]
  (let [initial-state {:decks {}
                       :cards {}
                       :cards-to-deck {}}]
    (->MemoryConnection (atom initial-state))))

(defmethod db/create-database :memory
  [connection]
  (->MemoryCardDatabase (conn/get-db connection)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; development

(comment
  (def test-db-atom (atom {}))
  (def db (->MemoryCardDatabase test-db-atom))
  (run! (partial load-file! test-db-atom) (map create-load-path sets-to-load)) ;; pre-load data
  (def test-params {:speed {:gt 50} :side {:ne "N"} :cost {:lte 5}})
  @test-db-atom

  (filter (build-filter-fn test-params) (vals (:cards @test-db-atom)))
  (into {} [[:foo "bar"]])
  #_())
