(ns swtcg.web.routes
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
            [swtcg.web.handlers :as handlers]
            [swtcg.web.middleware :as mw]
            [swtcg.web.schema :as schema]))

(def cards-routes
  ["/cards" {:swagger {:tags ["cards"]}}
   [""
    {:get {:summary "list all cards in the system"
           :handler handlers/list-cards}}]
   ["/:id" {:name ::card-by-id}
    ["" {:get {:summary "get a card by id"
               :parameters {:path {:id number?}}
               :handler handlers/get-card-by-id}}]]])
(def deck-routes
  ["/decks" {:swagger {:tags ["decks"]}}

   ["" {:get {:summary "List all decks"
              :handler handlers/list-decks}
        :post {:summary "Create a new deck"
               :parameters {:body schema/CreateDeckRequest}
               :handler handlers/create-deck}}]

   ["/:deck-id" {:name ::deck-by-id
                 :get {:summary "Get a deck by ID"
                       :parameters {:path {:deck-id int?}}
                       :handler handlers/get-deck-by-id}
                 :delete {:summary "Delete a deck"
                          :parameters {:path {:id int?}}
                          :handler handlers/delete-deck}}]

   ["/:deck-id/cards" {:post {:summary "Add multiple cards to deck"
                              :parameters {:path {:deck-id int?}
                                           :body schema/AddCardToDeck}
                              :handler handlers/add-cards-to-deck}}]

   ["/:deck-id/cards/:card-id" {:delete {:summary "Remove card from deck"
                                         :parameters {:path {:deck-id int?
                                                             :card-id int?}}
                                         :handler handlers/remove-card-from-deck}
                                :put {:summary "Add card to deck"
                                      :parameters {:path {:id int?}
                                                   :body schema/AddCardToDeck}
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
