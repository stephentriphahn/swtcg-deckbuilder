# Design: Collection & Pack Opening

Companion to [`ui-plan.md`](ui-plan.md) (routes, tech stack, workspace conventions all carry over) and [`ui-principles.md`](ui-principles.md). This adds a collection of owned cards, built by opening simulated booster packs, one set at a time.

## 1. Goals & scope

- Open a virtual pack of one official set and reveal its 11 cards with a face-down → flip animation.
- Track what the player owns (a "collection") and show it in the existing catalog (owned counts), which `ui-plan.md` §5 already left a slot for and deferred.
- Official 10 sets only, matching the rest of the app. No real-money economy, no trading — out of scope for this doc.
- Single implicit player for now, consistent with how decks already work (no auth; a free-text `owner` string). Users, an economy and per-user collections are explicitly future work, not designed here — pick a pack, open it, see what you got.
- Ships as its own top-level page, `Packs`, alongside `Cards` and `Decks` — not folded into either existing page.

## 2. What's already in the repo

- **Pack composition is already authored, not invented here.** `resources/public/packs/packdefinitions.xml` defines a pack per set: `1 Rare + 3 Uncommon + 7 Common` (11 cards), matching exactly what was asked. Every official set has an entry (`packimage` matches `set_code`, case varies).
- **Pack wrapper art** is at `resources/public/packs/{SET_CODE}.jpg` (e.g. `AOTC.jpg`), tall portrait-ish (~436×800, aspect ≈ 0.55 — noticeably narrower/taller than a card). Already served by the existing static handler (`routes.clj`, root `resources/public/`) at `/packs/{SET}.jpg`, no server change needed.
- **Card back art** is `resources/public/cardback.jpg` (315×437 — matches the portrait card aspect ratio closely enough to reuse `isLandscape`/aspect-ratio handling as-is), served at `/cardback.jpg`.
- **Rarity pools are large enough** in `cards.db` to draw from — checked directly: every official set has 30–70 cards in each of C/U/R (e.g. ANH has 60/60/60, PM has 30/30/30). A pack's 7 commons, 3 uncommons and 1 rare are comfortably satisfiable per set.
- **Deck's `max 4 copies` rule does not apply here.** That's a deck-legality rule, not a print-run limit; packs should allow duplicate copies of a card within or across openings, same as a real booster.

## 3. Data model

Two new tables (new migration, `bb create-migration add-collection`), following the existing `card_id`/`deck_id` conventions:

```sql
CREATE TABLE collection_cards (
  owner    TEXT NOT NULL,
  card_id  TEXT NOT NULL REFERENCES cards(card_id),
  quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
  PRIMARY KEY (owner, card_id)
);

CREATE TABLE pack_openings (
  opening_id TEXT PRIMARY KEY,     -- same SHA/UUID convention as deck_id
  owner      TEXT NOT NULL,
  set_code   TEXT NOT NULL,
  opened_at  TEXT NOT NULL DEFAULT (datetime('now')),
  card_ids   TEXT NOT NULL         -- JSON array, the 11 card_ids drawn, in reveal order
);
```

- `collection_cards` is the running total per card — what the catalog's "owned N" badge reads. Unbounded quantity (unlike decks' 1–4 cap), since owning 12 copies of a common is normal.
- `pack_openings` is a history log — lets a reveal be re-fetched (e.g. after a page refresh mid-animation) and gives "packs opened" stats later. `card_ids` as a JSON blob is enough for a log; SQLite doesn't need a join table for it since it's never queried by card.
- `owner` is a free string like `decks.owner` — no user table exists yet, so this stays consistent with the rest of the app rather than inventing auth.

## 4. Backend: generation & endpoints

New namespace `stevetrip.swtcg.deck-builder.services.pack-service`, alongside `deck-service`. New SQL in `db/sql/packs.sql` (mirrors `cards.sql`/`decks.sql`, generated via `def-db-fns`).

**Generation** — for a given `set-code`:
1. Query `cards` for that set, split into three pools by `rarity` (C/U/R), excluding `NULL` rarity (promos).
2. Draw 7 from the common pool, 3 from uncommon, 1 from rare — **with replacement** (a real booster can, and often does, repeat a common), each draw uniform-random within its pool.
3. Order the 11 by rarity — commons, then uncommons, then the rare last — matching `pack-composition`'s own order. (Revised from an earlier full shuffle: the UI reveals one card at a time and deliberately builds up to the rare, so grouped order is the point, not an oversight.)
4. Persist: upsert each drawn card into `collection_cards` (increment quantity), insert one `pack_openings` row.

```sql
-- :name get-cards-by-set-and-rarity :? :*
SELECT * FROM cards WHERE set_code = :set-code AND rarity = :rarity;

-- :name upsert-collection-card! :! :1
INSERT INTO collection_cards (owner, card_id, quantity) VALUES (:owner, :card-id, 1)
  ON CONFLICT (owner, card_id) DO UPDATE SET quantity = quantity + 1;

-- :name get-collection :? :*
SELECT c.*, cc.quantity AS owned
FROM collection_cards cc JOIN cards c ON c.card_id = cc.card_id
WHERE cc.owner = :owner;
```

**Endpoints** (`/api/v1`, added to `web/routes.clj` + `web/schema.clj` + `web/handlers.clj`, same reitit/malli/HugSQL pattern as decks):

| Method & path | Body / params | Returns |
|---|---|---|
| `GET /packs` | — | `[{set-code, name, image}]` for the 10 official sets — `name` from `resources/set-name-to-code.edn`, `image` = `/packs/{SET}.jpg` |
| `POST /packs/open` | `{owner, set-code}` | `201 {opening-id, set-code, cards: [Card...]}` — cards hydrated (not just ids), in reveal order |
| `GET /packs/openings/:opening-id` | — | same shape, to re-fetch an in-progress reveal after a refresh |
| `GET /collection` | `?owner=` | `[{card-id, owned}]` or hydrated `[Card & {owned}]` — mirrors the deck-cards hydration gap already flagged in `ui-plan.md` §8, so fix both together |

`POST /packs/open` does the draw, the collection upsert, and the log insert as one transaction (`next.jdbc` transaction, same as any multi-write deck operation) — a pack should never be "half credited."

## 5. Frontend: pack opening flow

A third top-level page, alongside Cards and Decks — a `Packs` link in `App.tsx`'s nav, next to the existing ones. New routes: `/packs` (choose a set) and `/packs/:openingId` (the reveal).

**`/packs` — choosing a pack**
- A row/grid of the 10 pack-wrapper images (`/packs/{SET}.jpg`), tall cards, set name beneath each. Clicking one calls `POST /packs/open` and navigates to `/packs/:openingId`.
- Simple wrapper "tear open" hover/press affordance (scale + shadow on hover) — no animation needed before the flip screen; the fun part is the reveal.

**`/packs/:openingId` — the reveal.** Done, revised from the original one-grid-of-11 plan below to a sequential "thumb through the pack" flow:
- `PackRevealPage` renders the plain grid of all 11 cards (already face up — same as before) with `PackFlipModal` covering it on top, same relationship as `CardDetail` overlaying the catalog grid. Closing the modal (finishing or skipping) just reveals the grid underneath; nothing re-fetches.
- **`PackFlipModal`** is a single dialog that pages through one step at a time: the pack wrapper art first ("click to open"), then **only the first card** is face-down (`cardback.jpg`) and flips on click — matching how a real pack works: you flip the whole stack over once, and every card after that is already face up, just sitting under the one in front of it. So card 2 onward shows immediately (a quick slide-in, not a flip) on "Next card," in reveal order — **all 7 commons, then the 3 uncommons, then the rare last** (§4's `open-pack` no longer shuffles across rarities, on purpose — see the note there). The rare's reveal gets a small ring/glow (`rarity: 'R'`) — the "cheap payoff" this doc originally suggested landed here instead of a rarity badge, since revealing a card already shows its rarity via the same info panel as `CardDetail`.
- **`FlipCard`** is the reusable 3D-flip primitive, used once, for the first card only: a `transform-style: preserve-3d` box with two `backface-visibility: hidden` faces, the revealed face pre-rotated 180° so it lands right-side up when the whole box flips. Cards 2–11 skip it entirely and just fade/slide in (a `pack-card-in` CSS keyframe, retriggered by remounting the element keyed on card id).
- Once a card is flipped, it's shown with **`CardInfoPanel`** — extracted out of `CardDetail` so a card looks identical whether you found it in the catalog or just pulled it from a pack. This is deliberately the "first pass" per this conversation; a closer-to-the-metal presentation (bigger art, more theatrical reveal) is future work, not designed here.
- A "Skip" (×) control closes the modal from any step. Keyboard: Enter/Space/→ advance (flip, or move to the next card), same action as clicking.
- No flip state is persisted (matches §7's decision): a refresh replays the modal from the pack art, since the cards themselves were already credited to the collection the instant the pack was opened.

<details><summary>Original plan (superseded above)</summary>

Fetches the opening and lays out 11 face-down cards in a simple grid, each independently clickable/flippable, plus a "Reveal all" button — no forced order, no single-flip-at-a-time state machine. Superseded once "page through the pack, in rarity order, using one flip element" was specified.

</details>

## 6. Collection → catalog integration

This is exactly the ownership feature `ui-plan.md` §5 deferred ("tiles and tray keep a slot for 'owned N'"). Once `GET /collection` exists:
- `useCollection()` query hook, same shape as `useCatalog()`.
- `CardTile` gains an optional `owned?: number` badge (top-left corner, small pill) — rendered only when a collection is loaded, so `/cards` works unchanged for anyone who hasn't opened any packs.
- A `/collection` view is just `/cards` pre-filtered to `owned > 0` — worth a "My Collection" nav link and possibly a new client-side-only filter (`ownedOnly: boolean`) in `lib/filters.ts`, no new API filter needed since the whole catalog is already loaded client-side (§7 of `ui-plan.md`).
- Deck building doesn't need to change — `ui-principles.md` P5 says grey out or badge unowned cards rather than hiding them, so the workspace's catalog can eventually show the same badge without blocking adding a card you don't "own" (this app was never a strict collection-limited deckbuilder).

## 7. Decisions & open questions

Decided:
- **Pack cost / pacing**: unlimited, instant packs for now — no currency, no cooldown, no economy. Pick a set, open it. `pack_openings` still logs every draw, so a later economy (currency, a daily cap, per-user limits) has an audit trail to build on without a schema change.
- **Reveal persistence mid-flip**: a refresh mid-reveal resets to all-face-down rather than resuming exact flip state. The cards themselves are already safely in the collection the moment the pack is opened (§4's transaction), so nothing is at risk — only the *order you'd already seen them in* resets, which is low-stakes and keeps the reveal page simple (no need to persist flip state anywhere).

Still open, not blocking:
- **Users & economy**: explicitly future work per this conversation — `owner` stays a free string for now (matching decks), the same way a real user system would eventually replace it everywhere at once, not just here.
- **Foils / promos**: real WOTC packs occasionally included promos or foils; out of scope here since `rarity: P` cards aren't in any set's normal pool and `packdefinitions.xml` doesn't call them out per-pack either.

## 8. Phased delivery

1. **Backend**: migration, `pack-service`, `GET /packs`, `POST /packs/open`, `GET /collection` (unhydrated first, hydrate once the deck-hydration gap is fixed for both). Tests mirror `deck-service`'s (a full open updates `collection_cards` correctly; drawing respects rarity counts; concurrent opens don't clobber each other's quantity — transaction test).
2. **Pack picker** (`/packs`): the 10 wrappers, `POST` + navigate.
3. **Reveal.** Done, as described in §5: `PackFlipModal` (one flip element, paged through in rarity order) over the plain grid, plus "Open another pack."
4. **Collection integration**: `useCollection`, owned badge on `CardTile`, `/collection` view.
