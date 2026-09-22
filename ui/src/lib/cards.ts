import type { Card, CardType } from '../api/types'

/** Image URL convention: /setimages/{set-code}/{image-file}.jpg (served by the API). */
export function cardImageUrl(card: Card): string | null {
  return card['image-file']
    ? `/setimages/${card['set-code']}/${card['image-file']}.jpg`
    : null
}

/**
 * Battle, Mission, Location and Equipment art is landscape (437x312); Character, Space and
 * Ground are portrait (312x437). Verified against every image for the 1324 official cards,
 * so orientation is derived from `type` without loading images.
 */
const LANDSCAPE_TYPES: ReadonlySet<CardType> = new Set(['Battle', 'Mission', 'Location', 'Equipment'])

export const isLandscape = (card: Card): boolean => LANDSCAPE_TYPES.has(card.type)

/** Stats use -1 as a sentinel for a variable value, shown as "*". */
export function formatStat(value: number | null | undefined): string {
  if (value == null) return '–'
  return value === -1 ? '*' : String(value)
}

export const SET_ORDER = [
  'ANH', 'ESB', 'ROTJ', 'AOTC', 'ROTS', 'SR', 'BOY', 'RAS', 'JG', 'PM',
] as const

/** Default sort: set, then card number (promos with no number last), then name. */
export function compareCards(a: Card, b: Card): number {
  const set = SET_ORDER.indexOf(a['set-code'] as never) - SET_ORDER.indexOf(b['set-code'] as never)
  if (set !== 0) return set
  const an = a.number ?? Infinity
  const bn = b.number ?? Infinity
  if (an !== bn) return an < bn ? -1 : 1
  return a.name.localeCompare(b.name)
}

/** Card text separates abilities with " | " (e.g. "Upkeep: ... | Armor | Bounty: ..."). */
export const splitText = (text: string | null | undefined): string[] =>
  (text ?? '').split('|').map((s) => s.trim()).filter(Boolean)
