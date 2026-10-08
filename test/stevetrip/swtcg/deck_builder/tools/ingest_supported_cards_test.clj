(ns stevetrip.swtcg.deck-builder.tools.ingest-supported-cards-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [next.jdbc :as jdbc]
            [stevetrip.swtcg.deck-builder.db.migratus :as migratus]
            [stevetrip.swtcg.deck-builder.tools.ingest-supported-cards :as sut])
  (:import (java.io File)))

(def dbname (str "swtcg-ingest-supported-test-" (System/nanoTime) ".db"))
(def edn-path (str "swtcg-ingest-supported-test-" (System/nanoTime) ".edn"))

(def supported-a "aaaaaaaaaaaa")
(def supported-b "bbbbbbbbbbbb")
(def never-supported "cccccccccccc")
;; listed as supported in the file, but no such row exists in this test db
(def ghost-id "dddddddddddd")

(defn- with-fixtures [f]
  (migratus/migrate! (str "sqlite://" dbname))
  (let [ds (jdbc/get-datasource {:dbtype "sqlite" :dbname dbname})]
    (doseq [[id n] [[supported-a "A"] [supported-b "B"] [never-supported "C"]]]
      (jdbc/execute! ds
                     ["INSERT INTO cards (card_id, name, image_file, side, set_code, rarity) VALUES (?, ?, ?, 'L', 'ANH', 'C')"
                      id n (str n ".jpg")])))
  (spit edn-path (pr-str {:supported [{:card-id supported-a :name "A"}
                                       {:card-id supported-b :name "B"}
                                       {:card-id ghost-id :name "Ghost"}]
                           :excluded [never-supported]}))
  (try
    (f)
    (finally
      (doseq [suffix ["" "-wal" "-shm"]] (.delete (File. (str dbname suffix))))
      (.delete (File. edn-path)))))

(use-fixtures :each with-fixtures)

(defn- is-supported [ds card-id]
  (-> (jdbc/execute-one! ds ["SELECT is_supported FROM cards WHERE card_id = ?" card-id])
      :cards/is_supported))

(deftest read-supported-ids-pulls-only-the-supported-list
  (testing "excluded ids (a different, stricter category) are never included"
    (is (= [supported-a supported-b ghost-id] (sut/read-supported-ids edn-path)))))

(deftest ingest-marks-listed-ids-true-and-leaves-others-false
  (let [result (sut/ingest-supported-cards-cli {:dbname dbname :path edn-path})
        ds (jdbc/get-datasource {:dbtype "sqlite" :dbname dbname})]
    (is (= {:listed 3 :matched 2 :missing 1} result))
    (is (= 1 (is-supported ds supported-a)))
    (is (= 1 (is-supported ds supported-b)))
    (is (= 0 (is-supported ds never-supported)))))

(deftest ingest-is-idempotent-and-un-marks-cards-dropped-from-the-file
  (let [ds (jdbc/get-datasource {:dbtype "sqlite" :dbname dbname})]
    (sut/ingest-supported-cards-cli {:dbname dbname :path edn-path})
    ;; regenerate the file without supported-b, as a re-run after the engine regresses would
    (spit edn-path (pr-str {:supported [{:card-id supported-a :name "A"}]}))
    (sut/ingest-supported-cards-cli {:dbname dbname :path edn-path})
    (is (= 1 (is-supported ds supported-a)))
    (is (= 0 (is-supported ds supported-b)))))
