-- Migration: create-cards-table (UP)
-- :disable-transaction
CREATE TABLE cards (
  card_id INTEGER PRIMARY KEY AUTOINCREMENT,
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
  classification TEXT
);
