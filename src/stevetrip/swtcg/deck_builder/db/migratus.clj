(ns stevetrip.swtcg.deck-builder.db.migratus
  (:require [migratus.core :as migratus]
            [stevetrip.swtcg.deck-builder.db.db :as db]))

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
  (def cfg (cs->migratus-config "sqlite://cards.db"))
  cfg
  (migrate! "sqlite://foo.db")
  (reset-db! "sqlite://cards.db")
  #_())
