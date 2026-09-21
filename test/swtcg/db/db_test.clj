(ns swtcg.db.db-test
  (:require [clojure.test :refer :all]
            [swtcg.db.connection :as conn]
            [swtcg.db.migratus :as migratus]
            [swtcg.db.db :as db]
            [swtcg.db.memory]
            [swtcg.db.sqlite]))

(doseq [cs ["memory://" "sqlite://test.db"]]
  (let [conn (conn/connect cs)
        card-db (db/create-database conn)]
    (deftest list-cards
      (testing "lists all cards"
        (let [cards (db/list-cards card-db {})
              expected []]
          (is (= expected cards)))))))
