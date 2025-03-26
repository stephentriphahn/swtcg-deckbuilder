(ns swtcg.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [malli.core :as malli]
            [malli.error :as malli-error]))

(def ConfigSchema
  [:map
   [:swtcg-card-db-cs [:string {:min 1}]]])

(defn validate-config! [config]
  (if (malli/validate ConfigSchema config)
    config
    (throw (ex-info "Invalid config"
                    {:errors (malli-error/humanize (malli/explain ConfigSchema config))}))))

(defn read-config
  []
  (-> "config.edn"
      io/resource
      aero/read-config
      validate-config!))

(defn card-db-cs
  [config]
  (:swtcg-card-db-cs config))

(comment
  (read-config)
  (System/getenv "SWTCG_CARD_DB_CS")
  #_())
