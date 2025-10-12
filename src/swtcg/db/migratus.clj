(ns swtcg.db.migratus
  (:require [migratus.core :as migratus]
            [swtcg.config :as config]
            [swtcg.db.db :as db]))

(def db-config (-> (config/read-config)
                   config/card-db-cs
                   db/parse-connection-string
                   db/parsed-cs->jdbc-config))

(defn cs->migratus-config
  [cs]
  {:store :database
   :migration-dir "resources/migrations"
   :db (-> cs
           db/parse-connection-string
           db/parsed-cs->jdbc-config)})

(defn migrate! [cs]
  (migratus/migrate (cs->migratus-config cs)))

(defn rollback! [cs]
  (migratus/rollback (cs->migratus-config cs)))

(defn reset-db! [cs]
  (migratus/reset (cs->migratus-config cs)))

(comment
  (def cfg (cs->migratus-config (config/read-config)))
  cfg
  (migrate! "sqlite://foo.db")
  (reset-db! "sqlite://cards.db")
  db-config
  #_())
