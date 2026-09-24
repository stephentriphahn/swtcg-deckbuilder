(ns stevetrip.swtcg.deck-builder.web.packs-test
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

;; ANH is one of the 10 official sets pack-service/official-sets recognizes; FAKE isn't.
(def common "aaaaaaaaaaaa")
(def uncommon "bbbbbbbbbbbb")
(def rare "cccccccccccc")

(defn- call
  ([method uri] (call method uri nil))
  ([method uri body]
   ;; a real server splits ?query off :uri into :query-string before a handler ever sees
   ;; it; this harness builds the request map by hand, so it has to do that split itself.
   (let [[path query-string] (string/split uri #"\?" 2)
         req (cond-> {:request-method method :uri path :headers {}}
               query-string (assoc :query-string query-string)
               body (assoc :body (m/encode "application/json" body)
                           :headers {"content-type" "application/json"}))
         res (*app* req)]
     (cond-> res
       (and (:body res) (not (string? (:body res))))
       (update :body #(m/decode "application/json" %))))))

(defn with-app [f]
  (let [name (str "swtcg-packs-test-" (System/nanoTime) ".db")
        cs (str "sqlite://" name)]
    (try
      (migratus/migrate! cs)
      (let [c (conn/connect cs)]
        (doseq [[id n rarity] [[common "Common Card" "C"] [uncommon "Uncommon Card" "U"] [rare "Rare Card" "R"]]]
          (jdbc/execute! (conn/get-db c)
                         ["INSERT INTO cards (card_id, name, image_file, side, type, set_code, rarity) VALUES (?, ?, ?, 'L', 'Character', 'ANH', ?)"
                          id n (str n ".jpg") rarity]))
        (binding [*app* (routes/app (db/create-database c))]
          (f)))
      (finally (doseq [suffix ["" "-wal" "-shm"]] (.delete (File. (str name suffix))))))))

(use-fixtures :each with-app)

(deftest list-packs
  (let [res (call :get "/api/v1/packs")]
    (is (= 200 (:status res)))
    (is (= 10 (count (:body res))))
    (is (some #(= "ANH" (:set-code %)) (:body res)))
    (is (every? #(re-find #"^/packs/.+\.jpg$" (:image %)) (:body res)))))

(deftest open-pack-rejects-unofficial-sets
  (is (= 400 (:status (call :post "/api/v1/packs/open" {:owner "steve" :set-code "FAKE"})))))

(deftest open-pack-rejects-a-set-with-no-cards-of-a-rarity
  ;; ESB is official but nothing was seeded for it in this test db
  (is (= 400 (:status (call :post "/api/v1/packs/open" {:owner "steve" :set-code "ESB"})))))

(deftest pack-opening-lifecycle
  (let [res (call :post "/api/v1/packs/open" {:owner "steve" :set-code "ANH"})
        body (:body res)]
    (testing "draws 7 common, 3 uncommon, 1 rare, hydrated"
      (is (= 201 (:status res)))
      (is (string? (get-in res [:headers "Location"])))
      (is (= "ANH" (:set-code body)))
      (is (= "steve" (:owner body)))
      (is (= 11 (count (:cards body))))
      (is (= {common 7, uncommon 3, rare 1}
             (frequencies (map :card-id (:cards body)))))
      (is (every? #(= "Character" (:type %)) (:cards body))))

    (testing "reveal order is grouped by rarity — commons, then uncommons, then the rare last"
      (is (= (concat (repeat 7 common) (repeat 3 uncommon) [rare])
             (map :card-id (:cards body)))))

    (testing "credits the owner's collection"
      (let [collection (:body (call :get "/api/v1/collection?owner=steve"))]
        (is (= {common 7, uncommon 3, rare 1}
               (into {} (map (juxt :card-id :owned)) collection)))))

    (testing "a second pack adds to the existing collection instead of replacing it"
      (call :post "/api/v1/packs/open" {:owner "steve" :set-code "ANH"})
      (let [collection (:body (call :get "/api/v1/collection?owner=steve"))]
        (is (= 22 (reduce + (map :owned collection))))))

    (testing "re-fetching the opening returns the same cards"
      (let [refetched (:body (call :get (str "/api/v1/packs/openings/" (:opening-id body))))]
        (is (= (map :card-id (:cards body)) (map :card-id (:cards refetched))))))

    (testing "an unknown opening id 404s"
      (is (= 404 (:status (call :get "/api/v1/packs/openings/nope")))))

    (testing "collections are isolated per owner"
      (is (empty? (:body (call :get "/api/v1/collection?owner=someone-else")))))))
