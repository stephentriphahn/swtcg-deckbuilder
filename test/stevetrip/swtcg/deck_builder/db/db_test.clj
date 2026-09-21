(ns stevetrip.swtcg.deck-builder.db.db-test
  (:require [clojure.test :refer :all]
            [stevetrip.swtcg.deck-builder.db.connection :as conn]
            [stevetrip.swtcg.deck-builder.db.migratus :as migratus]
            [stevetrip.swtcg.deck-builder.db.db :as db]
            [stevetrip.swtcg.deck-builder.db.memory]
            [stevetrip.swtcg.deck-builder.db.sqlite]))

(doseq [cs ["memory://" "sqlite://test.db"]]
  (let [conn (conn/connect cs)
        card-db (db/create-database conn)]
    (deftest list-cards
      (testing "lists all cards"
        (let [cards (db/list-cards card-db {})
              expected []]
          (is (= expected cards)))))))
