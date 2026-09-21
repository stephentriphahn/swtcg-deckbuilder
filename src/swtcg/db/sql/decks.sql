-- :name insert-deck! :? :1
INSERT INTO decks (deck_id, name, owner, format, side)
VALUES (:deck-id, :name, :owner, :format, :side)
RETURNING *;

-- :name insert-card-to-deck! :? :1
INSERT INTO cards_to_deck (deck_id, card_id, quantity)
VALUES (:deck-id, :card-id, :quantity)
       ON CONFLICT (deck_id, card_id) DO UPDATE SET quantity=excluded.quantity
RETURNING *;

-- :name get-decks :? :*
SELECT * FROM decks ORDER BY name;

-- :name get-deck-by-name :? :1
SELECT * FROM decks WHERE name = :name LIMIT 1;

-- :name get-deck-by-id :? :1
SELECT * FROM decks WHERE deck_id = :deck-id;

-- :name get-deck-cards :? :*
SELECT c.*, d.quantity
FROM cards c
JOIN cards_to_deck d ON c.card_id = d.card_id
WHERE d.deck_id = :deck-id
ORDER BY c.name;

-- :name delete-deck! :! :1
DELETE FROM decks WHERE deck_id = :deck-id;

-- :name remove-card-from-deck! :! :1
DELETE FROM cards_to_deck WHERE deck_id = :deck-id AND card_id = :card-id;

-- :name remove-all-cards-from-deck! :! :1
-- FIXME: this is a hack because ON DELETE CASCADE is not working
-- call this after delete-deck!
DELETE FROM cards_to_deck WHERE deck_id = :deck-id;
