(ns swtcg.web.routes
  (:require [reitit.core :as reitit]
            [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [ring.middleware.nested-params :as nested]
            [ring.middleware.keyword-params :as kw-params]
            [reitit.ring.middleware.parameters :as param-mw]
            [muuntaja.core :as m]
            [swtcg.web.handlers :as handlers]
            [swtcg.web.middleware :as mw]
            [ring.adapter.jetty :as jetty]))

(def routes
  [["/heartbeat"
    {:get (fn [req] {:status 200 :body "ok"})}]
   ["/swagger.json"
    {:get {:no-doc true
           :handler (swagger/create-swagger-handler)}}]
   ["/api/v1"
    ["/cards" {:name ::cards
               :swagger {:tags ["cards"]}}
     [""
      {:get {:summary "list all cards in the system"
             :handler handlers/list-cards}}]]]])

(defn app [db]
  (ring/ring-handler
   (ring/router routes
                {:data {:db db
                        :muuntaja m/instance
                        :middleware [swagger/swagger-feature
                                     muuntaja/format-middleware
                                     param-mw/parameters-middleware
                                     mw/nested-params
                                     mw/keyword-params
                                     mw/add-db]}})

   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/docs"})
    (ring/create-file-handler {:path "/" :root "resources"})
    (ring/create-default-handler))))

(comment
  (def server (start))
  (.stop server)
  #_())
