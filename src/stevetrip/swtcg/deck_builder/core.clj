(ns stevetrip.swtcg.deck-builder.core
  (:require [ring.adapter.jetty :as jetty]
            [stevetrip.swtcg.deck-builder.web.routes :as routes])
  (:gen-class))

(defn -main [& args]
  (jetty/run-jetty #'routes/app {:port 8080}))

(comment
  (def server (jetty/run-jetty #'routes/app {:port 8080 :join? false}))
  (.stop server)
  #_())
