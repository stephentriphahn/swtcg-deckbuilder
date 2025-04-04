-- Migration: create-decks-tables (UP)
-- :qisable-transaction
-- Enable FK constraints in SQLite (if needed)
PRAGMA foreign_keys = ON;
-- ;;
-- Create the main `decks` table
CREATE TABLE decks (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL UNIQUE,
  owner TEXT NOT NULL,
  format TEXT NOT NULL,
  side TEXT CHECK (side IN ('L', 'D')) NOT NULL
);
--;;

-- Create the junction table for deck cards
CREATE TABLE cards_to_deck (
  deck_id INTEGER,
  card_id INTEGER,
  quantity INTEGER CHECK(quantity BETWEEN 1 AND 4),
  PRIMARY KEY (deck_id, card_id),
  FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE,
  FOREIGN KEY (card_id) REFERENCES cards(iq)
);
