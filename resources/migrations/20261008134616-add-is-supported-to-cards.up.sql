-- Migration: add-is-supported-to-cards (UP)
-- Whether every printed keyword/ability on this card is implemented by swtcg-game-engine today.
-- Defaults to false/0; stevetrip.swtcg.deck-builder.tools.ingest-supported-cards flips the ones
-- the engine's resources/supported-cards.edn lists as supported.
ALTER TABLE cards ADD COLUMN is_supported INTEGER NOT NULL DEFAULT 0 CHECK (is_supported IN (0, 1));
