-- Migration: add-collection-and-packs (UP)

CREATE TABLE IF NOT EXISTS collection_cards (
  owner TEXT NOT NULL,
  card_id TEXT NOT NULL,
  quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
  PRIMARY KEY (owner, card_id),
  FOREIGN KEY (card_id) REFERENCES cards(card_id)
);

--;;

CREATE TABLE IF NOT EXISTS pack_openings (
  opening_id TEXT PRIMARY KEY,
  owner TEXT NOT NULL,
  set_code TEXT NOT NULL,
  opened_at TEXT NOT NULL DEFAULT (datetime('now')),
  card_ids TEXT NOT NULL
);
