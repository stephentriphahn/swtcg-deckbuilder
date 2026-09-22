# UX Principles — Card Catalog / Deck Builder

Principles for a card collection viewer that also supports a deck-building mode.

---

## 1. Two clearly distinct modes, one shared vocabulary
Collection view ("what do I own") and deck-building view ("what am I putting together") should
feel like different modes, not different apps. Same card component, same filter bar, same visual
language — just a different *purpose* layered on top. Switching should feel like flipping a
toggle, not navigating away.

## 2. Never make the user lose their place
If someone is scrolling deep into a filtered card list and switches to check their deck, coming
back should restore scroll position, filters, and sort order exactly as they left them. This is
one of the most-violated principles in deck builders and it's a top source of rage-quits.

## 3. Adding a card should never leave the catalog
Don't navigate away to "add to deck." A card added to the deck should update in place (badge,
counter, highlight, dimmed-if-maxed) while the user stays in the browsing flow. Think "add to
cart," not "add to cart, now redirected to your cart."

## 4. Make deck state visible everywhere, persistently
A persistent element — sidebar, drawer, sticky footer — showing current deck size, mana/cost
curve, or copies-of-this-card-in-deck should be visible from the catalog view too, not just
deck-builder mode. People building decks are constantly cross-referencing "what do I have vs.
what do I need."

## 5. Show ownership and legality inline, at a glance
In the catalog: how many copies you own, how many are already in the current deck, and whether
adding another is legal (max copies, format restrictions) — all visible without a click. Grey out
or badge cards you don't own rather than hiding them; people want to browse the full card pool and
drool over what they're missing.

## 6. Fast, layered filtering — not one giant form
Card catalogs live and die on filter/search speed. Favor: a persistent search bar, quick-toggle
chips for common facets (suit, cost, type), and an "advanced filters" drawer for the long tail.
Combine filters additively and show result count updating live.

## 7. Undo over confirm
Removing a card from a deck should never require a confirmation modal — that's friction for a
low-stakes, instantly reversible action. Do it immediately and offer a brief "Undo" toast instead.
Save confirmation dialogs for destructive, hard-to-reverse actions (deleting an entire deck).

## 8. Deck building should feel spatial, not list-only
A flat list of card names in a deck is functional but sterile. Even a lightweight visual grid
(mini card art, cost curve chart, type breakdown) helps people reason about their deck's shape at
a glance — this is a huge part of why physical card games *feel* good to build for.

## 9. Autosave, always
Nobody should ever lose a deck because they navigated away or closed a tab. Save on every change
(debounced), and make "Saved" a quiet, ambient status indicator rather than a button someone has
to remember to press.

## 10. Support "drafting" without commitment
Let people build/save multiple in-progress decks, duplicate a deck as a starting point, and freely
rename/discard drafts. Deck building is iterative and exploratory by nature — the UI shouldn't
imply there's one canonical deck they're editing.

---

## Mental model

The catalog is a **library you browse**, the deck is a **cart you fill**, and switching between
them should feel as lightweight as glancing between two panes — because in spirit, that's often
literally what it should be (side-by-side on desktop, tab-switch with persistent state on mobile).
