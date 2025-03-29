-- :name create-decks-table :! :n
CREATE TABLE IF NOT EXISTS decks (
  deck_id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  owner TEXT NOT NULL,
  format TEXT NOT NULL,
  side TEXT CHECK (side IN ('L', 'D')) NOT NULL
);

-- :name create-cards-to-deck-table :! :n
CREATE TABLE IF NOT EXISTS cards_to_deck (
  deck_id INTEGER,
  card_id INTEGER,
  quantity INTEGER CHECK(quantity BETWEEN 1 AND 4),
  PRIMARY KEY (deck_id, card_id),
  FOREIGN KEY (deck_id) REFERENCES decks(deck_id) ON DELETE CASCADE,
  FOREIGN KEY (card_id) REFERENCES cards(card_id)
);

-- :name insert-deck! :! :1
INSERT INTO decks (name, owner, format, side)
VALUES (:name, :owner, :format, :side);

-- :name insert-card-to-deck! :! :1
INSERT INTO cards_to_deck (deck_id, card_id, quantity)
VALUES (:deck_id, :card_id, :quantity)
       ON CONFLICT (deck_id, card_id) DO UPDATE SET quantity=excluded.quantity;

-- :name get-decks :? :*
SELECT * FROM decks ORDER BY name;

-- :name get-deck-by-name :? :1
SELECT * FROM decks WHERE name = :name LIMIT 1;

-- :name get-deck-by-id :? :1
SELECT * FROM decks WHERE deck_id = :deck_id;

-- :name get-deck-cards :? :*
SELECT c.*, d.quantity
FROM cards c
JOIN cards_to_deck d ON c.card_id = d.card_id
WHERE d.deck_id = :deck_id
ORDER BY c.name;

-- :name delete-deck! :! :1
DELETE FROM decks WHERE deck_id = :deck_id;

-- :name remove-card-from-deck! :! :1
DELETE FROM cards_to_deck WHERE deck_id = :deck_id AND card_id = :card_id;

-- :name remove-all-cards-from-deck! :! :1
-- FIXME: this is a hack because ON DELETE CASCADE is not working
-- call this after delete-deck!
DELETE FROM cards_to_deck WHERE deck_id = :deck_id;
