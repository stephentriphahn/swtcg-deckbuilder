(ns swtcg.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defn read-config
  []
  (-> "config.edn"
      io/resource
      aero/read-config))

(defn card-db-cs
  [config]
  (:swtcg-card-db-cs config))

(comment
  (read-config)
  (System/getenv "SWTCG_CARD_DB_CS")
  #_())
