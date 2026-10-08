(ns stevetrip.swtcg.deck-builder.web.cards-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [muuntaja.core :as m]
            [next.jdbc :as jdbc]
            [stevetrip.swtcg.deck-builder.db.connection :as conn]
            [stevetrip.swtcg.deck-builder.db.db :as db]
            [stevetrip.swtcg.deck-builder.db.migratus :as migratus]
            [stevetrip.swtcg.deck-builder.db.sqlite]
            [stevetrip.swtcg.deck-builder.web.routes :as routes])
  (:import (java.io File)))

(def ^:dynamic *app* nil)

(defn- call
  ([method uri] (call method uri nil))
  ([method uri body]
   (let [[path query-string] (string/split uri #"\?" 2)
         req (cond-> {:request-method method :uri path :headers {}}
               query-string (assoc :query-string query-string)
               body (assoc :body (m/encode "application/json" body)
                           :headers {"content-type" "application/json"}))
         res (*app* req)]
     (cond-> res
       (and (:body res) (not (string? (:body res))))
       (update :body #(m/decode "application/json" %))))))

;; Jar Jar Binks (A): "Gungan" is only in his subtype, never in his name or card text -
;; the exact case the search bar was reported missing.
(def jar-jar "aaaaaaaaaaaa")
;; Luke Skywalker (A): "Gungan" appears nowhere on this card - a negative control.
(def luke "bbbbbbbbbbbb")

(defn with-app [f]
  (let [name (str "swtcg-cards-test-" (System/nanoTime) ".db")
        cs (str "sqlite://" name)]
    (try
      (migratus/migrate! cs)
      (let [c (conn/connect cs)]
        (jdbc/execute! (conn/get-db c)
                       ["INSERT INTO cards (card_id, name, image_file, side, type, subtype, text, set_code, rarity) VALUES (?, ?, ?, 'N', 'Character', ?, ?, 'ANH', 'R')"
                        jar-jar "Jar Jar Binks (A)" "jarjar.jpg" "Gungan Diplomat"
                        "When your opponent rolls one or more dice in an attack on Jar Jar, you may reroll any of those dice."])
        (jdbc/execute! (conn/get-db c)
                       ["INSERT INTO cards (card_id, name, image_file, side, type, subtype, text, set_code, rarity) VALUES (?, ?, ?, 'L', 'Character', ?, ?, 'ANH', 'R')"
                        luke "Luke Skywalker (A)" "luke.jpg" "Rebel Pilot"
                        "Pay 1 Force -> Evade 1."])
        (binding [*app* (routes/app (db/create-database c))]
          (f)))
      (finally (doseq [suffix ["" "-wal" "-shm"]] (.delete (File. (str name suffix))))))))

(use-fixtures :each with-app)

(deftest search-matches-subtype
  (testing "a search term that only appears in subtype still finds the card"
    (let [res (call :get "/api/v1/cards?search=Gungan")]
      (is (= 200 (:status res)))
      (is (= [jar-jar] (map :card-id (:cards (:body res)))))))
  (testing "a search term matching nothing still returns an empty list, not an error"
    (let [res (call :get "/api/v1/cards?search=Wookiee")]
      (is (= 200 (:status res)))
      (is (empty? (:cards (:body res)))))))

(deftest search-still-matches-name-and-text
  (is (= [jar-jar] (map :card-id (:cards (:body (call :get "/api/v1/cards?search=Jar+Jar"))))))
  (is (= [luke] (map :card-id (:cards (:body (call :get "/api/v1/cards?search=Evade")))))))
