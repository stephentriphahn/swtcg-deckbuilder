-- :name create-cards-table :! :n
CREATE TABLE IF NOT EXISTS cards (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  set_code TEXT,
  image_file TEXT,
  side TEXT CHECK (side IN ('L', 'D', 'N')) NOT NULL,
  type TEXT,
  subtype TEXT,
  cost INTEGER,
  speed INTEGER,
  power INTEGER,
  health INTEGER,
  rarity TEXT,
  number INTEGER,
  usage TEXT,
  text TEXT,
  script TEXT,
  classification TEXT,
  abilities TEXT  -- Stored as JSON
);

-- :name insert-card! :! :1
INSERT INTO cards (name, set_code, image_file, side, type, subtype, cost, speed, power, health, rarity, number, usage, text, script, classification, abilities)
VALUES (:name, :set_code, :image_file, :side, :type, :subtype, :cost, :speed, :power, :health, :rarity, :number, :usage, :text, :script, :classification, :abilities);

-- :name get-card-by-id :? :1
SELECT * FROM cards WHERE id = :id;

-- :name get-card-by-name :? :1
SELECT * FROM cards WHERE name = :name;

-- :name search-cards :? :*
/* :require [clojure.string :as s] */
SELECT * FROM cards
/*~
(let [params (filter (comp some? val) params)]
  (when (not (empty? params))
    (str "WHERE "
      (s/join " AND "
              (for [[field _] params]
                (str (name field) " = " field))))))
~*/
;
