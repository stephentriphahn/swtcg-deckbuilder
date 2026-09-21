(ns stevetrip.swtcg.deck-builder.web.routes
  (:require [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as param-mw]
            [reitit.coercion.malli]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [ring.middleware.cors :refer [wrap-cors]]
            [muuntaja.core :as m]
            [stevetrip.swtcg.deck-builder.web.handlers :as handlers]
            [stevetrip.swtcg.deck-builder.web.middleware :as mw]
            [stevetrip.swtcg.deck-builder.web.schema :as schema]))

(def cards-routes
  ["/cards" {:swagger {:tags ["cards"]}}
   [""
    {:get {:summary "list all cards in the system"
           :responses {200 {:body schema/ListCardsResponse
                            :description "List of cards"}}
           :parameters {:query schema/ListQueryParams}
           :handler handlers/list-cards}}]
   ["/:card-id" {:name ::card-by-id}
    ["" {:get {:summary "get a card by id"
               :parameters {:path {:card-id string?}}
               :responses {200 {:body schema/CardResponse
                                :description "Card data"}}
               :handler handlers/get-card-by-id}}]]])

(def deck-routes
  ["/decks" {:swagger {:tags ["decks"]}}

   ["" {:get {:summary "List all decks"
              :responses {200 {:body schema/ListDecksResponse}}
              :handler handlers/list-decks}
        :post {:summary "Create a new deck"
               :parameters {:body schema/CreateDeckRequest}
               :handler handlers/create-deck}}]

   ["/:deck-id" {:name ::deck-by-id
                 :get {:summary "Get a deck by ID"
                       :parameters {:path {:deck-id string?}}
                       :responses {200 {:body schema/DeckResponse}}
                       :handler handlers/get-deck-by-id}
                 :delete {:summary "Delete a deck"
                          :parameters {:path {:deck-id string?}}
                          :handler handlers/delete-deck}}]

   ["/:deck-id/cards" {:post {:summary "Add multiple cards to deck"
                              :parameters {:path {:deck-id string?}
                                           :body schema/AddCardsToDeckRequest}
                              :handler handlers/add-cards-to-deck}}]

   ["/:deck-id/cards/:card-id" {:delete {:summary "Remove card from deck"
                                         :parameters {:path {:deck-id string?
                                                             :card-id string?}}
                                         :handler handlers/remove-card-from-deck}
                                :put {:summary "Add card to deck"
                                      :parameters {:path {:deck-id string?
                                                          :card-id string?}
                                                   :body schema/AddCardToDeckRequest}
                                      :handler handlers/add-card-to-deck}}]])

(def routes
  [["/heartbeat"
    {:get (fn [req] {:status 200 :body "ok"})}]
   ["/swagger.json"
    {:get {:no-doc true
           :handler (swagger/create-swagger-handler)}}]
   ["/api/v1"
    cards-routes
    deck-routes]])

(defn app [db]
  (-> (ring/ring-handler
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
        (ring/create-default-handler)))
      (wrap-cors
       :access-control-allow-origin [#"http://localhost:5173"]
       :access-control-allow-methods [:get :post :put :delete :options])))

(comment
  #_())
