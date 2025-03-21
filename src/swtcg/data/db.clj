(ns swtcg.data.db
  (:require
   [clojure.string :as str]
   [swtcg.data.memory :as memory]
   [swtcg.data.sqlite :as sqlite]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; connect and memory implementation

(defprotocol CardDatabase
  (list-all [this opts])
  (get-by-id [this id]))

(defmulti connect #(first (str/split % #"://")))

(defmethod connect "memory"
  [connect-string]
  (reify CardDatabase
    (get-by-id [_ id]
      (memory/get-card-by-id id))
    (list-all [_ opts]
      (memory/list-all opts))))

(defmethod connect "sqlite"
  [connect-string]
  (let [db {:dbtype "sqlite" :dbname "cards.db"}]
    (reify CardDatabase
      (get-by-id [this id]
        (sqlite/search-cards db {:id id}))
      (list-all [this opts]
        (sqlite/search-cards db opts)))))
