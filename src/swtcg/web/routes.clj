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

(def DeckRequestSchema
  [:map
   [:name [:string {:min 1}]]
   [:owner [:string {:min 1}]]
   [:format [:string {:min 1}]]
   [:side [:enum "L" "D"]]]) ;; Light/Dark

(def AddCardToDeckSchema
  [:map
   [:card-id int?]
   [:quantity [:int {:min 1 :max 4}]]])

(def routes
  [["/heartbeat"
    {:get (fn [req] {:status 200 :body "ok"})}]
   ["/swagger.json"
    {:get {:no-doc true
           :handler (swagger/create-swagger-handler)}}]
   ["/api/v1"
    ["/cards" {:swagger {:tags ["cards"]}}
     [""
      {:get {:summary "list all cards in the system"
             :handler handlers/list-cards}}]
     ["/:id" {:name ::card-by-id}
      ["" {:get {:summary "get a card by id"
                 :parameters {:path {:id number?}}
                 :handler handlers/get-card-by-id}}]]]
    ["/decks" {:swagger {:tags ["decks"]}}

 ;; GET + POST combined
     ["" {:get {:summary "List all decks"
                :handler handlers/list-decks}
          :post {:summary "Create a new deck"
                 :parameters {:body DeckRequestSchema}
                 :handler handlers/create-deck}}]

 ;; /:id GET + DELETE combined
     ["/:id" {:name ::deck-by-id
              :get {:summary "Get a deck by ID"
                    :parameters {:path {:id int?}}
                    :handler handlers/get-deck-by-id}
              :delete {:summary "Delete a deck"
                       :parameters {:path {:id int?}}
                       :handler handlers/delete-deck}}]

 ;; Add card to deck
     ["/:id/cards" {:post {:summary "Add card to deck"
                           :parameters {:path {:id int?}
                                        :body AddCardToDeckSchema}
                           :handler handlers/add-card-to-deck}}]

 ;; Remove card from deck
     ["/:id/cards/:card-id" {:delete {:summary "Remove card from deck"
                                      :parameters {:path {:id int?
                                                          :card-id int?}}
                                      :handler handlers/remove-card-from-deck}}]]]])

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
