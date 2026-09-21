(ns swtcg.web.decks-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [muuntaja.core :as m]
            [next.jdbc :as jdbc]
            [swtcg.db.connection :as conn]
            [swtcg.db.db :as db]
            [swtcg.db.migratus :as migratus]
            [swtcg.db.sqlite]
            [swtcg.web.routes :as routes])
  (:import (java.io File)))

(def ^:dynamic *app* nil)

(def luke "aaaaaaaaaaaa")
(def vader "bbbbbbbbbbbb")

(defn- call
  ([method uri] (call method uri nil))
  ([method uri body]
   (let [req (cond-> {:request-method method :uri uri :headers {}}
               body (assoc :body (m/encode "application/json" body)
                           :headers {"content-type" "application/json"}))
         res (*app* req)]
     (cond-> res
       (and (:body res) (not (string? (:body res))))
       (update :body #(m/decode "application/json" %))))))

(defn with-app [f]
  ;; relative name: swtcg.db.db/parsed-cs->jdbc-config reads sqlite paths from the URI host
  (let [name (str "swtcg-decks-test-" (System/nanoTime) ".db")
        cs (str "sqlite://" name)]
    (try
      (migratus/migrate! cs)
      (let [c (conn/connect cs)]
        (doseq [[id n side] [[luke "Luke" "L"] [vader "Vader" "D"]]]
          (jdbc/execute! (conn/get-db c)
                         ["INSERT INTO cards (card_id, name, image_file, side, type) VALUES (?, ?, ?, ?, 'Character')"
                          id n (str n ".jpg") side]))
        (binding [*app* (routes/app (db/create-database c))]
          (f)))
      (finally (doseq [suffix ["" "-wal" "-shm"]] (.delete (File. (str name suffix))))))))

(use-fixtures :each with-app)

(def deck {:name "d" :owner "o" :format "standard" :side "L"})

(deftest deck-lifecycle
  (let [created (call :post "/api/v1/decks" deck)
        id (-> created :body :deck-id)]
    (is (= 201 (:status created)))
    (testing "list"
      (is (string? id))
      (is (= [id] (map :deck-id (:body (call :get "/api/v1/decks"))))))
    (testing "add single card via PUT"
      (let [res (call :put (str "/api/v1/decks/" id "/cards/" luke) {:quantity 2})]
        (is (= 200 (:status res)))
        (is (= {:deck-id id :card-id luke :quantity 2} (:body res)))))
    (testing "add multiple cards"
      (is (= 200 (:status (call :post (str "/api/v1/decks/" id "/cards")
                                [{:card-id luke :quantity 3} {:card-id vader :quantity 1}])))))
    (testing "get includes cards and id"
      (let [body (:body (call :get (str "/api/v1/decks/" id)))]
        (is (= id (:deck-id body)))
        (is (= #{[luke 3] [vader 1]} (set (map (juxt :card-id :quantity) (:cards body)))))))
    (testing "missing card/deck -> 404, nothing partially inserted"
      (is (= 404 (:status (call :put (str "/api/v1/decks/" id "/cards/nope") {:quantity 1}))))
      (is (= 404 (:status (call :put (str "/api/v1/decks/nope/cards/" luke) {:quantity 1}))))
      (is (= 404 (:status (call :post (str "/api/v1/decks/" id "/cards")
                                [{:card-id luke :quantity 4} {:card-id "nope" :quantity 1}]))))
      (is (= 3 (->> (call :get (str "/api/v1/decks/" id)) :body :cards
                    (filter #(= luke (:card-id %))) first :quantity))))
    (testing "remove card"
      (is (= 204 (:status (call :delete (str "/api/v1/decks/" id "/cards/" luke)))))
      (is (= 404 (:status (call :delete (str "/api/v1/decks/" id "/cards/" luke))))))
    (testing "delete deck"
      (is (= 204 (:status (call :delete (str "/api/v1/decks/" id)))))
      (is (= 404 (:status (call :get (str "/api/v1/decks/" id)))))
      (is (= 404 (:status (call :delete (str "/api/v1/decks/" id))))))))
