(ns swtcg.system
  (:require
   [integrant.core :as integrant]
   [ring.adapter.jetty :as jetty]
   [swtcg.config :as config]
   [swtcg.db.db :as db]
   [swtcg.db.memory]
   [swtcg.db.migratus :as mig]
   [swtcg.db.sqlite]
   [swtcg.web.routes :as routes]
   [swtcg.db.connection :as conn]))

(defn config->system-map
  [config]
  {::app {:db (integrant/ref ::db)}
   ::connection {:conn-str (config/card-db-cs config)}
   ::db {:connection (integrant/ref ::connection)}
   ::migrations {:db (integrant/ref ::db)
                 :conn-str (config/card-db-cs config)}
   ::server {:app (integrant/ref ::app)
             :port 3000
             :join? false}})

(defmethod integrant/init-key ::app
  [_ {:keys [db]}]
  (routes/app db))

(defmethod integrant/init-key ::server
  [_ {:keys [app] :as args}]
  (jetty/run-jetty app (dissoc args :handler)))

(defmethod integrant/init-key ::connection
  [_ {:keys [conn-str]}]
  (conn/connect conn-str))

(defmethod integrant/halt-key! ::connection
  [_ connection]
  (conn/close connection))

(defmethod integrant/init-key ::db
  [_ {:keys [connection]}]
  (db/create-database connection))

(defmethod integrant/init-key ::migrations
  [_ {:keys [conn-str]}]
  (mig/migrate! conn-str))

(defmethod integrant/halt-key! ::server
  [_ server]
  (.stop server))

(comment
  (def system (integrant/init (config->system-map (config/read-config))))
  system
  (integrant/halt! system)
  #_())
