import type { Card, CardType, Rarity, Side } from '../api/types'
import { SET_ORDER, compareCards } from './cards'

export const SIDES: Side[] = ['L', 'D', 'N']
export const SIDE_LABELS: Record<Side, string> = { L: 'Light', D: 'Dark', N: 'Neutral' }
export const TYPES: CardType[] = [
  'Character', 'Space', 'Ground', 'Battle', 'Mission', 'Location', 'Equipment',
]
export const SETS: readonly string[] = SET_ORDER
export const RARITIES: Rarity[] = ['C', 'U', 'R', 'P']
export const RARITY_LABELS: Record<Rarity, string> = {
  C: 'Common', U: 'Uncommon', R: 'Rare', P: 'Promo',
}
/** Format/era tags found in `classification` (WOTC and the variant codes are noise). */
export const TAGS = ['REB', 'REP', 'CW', 'EP456'] as const
/** Keywords on official cards (see CLAUDE.md "Keyword scope"). */
export const KEYWORDS = [
  'Evade', 'Accuracy', 'Critical Hit', 'Pilot', 'Shields', 'Armor', 'Intercept', 'Upkeep',
  'Retaliate', 'Stun', 'Bounty', 'Lucky', 'Deflect', 'Bombard', 'Overkill', 'Stack',
  'Reserves', 'Enhance', 'Parry', 'Equip', 'Hidden Cost', 'Ion Cannon',
] as const

export type SortKey = 'set' | 'name' | 'cost' | 'type'
export const SORTS: { key: SortKey; label: string }[] = [
  { key: 'set', label: 'Set & number' },
  { key: 'name', label: 'Name' },
  { key: 'cost', label: 'Cost' },
  { key: 'type', label: 'Type' },
]

export interface Range {
  min?: number
  max?: number
}
export type StatKey = 'cost' | 'power' | 'health'
export const STAT_KEYS: StatKey[] = ['cost', 'power', 'health']

export interface Filters {
  q: string
  sides: Side[]
  types: CardType[]
  sets: string[]
  rarities: Rarity[]
  tags: string[]
  keywords: string[]
  subtype: string
  cost: Range
  power: Range
  health: Range
  sort: SortKey
}

export const EMPTY_FILTERS: Filters = {
  q: '', sides: [], types: [], sets: [], rarities: [], tags: [], keywords: [], subtype: '',
  cost: {}, power: {}, health: {}, sort: 'set',
}

/** Facets that are multi-select chips: OR within a facet, AND across facets. */
export type FacetKey = 'sides' | 'types' | 'sets' | 'rarities' | 'tags' | 'keywords'

// ---- card properties ----------------------------------------------------------------

const tagCache = new WeakMap<Card, string[]>()
export function tagsOf(card: Card): string[] {
  let tags = tagCache.get(card)
  if (!tags) {
    const raw = (card.classification ?? '').split(',').map((t) => t.trim())
    tags = TAGS.filter((t) => raw.includes(t))
    tagCache.set(card, tags)
  }
  return tags
}

const kwRegexes = KEYWORDS.map((k) => [k, new RegExp(`\\b${k}\\b`, 'i')] as const)
const kwCache = new WeakMap<Card, string[]>()
export function keywordsOf(card: Card): string[] {
  let kws = kwCache.get(card)
  if (!kws) {
    const text = card.text ?? ''
    kws = kwRegexes.filter(([, re]) => re.test(text)).map(([k]) => k)
    kwCache.set(card, kws)
  }
  return kws
}

const inRange = (value: number | null | undefined, { min, max }: Range): boolean => {
  if (min == null && max == null) return true
  // null and -1 (variable "*") have no comparable value, so they never match a range
  if (value == null || value < 0) return false
  return (min == null || value >= min) && (max == null || value <= max)
}

// ---- filtering ----------------------------------------------------------------------

/** `skip` leaves one facet out, used to count how many results each of its chips would give. */
export function matches(card: Card, f: Filters, skip?: FacetKey): boolean {
  if (skip !== 'sides' && f.sides.length && !f.sides.includes(card.side)) return false
  if (skip !== 'types' && f.types.length && !f.types.includes(card.type)) return false
  if (skip !== 'sets' && f.sets.length && !f.sets.includes(card['set-code'])) return false
  if (skip !== 'rarities' && f.rarities.length && !f.rarities.includes(card.rarity)) return false
  if (skip !== 'tags' && f.tags.length && !tagsOf(card).some((t) => f.tags.includes(t))) return false
  if (skip !== 'keywords' && f.keywords.length && !keywordsOf(card).some((k) => f.keywords.includes(k)))
    return false
  if (f.subtype && !(card.subtype ?? '').toLowerCase().includes(f.subtype.toLowerCase())) return false
  if (f.q) {
    const q = f.q.toLowerCase()
    if (!card.name.toLowerCase().includes(q) && !(card.text ?? '').toLowerCase().includes(q)) return false
  }
  return STAT_KEYS.every((k) => inRange(card[k], f[k]))
}

const TYPE_RANK = (c: Card) => TYPES.indexOf(c.type)

export function sortCards(cards: Card[], sort: SortKey): Card[] {
  const by: Record<SortKey, (a: Card, b: Card) => number> = {
    set: compareCards,
    name: (a, b) => a.name.localeCompare(b.name) || compareCards(a, b),
    cost: (a, b) => (a.cost ?? Infinity) - (b.cost ?? Infinity) || compareCards(a, b),
    type: (a, b) => TYPE_RANK(a) - TYPE_RANK(b) || compareCards(a, b),
  }
  // `cards` is already in set order from the catalog query, and Array.sort is stable
  return [...cards].sort(by[sort])
}

export const applyFilters = (cards: Card[], f: Filters): Card[] =>
  sortCards(cards.filter((c) => matches(c, f)), f.sort)

const FACET_VALUES: Record<FacetKey, (c: Card) => string[]> = {
  sides: (c) => [c.side],
  types: (c) => [c.type],
  sets: (c) => [c['set-code']],
  rarities: (c) => [c.rarity],
  tags: tagsOf,
  keywords: keywordsOf,
}

/** Per-facet chip counts, each computed with every filter except its own facet applied. */
export function facetCounts(cards: Card[], f: Filters): Record<FacetKey, Map<string, number>> {
  const out = {} as Record<FacetKey, Map<string, number>>
  for (const key of Object.keys(FACET_VALUES) as FacetKey[]) {
    const counts = new Map<string, number>()
    for (const c of cards) {
      if (!matches(c, f, key)) continue
      for (const v of FACET_VALUES[key](c)) counts.set(v, (counts.get(v) ?? 0) + 1)
    }
    out[key] = counts
  }
  return out
}

// ---- URL <-> filters ----------------------------------------------------------------

const list = <T extends string>(p: URLSearchParams, key: string, allowed: readonly T[]): T[] =>
  (p.get(key) ?? '').split(',').filter((v): v is T => (allowed as readonly string[]).includes(v))

function parseRange(raw: string | null): Range {
  const m = /^(\d*)-(\d*)$/.exec(raw ?? '')
  if (!m) return {}
  const r: Range = {}
  if (m[1]) r.min = Number(m[1])
  if (m[2]) r.max = Number(m[2])
  return r
}
const formatRange = ({ min, max }: Range) => `${min ?? ''}-${max ?? ''}`
const hasRange = (r: Range) => r.min != null || r.max != null

export function parseFilters(p: URLSearchParams): Filters {
  const sort = p.get('sort')
  return {
    q: p.get('q') ?? '',
    sides: list(p, 'side', SIDES),
    types: list(p, 'type', TYPES),
    sets: list(p, 'set', SETS),
    rarities: list(p, 'rarity', RARITIES),
    tags: list(p, 'tag', TAGS),
    keywords: list(p, 'kw', KEYWORDS),
    subtype: p.get('subtype') ?? '',
    cost: parseRange(p.get('cost')),
    power: parseRange(p.get('power')),
    health: parseRange(p.get('health')),
    sort: SORTS.some((s) => s.key === sort) ? (sort as SortKey) : 'set',
  }
}

/** Defaults are omitted so the URL only carries what the user changed. */
export function serializeFilters(f: Filters): URLSearchParams {
  const p = new URLSearchParams()
  const put = (k: string, v: string | string[]) => {
    const s = Array.isArray(v) ? v.join(',') : v
    if (s) p.set(k, s)
  }
  put('q', f.q)
  put('side', f.sides)
  put('type', f.types)
  put('set', f.sets)
  put('rarity', f.rarities)
  put('tag', f.tags)
  put('kw', f.keywords)
  put('subtype', f.subtype)
  for (const k of STAT_KEYS) if (hasRange(f[k])) p.set(k, formatRange(f[k]))
  if (f.sort !== 'set') p.set('sort', f.sort)
  return p
}

// ---- active filter chips ------------------------------------------------------------

export interface ActiveFilter {
  id: string
  label: string
  remove: (f: Filters) => Filters
}

const without = <T>(xs: T[], x: T) => xs.filter((y) => y !== x)

export function activeFilters(f: Filters): ActiveFilter[] {
  const out: ActiveFilter[] = []
  if (f.q) out.push({ id: 'q', label: `“${f.q}”`, remove: (x) => ({ ...x, q: '' }) })
  f.sides.forEach((v) =>
    out.push({ id: `side:${v}`, label: SIDE_LABELS[v], remove: (x) => ({ ...x, sides: without(x.sides, v) }) }))
  f.types.forEach((v) =>
    out.push({ id: `type:${v}`, label: v, remove: (x) => ({ ...x, types: without(x.types, v) }) }))
  f.sets.forEach((v) =>
    out.push({ id: `set:${v}`, label: v, remove: (x) => ({ ...x, sets: without(x.sets, v) }) }))
  f.rarities.forEach((v) =>
    out.push({ id: `rarity:${v}`, label: RARITY_LABELS[v], remove: (x) => ({ ...x, rarities: without(x.rarities, v) }) }))
  f.tags.forEach((v) =>
    out.push({ id: `tag:${v}`, label: v, remove: (x) => ({ ...x, tags: without(x.tags, v) }) }))
  f.keywords.forEach((v) =>
    out.push({ id: `kw:${v}`, label: v, remove: (x) => ({ ...x, keywords: without(x.keywords, v) }) }))
  if (f.subtype)
    out.push({ id: 'subtype', label: `Subtype: ${f.subtype}`, remove: (x) => ({ ...x, subtype: '' }) })
  for (const k of STAT_KEYS) {
    const { min, max } = f[k]
    if (min != null || max != null)
      out.push({
        id: k,
        label: `${k[0].toUpperCase()}${k.slice(1)} ${min ?? '…'}–${max ?? '…'}`,
        remove: (x) => ({ ...x, [k]: {} }),
      })
  }
  return out
}
