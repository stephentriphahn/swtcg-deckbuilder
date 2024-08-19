(ns swtcg.data.card
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [swtcg.data.protocols :as protocols]))

(def num-fields #{:number :cost :health :power :speed})
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

(defn new-memory-repo
  [db]
  (reify protocols/Repo
    (get-by-id [_ id]
      (-> @db :cards (get id)))
    (list-all [_ opts]
      (let [filter-opts (dissoc opts :skip :limit)
            cards (-> @db :cards vals)]
        (cond->> cards
          (not-empty filter-opts) (filter (build-filter-fn filter-opts))
          (:limit opts) (take (:limit opts)))))
    (delete-by-id [_ id]
      (throw (ex-info "Not Implemented")))
    (update-by-id [_ id body]
      (throw (ex-info "Not Implemented")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; development

(comment
  (def test-db (atom {:decks {} :cards {}}))
  (run! (partial load-file! test-db) (map create-path sets-to-load)) ;; pre-load data
  (def test-params {:speed {:gt 50} :side {:ne "N"} :cost {:lte 5}})
  (filter (build-filter-fn test-params) @test-db)
  #_())
