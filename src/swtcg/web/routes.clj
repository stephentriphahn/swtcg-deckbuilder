(ns swtcg.web.routes
  (:require [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as param-mw]
            [reitit.coercion.malli]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [muuntaja.core :as m]
            [swtcg.web.handlers :as handlers]
            [swtcg.web.middleware :as mw]))

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
             :handler handlers/list-cards}}]
     ["/:id" {:name ::card-by-id}
      ["" {:get {:summary "get a card by id"
                 :parameters {:path {:id number?}}
                 :handler handlers/get-card-by-id}}]]]]])

(defn app [db]
  (ring/ring-handler
   (ring/router routes
                {:data {:db db
                        :coercion reitit.coercion.malli/coercion
                        :muuntaja m/instance
                        :middleware [swagger/swagger-feature
                                     muuntaja/format-middleware
                                     param-mw/parameters-middleware
                                     mw/nested-params
                                     mw/keyword-params
                                     mw/add-db
                                     exception/exception-middleware
                                     coercion/coerce-exceptions-middleware
                                     coercion/coerce-request-middleware
                                     coercion/coerce-response-middleware
                                     mw/translate-http-error]}})

   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/docs"})
    (ring/create-file-handler {:path "/" :root "resources/public/"})
    (ring/create-default-handler))))

(comment
  #_())
