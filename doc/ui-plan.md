# UI Plan — SWTCG Card Catalog & Deck Builder

UX principles are in [`ui-principles.md`](ui-principles.md); this plan references them as **P1–P10**.

## 1. Goals & scope

- Browse and filter the card catalog by set, type and other card properties.
- Build, edit, duplicate and delete 60-card decks, choosing cards straight from the catalog.
- Scope: the 10 official WOTC sets loaded in `cards.db` (AOTC, SR, ANH, BOY, ESB, RAS, JG, ROTJ, PM, ROTS; 1324 cards). Fan-made sets are out of scope.
- Single user, no auth, local first. Ownership/collection tracking is deferred but designed for (see §5).

## 2. Tech

- React + TypeScript + Vite (the API's CORS already allows `http://localhost:5173`).
- TanStack Query (server state), React Router, Zustand (local deck draft / UI state), Tailwind, Vitest + Playwright.
- Lives in `ui/` in this repo. Vite dev proxy: `/api` and `/setimages` → `http://localhost:3000`.
- Generate a typed client from `/swagger.json`.
- `CLAUDE.md` currently says the frontend is a separate project; update it when `ui/` is scaffolded.

## 3. Facts about the API the UI is built on

| Topic | Fact |
|---|---|
| Card list | `GET /api/v1/cards`: `search` (name/text), `side` (L/D/N), `type`, `set_code`, `limit` (1–100), `skip`. No sort (fixed `ORDER BY card_id`, effectively random), no total count. Verified against the running server: `limit=101` → 400; `rarity=R` works undocumented (435 cards) because extra query keys become column filters; an unknown key such as `nonexistent=1` → 500 `SQLiteException`. |
| Card fields | `card-id, name, type, side, set-code, number, rarity, subtype, cost, speed, power, health, usage, text, script, classification, image-file` |
| Types | Character 476, Battle 212, Ground 209, Space 208, Mission 137, Location 74, Equipment 8 |
| Rarity / side | C, U, R, P / L, D, N (Neutral) |
| Stat sentinel | `-1` means a variable stat (`*`); render as `*` |
| Missing values | `number` is NULL on 39 promos; `subtype` is empty on ~350 cards and messy; `classification` is a comma-separated tag string (e.g. `WOTC, REB, EP456`) |
| Images | `/setimages/{set-code}/{image-file}.jpg` (~312×437). Not `/public/...` as `API.md` states, and not organised by type. All 1324 exist. |
| Decks | list/create/get/delete; `PUT /decks/:id/cards/:card-id {quantity 1–4}` (upsert); `POST /decks/:id/cards` bulk; `DELETE` card. No rename/update. |
| Get deck | `cards: [{card-id, quantity}]` (not hydrated) + `validation {valid?, violations[{rule,message}], warnings}` |
| Validation | Advisory, computed on read; invalid decks can be saved. Rules: 60 cards, ≤4 copies, side match (or Neutral), ≥12 Character, ≥12 Space, ≥12 Ground. |

## 4. Routes

| Route | Purpose |
|---|---|
| `/cards` | Catalog browser (pure browse, no deck) |
| `/cards/:cardId` | Card detail (modal, deep-linkable) |
| `/decks` | Deck list: new (name, owner, format, side), duplicate, rename, delete |
| `/decks/:deckId` | Workspace: catalog + deck with Browse \| Build toggle (§5) |
| `/decks/:deckId/view` | Optional read-only / printable list |

## 5. Catalog & workspace UX

### Catalog browser
- Card-art grid (default) or compact list. Lazy-loaded images with a fixed aspect ratio to avoid layout shift.
- **Filtering (P6):** persistent search bar; quick-toggle chips for side, type, set (with counts), rarity, cost; "Advanced filters" drawer for cost/power/health ranges, subtype, classification tags and keyword chips (official keywords only, per `CLAUDE.md`). Additive, with live result count and removable active-filter chips.
- Sort: name, set + number, cost, type. Default is set + number, not the API's hash order.
- Card detail shows full text, stats and a large image.

### Workspace (P1–P5)
- **One page, two modes.** `/decks/:deckId` has a Browse | Build toggle; both panes stay mounted. Scroll position, filters and sort persist across toggles and are kept in the URL + store (P2). Desktop: catalog and deck side by side. Mobile: tab switch with preserved state.
- Same `CardTile` and `FilterBar` in both modes (P1).
- **Adding never navigates (P3).** The tile updates in place: in-deck badge, +/− stepper, dimmed when at 4 copies or on the wrong side.
- **Persistent deck tray (P4)**, visible in the catalog: size `37/60`, mini cost curve, type meters against the 12/12/12 minimums, copies-in-deck per card. `/cards` with no deck is pure browse; choosing an active deck turns the tray on.
- **Inline legality (P5):** each tile shows in-deck count and whether another copy is legal (max 4, side mismatch, deck full). Ownership is deferred: tiles and tray keep a slot for "owned N", and a future `collection` table + endpoints is a P2 API item. MVP treats the whole official pool as available.
- Catalog is pre-filtered to the deck's side + Neutral, with a one-click override.

### Build mode
- **Spatial (P8):** mini card-art grid grouped by type with stacked quantities, cost-curve chart, type breakdown. List view is a toggle.
- **Removal (P7):** immediate, with an Undo toast. Only deleting a whole deck asks for confirmation.
- **Autosave (P9):** no Save button. Optimistic per-card `PUT`/`DELETE`, debounced; rollback with a retry banner on failure; ambient "Saved / Saving… / Offline" indicator; `beforeunload` guard while a write is pending.
- **Validation:** advisory (the API allows invalid decks). Live client-side pre-check plus the server's result; each violation links to a pre-filtered catalog (e.g. "need 4 more Space" → Space filter).
- **Drafting (P10):** many decks, duplicate (create + bulk `POST`), rename, discard.

## 6. Card orientation

Not all card art is portrait. Checked against every image for the 1324 official cards:

| Orientation | Types | Cards | Size |
|---|---|---|---|
| Landscape | Battle 212, Mission 137, Location 74, Equipment 8 | 431 | 437×312 |
| Portrait | Character 476, Ground 209, Space 208 | 893 | 312×437 |

All cards of a type share an orientation, so `isLandscape(card)` (`ui/src/lib/cards.ts`) derives it from `type` without loading images.

- **Catalog grid:** a landscape tile spans two columns and a portrait tile one. At equal row height a landscape card is about twice as wide as a portrait one, so both fit the same rows. `CardGrid` uses only even column counts (2/4/6/8) and `grid-flow-dense` to back-fill gaps. Trade-off: dense packing can pull a later portrait card ahead of a wide one, so the sort order is only approximate when types are mixed. Filtering or grouping by type avoids it.
- **Card detail:** the modal sizes to the card's orientation.
- **Build mode (deck grid):** grouped by type, so portrait and landscape cards are in separate sections and need no dense packing.
- **Deck tray:** text rows, unaffected.
- **Rejected:** rotating landscape cards into portrait slots (text is hard to read) and letterboxing them into portrait cells (wastes about half the cell).

## 7. Data loading

At 1324 cards, fetch the whole catalog once (pages of 100) into an in-memory index cached in IndexedDB, and do filtering, sorting and faceting client-side. This makes the missing API filters and sort non-blockers. Deck cards (`card-id` + quantity only) are hydrated from this index.

## 8. Components & state

- Components: `CardTile`, `CardGrid`, `FilterBar`, `AdvancedFilters`, `CardDetail`, `DeckTray`, `DeckGrid`, `DeckList`, `CostCurve`, `TypeMeters`, `ValidationPanel`, `SaveStatus`, `UndoToast`, `NewDeckDialog`.
- State: catalog index (query cache), filters/sort (URL), active deck draft + pending-write queue (Zustand).

## 9. API gaps / backend work

| Pri | Item |
|---|---|
| P0 | Fix `API.md`: image URL pattern, error body shape (flat `{"error": "msg"}`), missing types Equipment/Location. |
| P0 | Confirmed: unknown query keys are used as SQL column names (`?nonexistent=1` → 500). Allowlist filter keys and return 400; this is a likely injection surface. |
| P0 | Creating a deck with a duplicate name returns 500 (`ArityException`), not the documented 409. Verified with curl. The UI pre-checks names client-side. |
| P1 | `PATCH /decks/:id` (rename, format, side). Deck rename in the UI is blocked on this. |
| P1 | Deck endpoint with hydrated cards (`?expand=cards`), and `POST /decks/validate` for drafts. |
| P1 | Cards: `sort`/`order`, total count, `rarity` / cost / power / health / `subtype` filters, `/cards/facets`. Optional for MVP given §7. |
| P1 | Return an explicit image URL, or document the convention. |
| P2 | Env-driven CORS origins; parsed classification (faction/era) tags; cleaned subtypes; `warnings` rules; `collection` table + endpoints. |

## 10. Phases

0. **Scaffold** `ui/`, proxy, typed client, catalog loader. *Done when:* the full catalog loads and images render.
1. **Catalog browser.** *Done when:* all filters combine, counts update live, state survives reload via URL (P2, P6).
2. **Deck list.** Create, duplicate, rename, delete (with confirm). *Done when:* multiple decks can be managed (P10).
3. **Workspace.** Done: mode toggle, tray, inline legality, autosave, undo, validation. `ui/src/pages/WorkspacePage.tsx`, `stores/deckDraft.ts`, `lib/deckRules.ts` (client-side mirror of the Clara rules), `components/{DeckTray,DeckCardTile,CostCurve,TypeMeters,ValidationPanel,SaveStatus,UndoToast}.tsx`. Card edits are optimistic and debounced (400ms) per card, then always refetch the deck — that refetch is what recomputes server-side validation and self-corrects any optimistic drift. Browse pre-filters to the deck's side + Neutral (P5) with an opt-out checkbox, since the API has no per-deck default. A validation violation for a type minimum can jump Browse to that type filter.
4. **Polish.** Spatial build view, keyboard nav, alt text (card name), mobile bottom-sheet tray, empty/error/loading states, export/import text list, printable view.
5. **Tests.** Vitest for filter/legality/validation logic; Playwright for build-a-deck and undo/autosave flows.

## 11. Open questions

- Format rules beyond the 60-card / 12-12-12 rules (the `format` field is a free string today).
- Whether deck side should be locked at creation.
- Auth / multi-user, if the app is ever shared.
