-- :name get-cards-by-set-and-rarity :? :*
SELECT * FROM cards WHERE set_code = :set-code AND rarity = :rarity;

-- :name insert-pack-opening! :? :1
INSERT INTO pack_openings (opening_id, owner, set_code, card_ids)
VALUES (:opening-id, :owner, :set-code, :card-ids)
RETURNING *;

-- :name get-pack-opening-by-id :? :1
SELECT * FROM pack_openings WHERE opening_id = :opening-id;

-- :name upsert-collection-card! :! :1
INSERT INTO collection_cards (owner, card_id, quantity)
VALUES (:owner, :card-id, 1)
       ON CONFLICT (owner, card_id) DO UPDATE SET quantity = quantity + 1;

-- :name get-collection :? :*
SELECT card_id, quantity AS owned FROM collection_cards WHERE owner = :owner;
