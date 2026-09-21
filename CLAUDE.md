# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

SWTCG: backend API (Clojure) for a Star Wars Trading Card Game deck builder and card catalog. It serves card search and deck building/validation over REST (`/api/v1`, Swagger UI at `/docs`; see `API.md`). The frontend and game logic live in separate projects; this repo is only the API. Only the 10 official WOTC sets (AOTC, SR, ANH, BOY, ESB, RAS, JG, ROTJ, PM, ROTS; `original-sets` in `swtcg.tools.load-cards`) are loaded into `cards.db`, so it is the source of truth for whether a card or keyword is official; the other files in `resources/public/sets` are fan-made (IDC) and out of scope. The README is an unfilled template.

## Commands

- Env: `.envrc` exports `SWTCG_CARD_DB_CS="sqlite://cards.db"` (required; read via aero `#env` in `resources/config.edn`, validated by malli in `swtcg.config`). Load it (direnv) before starting a REPL.
- Dev REPL: `clj -M:dev`, then use integrant-repl in `dev/user.clj`: `(go)` / `(reset)`. Server starts on port 3000 (`swtcg.system`). Migrations run as part of the system (`::migrations`).
- All tests: `clj -X:test`
- Single namespace: `clj -X:test :nses '[swtcg.db.db-test]'`; single var: `clj -X:test :vars '[swtcg.db.db-test/some-test]'`
- Load card data from CSV into SQLite: `clj -X:load-cards-sqlite` (`swtcg.tools.load-cards/load-cards-cli`)
- New migration: `bb create-migration <kebab-name>` (creates timestamped up/down SQL in `resources/migrations`, run with migratus)

## Architecture

- **Wiring**: `swtcg.system` defines the integrant system map: config → `::connection` → `::db` → `::app` (reitit ring handler) → `::server` (jetty). `swtcg.core/-main` is a separate, simpler entry point that runs `routes/app` directly.
- **DB abstraction**: `swtcg.db.db` parses the connection string URI (`sqlite://cards.db`) into a map and dispatches on `:scheme` to create a database implementation (`db/sqlite.clj` for SQLite, `db/memory.clj` for in-memory). Handlers/services depend on the db protocol rather than SQL directly.
- **SQL**: SQLite impl uses HugSQL; queries live in `src/swtcg/db/sql/{cards,decks}.sql` and functions are generated with `def-db-fns` (hence the `declare`s in `sqlite.clj`). Results use `as-unqualified-kebab-maps`, so DB rows have kebab-case keys. Schema comes from `resources/migrations` (migratus). `cards.db` / `test.db` are local SQLite files (with WAL/SHM side files).
- **IDs**: `card_id` is a deterministic 12-hex-char SHA-1 of `image_file` (`swtcg.tools.load-cards/card-id`), so it survives a `cards.db` rebuild. `deck_id` is a random UUID string generated in `sqlite.clj`. Both are TEXT everywhere (schema, routes, malli).
- **Web layer** (`swtcg.web.*`): `routes` (reitit + swagger/coercion + CORS), `handlers`, `schema` (malli request/response schemas), `middleware`, `error` (error mapping using `resources/http-errors.edn`).
- **Deck validation**: `swtcg.validation.rules` is a Clara Rules rulebase (facts `Deck`, `Card`, `Violation`; e.g. 60-card size, max 4 copies), driven by `swtcg.validation.service`. `swtcg.services.deck_service` orchestrates deck operations.
- Source, `test`, `resources`, and `dev` are all on the classpath (`deps.edn` `:paths`).

## Keyword scope

Checked against the `text` column of the official cards in `cards.db`. Only these keywords appear on official cards: Evade, Accuracy, Critical Hit, Pilot, Shields, Armor, Intercept, Upkeep, Retaliate, Stun, Bounty, Lucky, Deflect, Bombard, Overkill, Stack, Reserves, Enhance, Parry, Equip, Hidden Cost, Ion Cannon. (Substring matches, so counts may be inflated.)

These candidates are unofficial and not needed for the MVP: Absorb, Alternative Cost, Ambush, Area Damage, Avenge, Backfire, Barrage, Cunning, Damage Control, Double Damage, Double Strike, Ferocity, Focus, Foresight, Forewarning, Fortitude, Fury, INSERT, Inspiration, Intimidation, Meditate, Persuade, Precision, Protect, Redirect, Reduced Cost, Resilience, Riposte, Stealth, Surge, Switch, Velocity. (Avenge and Barrage only matched the names "Avenger (A)" and "Blaster Barrage".)
