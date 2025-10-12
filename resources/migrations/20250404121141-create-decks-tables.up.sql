-- Migration: create-decks-tables (UP)

CREATE TABLE IF NOT EXISTS decks (
  deck_id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE,
  owner TEXT NOT NULL,
  format TEXT NOT NULL,
  side TEXT CHECK (side IN ('L', 'D')) NOT NULL
);

--;;

CREATE TABLE IF NOT EXISTS cards_to_deck (
  deck_id INTEGER,
  card_id INTEGER,
  quantity INTEGER CHECK(quantity BETWEEN 1 AND 4),
  PRIMARY KEY (deck_id, card_id),
  FOREIGN KEY (deck_id) REFERENCES decks(deck_id) ON DELETE CASCADE,
  FOREIGN KEY (card_id) REFERENCES cards(iq)
);
