-- Migration: create-cards-table (UP)
-- :disable-transaction
CREATE TABLE cards (
  -- deterministic short hash of image_file, see swtcg.tools.load-cards/card-id
  card_id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  set_code TEXT,
  image_file TEXT NOT NULL UNIQUE,
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
