(ns swtcg.system
  (:require [integrant.core :as integrant]
            [ring.adapter.jetty :as jetty]
            [swtcg.web.routes :as routes]
            [swtcg.data.card :as card]))

(def config {::app {:db (integrant/ref ::db)}
             ::db {:conn-str "sqlite://cards.db"}
             ::server {:app (integrant/ref ::app)
                       :port 3000
                       :join? false}})

(defmethod integrant/init-key ::app
  [_ {:keys [db]}]
  (routes/app db))

(defmethod integrant/init-key ::server
  [_ {:keys [app] :as args}]
  (jetty/run-jetty app (dissoc args :handler)))

(defmethod integrant/init-key ::db
  [_ {:keys [conn-str]}]
  (card/connect conn-str))

(defmethod integrant/halt-key! ::server
  [_ server]
  (.stop server))

(comment
  (def system (integrant/init config))
  system
  (integrant/halt! system)
  #_())
