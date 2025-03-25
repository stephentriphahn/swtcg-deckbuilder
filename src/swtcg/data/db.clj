(ns swtcg.data.db
  (:require
   [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; connect and memory implementation

(defprotocol CardDatabase
  (list-cards [this opts])
  (get-card-by-id [this id]))

(defmulti connect #(first (str/split % #"://")))
