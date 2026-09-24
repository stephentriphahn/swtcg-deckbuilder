# Catalog export script — plan

Status: **planned, not implemented.** Written from the `swtcg-game-engine` side (its slice 2 needs this
script's output) after reading this project's schema, load path, and real card data directly - not just
from `architecture.md`'s existing description. Companion doc there: `../swtcg-game-engine/doc/slice-2-plan.md`
(what the engine does with this script's output). This doc only specifies what the *export script* should
do, in this project; the engine-side interpretation (type/subtype parsing, side mapping, etc.) is
deliberately not this script's job - see "Not this script's job" at the end.

## Why this exists

`swtcg-game-engine` wants to run real cards instead of its 7-card hand-written test catalog, but shouldn't
gain a code dependency on this project (no Jetty/SQLite/migratus in the engine's classpath -
`architecture.md` §14, already decided). The bridge is a single EDN file this script produces from
`cards.db`, checked into (or generated into, then copied to) the engine project, read offline and
deterministically by its tests. This is the "read an EDN snapshot" half of that already-decided design;
this doc is what was missing - a real spec for the script itself.

## Prerequisite: fix `load-cards.clj`'s encoding before the next load

**Found during planning, real and current:** `tools/load_cards.clj`'s `read-tsv` opens each set file with
`(io/reader filename)` - no charset given, so it decodes with the JVM's platform default (UTF-8 on this
machine). At least 18 rows across the original-set TSVs contain a raw `0x96` byte (Windows-1252's EN DASH,
`–`) inside `Subtype` or `Type` text - not valid UTF-8 on its own. Decoded as UTF-8, `InputStreamReader`'s
default behavior on a malformed byte is to substitute the Unicode replacement character (`�`), silently -
no exception, no log line, just corrupted text sitting in the row from then on. Example (verified against
the raw bytes): `15TH.txt`, "Lando Calrissian's Charm (A)" - `Subtype` is `Character – Trait` in the
original file, decoded today as `Character � Trait`. Once a row like that is inserted into `cards.db`,
the correct text is gone - re-running the export can't recover it, only reloading from the original TSV
bytes with the right charset can.

**Fix, before running this export for real:** change `read-tsv` to open with `:encoding "windows-1252"`
(not `"ISO-8859-1"` - byte `0x96` means EN DASH in Windows-1252 but maps to an unused C1 control character
in true ISO-8859-1; Windows-1252 is very likely the actual source encoding of these TSVs and is a superset
of ISO-8859-1 everywhere else, so it's the strictly safer choice). Then **delete and rebuild `cards.db`**
(`load-cards-cli` skips sets already present, so a stale, already-corrupted set silently stays corrupted
unless the db file is removed first). `cards.db` at the workspace root is currently empty (0 bytes) in
this environment, so there's no existing corrupted state to worry about yet - just get the charset right
on the *first* real load.

This is called out here rather than left to be rediscovered because it's a one-line fix with a real,
silent-corruption cost if skipped, and it's naturally adjacent to this script's own work (both touch
`cards.db` and both care about exact string fidelity).

## What to export

**Cards - the whole table, every set that's loaded.** Use the existing `search-cards` hugsql fn with no
filter params (`(search-cards db {})`) - it already emits `SELECT * FROM cards ORDER BY card_id` with no
`WHERE` clause when every filter key is absent, so this is a straight "give me everything, stably ordered"
call with no new SQL needed. Row shape comes straight from `cards.sql`/the `cards` table via the existing
`as-unqualified-kebab-maps` builder-fn already wired in `db/sqlite.clj`:

```clojure
{:card-id "..." :name "..." :set-code "..." :image-file "..." :side "L" :type "Ground"
 :subtype "..." :cost 6 :speed 20 :power 3 :health 5 :rarity "R" :number 1
 :usage nil :text "..." :script nil :classification "..."}
```

**Decks - optional, skip for a first pass.** `get-decks` + `get-deck-cards` per deck would let real decks
(not just individually-picked real cards) load into the engine, but slice 2's actual goal is real *card*
data - the engine's own tests can already assemble ad hoc decks from any card ids, the same way
`test-catalog.clj`'s `deck-of` does today. Add `:decks [{:deck-id :name :owner :format :side :cards
[{:card-id :quantity}]}]` to the export later if/when there's a real reason to replay an actual saved
deck, rather than building it into the first version.

## Normalization the script itself should do

Keep this to hygiene, not interpretation (see "Not this script's job"). Concretely:

1. **Trim** every string field. Real data has trailing whitespace on `type` (`"Ground/Character "`,
   `"Space "`, `"Character "`, `"Equipment "`, `"Resource "`, `"Mission "`, `"Battle "` all appear, at
   least once each, across the original sets) and likely elsewhere. Untrimmed values would silently fail
   any exact-match `case`/`=` the engine-side adapter does.
2. **Numeric stat fields (`cost`, `speed`, `power`, `health`) - emit `nil`, not `-1`, for anything
   non-integer.** `load-cards.clj`'s current `parse-int` already coerces these four columns (via
   `process-card`'s generic `update ... parse-int` calls) the same way it coerces `number`, and treats a
   parse failure as `-1` for all of them alike. But `number` is cosmetic (never used in game arithmetic),
   while `cost`/`speed`/`power`/`health` feed directly into the engine's rules - a real card with
   `-1` power would be a landmine there (a fixed integer sentinel silently participating in comparisons
   like "damage ≥ health", not obviously invalid the way `nil` is). Concrete non-numeric values found in
   these four columns across the original sets: `"X"` (Cost, 33 rows - a real, meaningful "variable cost"
   TCG convention), `"*"` (Speed/Power/Health, 16/53/25 rows - "dynamic, computed from game state" per the
   existing code comment), and a lone `" "` space character (Speed/Power/Health, same rows counted above -
   almost certainly a data-entry gap, indistinguishable from `"*"` once both hit the same catch-all).
   `nil` is the right output for **all** of these today: the engine already has tested, safe handling for
   a `nil` stat (its own `odd-ground` test fixture exists specifically to exercise this; a property test
   invariant explicitly exempts `nil` health from the "no lethal survivors" check). A real `X`-cost or
   `*`-power card isn't computable yet either way - slice 2 doesn't add stat computation - so collapsing
   "genuinely dynamic" and "blank source data" into the same `nil` is an acceptable, honest simplification
   for now; a future slice that actually implements X-cost/variable-power cards can tell them apart by
   going back to `type`/`text`, not by anything this export loses.
3. **Leave `number`'s existing `-1` sentinel alone.** Already known and accepted (`architecture.md` §14:
   "39 rows have no valid `number`... mostly promos and previews") and never used in arithmetic, so it's
   not the same hazard as point 2. No change needed here.
4. **Filter to `side` in `{"L","D","N"}` - defensively, even though it should already hold.** Two real
   counter-examples exist in the raw TSVs: a `HELP` "set" (8 rows) that isn't real cards at all - it's
   turn-phase reminder text (`{Ready Phase}`, `{Attack Sequence}`, etc., `type = Reminder`, `side` blank -
   never load this set as cards) - and an `SBS` ("Star by Star") set with 194 rows whose `side` is `"Y"`,
   not `L`/`D`/`N` (likely a Yuuzhan-Vong-as-third-side convention from that expansion; the `cards` table's
   own `CHECK (side IN ('L','D','N'))` would reject every one of these on insert). Neither `HELP` nor `SBS`
   is in `load-cards.clj`'s `original-sets` default, so a default load never touches them - but if anyone
   ever loads a wider `:sets` list (or a future default expands), this filter keeps a bad row from ever
   reaching the exported EDN even if it somehow made it into `cards.db`. Log a count of anything filtered
   out here, so a future wider load doesn't silently lose 194 cards without anyone noticing.
5. **Sort by `card-id` before writing.** Deterministic file, stable diffs across regenerations - useful
   if this file ends up checked in anywhere, and harmless if not.

## Output

One EDN file, e.g. `resources/catalog-export.edn` in this project (exact path/name is your call to make
when implementing - not load-bearing for the engine side, which will just be told where to find it). Top
level: `{:cards [...]}` (a vector of the row maps above, post-normalization), `:decks` omitted for now per
"What to export" above. Write with `pr-str` + `spit`, or `clojure.pprint` if a human-diffable file is
wanted over a compact one - either reads back fine with `clojure.edn/read-string`.

## Suggested shape

A new `tools/export_catalog.clj` alongside the existing `tools/load_cards.clj`, same conventions (a
`-cli` entry fn, a `deps.edn` `:exec-fn` alias like the existing `:load-cards-sqlite`, e.g.
`:export-catalog`). Reuses `db/sqlite.clj`'s existing `search-cards`/`get-card-by-id` machinery - no new
SQL needed for the cards-only first pass (see "What to export" above).

## Verification before calling it done

- Round-trip: `clojure.edn/read-string` the written file back and confirm `(count (:cards read))` matches
  `SELECT count(*) FROM cards`.
- Spot-check the specific rows this planning session found tricky, by name, once the encoding fix has
  landed and `cards.db` has been rebuilt: "Lando Calrissian's Charm (A)" (`subtype` should read `Character
  – Trait` with a real en dash, not `�`), a card with `cost = "X"` in the raw TSV (should export as `nil`,
  not `-1` or a parse error), "Old Republic Strike Team (A)" (`type = "Ground/Character"` directly, no
  `" - "` suffix at all - confirms the export doesn't assume `type` is always a simple card-kind word).
- Confirm nothing from `HELP` or `SBS` (if ever loaded) makes it into the output.

## Not this script's job

Per `architecture.md` §14's already-decided split, kept unchanged here: interpreting `type`/`subtype` text
into arenas, mapping `side` codes to `:light`/`:dark`/`:neutral`, extracting unique-card version letters
from `name`, and any deck-legality logic all stay out of this script, in the engine's own
`catalog.clj` adapter (or, for legality, in this project's own validators, already separate). This script
gets accurate, clean, raw-shaped data out of `cards.db` and into EDN; it doesn't decide what any of it
*means* for gameplay.
