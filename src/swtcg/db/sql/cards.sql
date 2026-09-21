-- :name enable-foreign-keys! :! :n
PRAGMA foreign_keys = ON;

-- :name insert-card! :! :1
INSERT INTO cards (card_id, name, set_code, image_file, side, type, subtype, cost, speed, power, health, rarity, number, usage, text, script, classification)
VALUES (:card-id, :name, :set-code, :image-file, :side, :type, :subtype, :cost, :speed, :power, :health, :rarity, :number, :usage, :text, :script, :classification);

-- :name get-card-by-id :? :1
SELECT * FROM cards WHERE card_id = :card-id;

-- :name get-all-loaded-sets :? :*
SELECT DISTINCT set_code from cards;

-- :name get-card-by-name :? :1
SELECT * FROM cards WHERE name = :name;

-- :name old-search-cards :? :*
/* :require [clojure.string :as s] */
SELECT * FROM cards
/*~
(let [params (filter (comp some? val) params)]
  (when (not (empty? params))
    (str "WHERE "
      (s/join " AND "
              (for [[field _] params]
                (when (and (not= field :skip) (not= field :limitl))
                  (str (name field) " = " field)))))))
~*/
OFFSET :skip LIMIT :limit
;

-- :name search-cards :? :*
/* :require [clojure.string :as s] */
SELECT * FROM cards
/*~
(let [filter-params (dissoc params :skip :limit :search)
      where-clauses (cond-> []
                      (seq filter-params)
                      (into (for [[field _] filter-params]
                              (str (name field) " = :" (name field))))
                      
                      (:search params)
                      (conj "(name LIKE '%' || :search || '%' OR text LIKE '%' || :search || '%')"))]
  (when (seq where-clauses)
    (str "WHERE " (s/join " AND " where-clauses))))
~*/
ORDER BY card_id
/*~ (when (:limit params) "LIMIT :limit") ~*/
/*~ (when (:skip params) "OFFSET :skip") ~*/
;
