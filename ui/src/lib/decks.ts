import type { Deck } from '../api/types'

export const DECK_SIZE = 60
export const SIDE_NAMES = { L: 'Light', D: 'Dark' } as const

export const deckCount = (deck: Pick<Deck, 'cards'>): number =>
  deck.cards.reduce((n, c) => n + c.quantity, 0)

/** "Copy of X", then "Copy of X (2)", ... — deck names are unique in the API. */
export function copyName(name: string, existing: readonly string[]): string {
  const taken = new Set(existing)
  const base = `Copy of ${name}`
  if (!taken.has(base)) return base
  for (let i = 2; ; i++) if (!taken.has(`${base} (${i})`)) return `${base} (${i})`
}
