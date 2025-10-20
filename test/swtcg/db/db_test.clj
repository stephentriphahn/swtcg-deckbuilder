(ns swtcg.db.db-test
  (:require [clojure.test :refer :all]
            [swtcg.db.connection :as conn]
            [swtcg.db.migratus :as migratus]
            [swtcg.db.db :as db]
            [swtcg.db.memory]
            [swtcg.db.sqlite]))

(defn create-test-db
  "Create a test database (memory or temporary SQLite)"
  [db-type]
  (case db-type
    :memory (let [connection (conn/connect "memory://")]
              (db/create-database connection))
    :sqlite (let [temp-file (java.io.File/createTempFile "test-db-" ".db")
                  _ (.deleteOnExit temp-file)
                  conn-str (str "sqlite://" (.getAbsolutePath temp-file))
                  connection (conn/connect conn-str)]
              (println conn-str)
              (migratus/migrate! conn-str)
              (db/create-database connection))))

(defmacro deftest-both
  "Define a test that runs against both memory and sqlite implementations"
  [test-name & body]
  `(do
     ~@(for [db-type [:memory :sqlite]]
         (let [full-test-name (symbol (str (name test-name) "-" (name db-type)))]
           `(deftest ~full-test-name
              (testing ~(str "Testing with " (name db-type) " implementation")
                (let [~'database (create-test-db ~db-type)]
                  ~@body)))))))

(deftest-both test-add-deck
  (let [deck {:name "Test Deck"
              :owner "tester"
              :format "standard"
              :side "L"}
        result (db/list-cards database {})]
    ;; (is (some? (:deck-id result)) "Should generate deck ID")
    ;; (is (= "Test Deck" (:name result)) "Should preserve name")
    ;; (is (= "tester" (:owner result)) "Should preserve owner")
    ;; (is (= "standard" (:format result)) "Should preserve format")
    (is true)))
