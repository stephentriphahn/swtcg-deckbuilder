import type { Card, CardType, DeckCard, Side, Violation } from '../api/types'

export type CardIndex = Map<string, Card>

/** Mirrors the server's `>=12` rules in validation/rules.clj. */
export const MIN_RULES: { type: CardType; min: number; rule: string }[] = [
  { type: 'Character', min: 12, rule: 'min-characters' },
  { type: 'Space', min: 12, rule: 'min-space' },
  { type: 'Ground', min: 12, rule: 'min-ground' },
]

const sideName = (s: Side) => (s === 'L' ? 'Light' : s === 'D' ? 'Dark' : 'Neutral')

const resolve = (cards: DeckCard[], index: CardIndex) =>
  cards
    .map((dc) => ({ dc, card: index.get(dc['card-id']) }))
    .filter((x): x is { dc: DeckCard; card: Card } => x.card != null)

export const deckSize = (cards: DeckCard[]): number => cards.reduce((n, c) => n + c.quantity, 0)

export function deckTypeCounts(cards: DeckCard[], index: CardIndex): Record<string, number> {
  const counts: Record<string, number> = {}
  for (const { dc, card } of resolve(cards, index)) counts[card.type] = (counts[card.type] ?? 0) + dc.quantity
  return counts
}

const COST_ORDER = [...Array(10).keys()].map(String).concat('10+', '*')

/** Cost buckets 0-9, "10+", and "*" for missing/variable cost, in that display order. */
export function costCurve(cards: DeckCard[], index: CardIndex): { label: string; count: number }[] {
  const buckets = new Map<string, number>()
  for (const { dc, card } of resolve(cards, index)) {
    const label = card.cost == null || card.cost < 0 ? '*' : card.cost >= 10 ? '10+' : String(card.cost)
    buckets.set(label, (buckets.get(label) ?? 0) + dc.quantity)
  }
  return COST_ORDER.filter((l) => buckets.has(l)).map((label) => ({ label, count: buckets.get(label)! }))
}

/**
 * Client-side mirror of the server's Clara rulebase (validation/rules.clj), so violations
 * update instantly while editing instead of waiting on a round trip. The server's own result,
 * refreshed after every save, remains authoritative — this is advisory only, same as the API.
 */
export function precheckViolations(side: Side, cards: DeckCard[], index: CardIndex): Violation[] {
  const violations: Violation[] = []
  const size = deckSize(cards)
  if (size !== 60)
    violations.push({ rule: 'deck-size', message: `Deck must contain exactly 60 cards (currently has ${size})` })

  for (const { dc, card } of resolve(cards, index)) {
    if (card.side !== 'N' && card.side !== side)
      violations.push({
        rule: 'card-side',
        message: `Card '${card.name}' is ${sideName(card.side)} side, but this is a ${sideName(side)} side deck`,
      })
    if (dc.quantity > 4)
      violations.push({ rule: 'card-quantity', message: `Card '${card['card-id']}' has more than 4 copies` })
  }

  const counts = deckTypeCounts(cards, index)
  for (const { type, min, rule } of MIN_RULES) {
    const count = counts[type] ?? 0
    if (count < min)
      violations.push({ rule, message: `Deck must contain at least ${min} ${type} cards (currently has ${count})` })
  }
  return violations
}

export interface AddCheck {
  ok: boolean
  reason?: string
}

/** Whether one more copy of `card` may be added to a deck of `deckSide` (P5 inline legality). */
export function canAddCopy(card: Card, currentQuantity: number, deckSide: Side): AddCheck {
  if (currentQuantity >= 4) return { ok: false, reason: 'Already at the 4-copy limit' }
  if (card.side !== 'N' && card.side !== deckSide)
    return { ok: false, reason: `${sideName(card.side)} side card doesn't match this ${sideName(deckSide)} side deck` }
  return { ok: true }
}
