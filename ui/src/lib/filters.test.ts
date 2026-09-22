import { describe, expect, it } from 'vitest'
import type { Card } from '../api/types'
import {
  EMPTY_FILTERS, activeFilters, applyFilters, facetCounts, parseFilters, serializeFilters,
  type Filters,
} from './filters'

let n = 0
const card = (over: Partial<Card>): Card => ({
  'card-id': String(n++), name: 'Card', type: 'Character', side: 'L',
  'set-code': 'ANH', number: n, rarity: 'C', ...over,
})

const cards = [
  card({ name: 'Luke', side: 'L', cost: 5, power: 4, text: 'Evade | Pilot', classification: 'WOTC, REB, EP456' }),
  card({ name: 'Vader', side: 'D', cost: 9, power: 8, text: 'Critical Hit', classification: 'WOTC, REB, EP456', rarity: 'R' }),
  card({ name: 'X-wing', type: 'Space', side: 'L', 'set-code': 'ESB', cost: 3, power: -1, subtype: 'Rebel Starfighter', classification: 'WOTC, REB' }),
  card({ name: 'Mission A', type: 'Mission', side: 'N', 'set-code': 'ESB', cost: null }),
]
const f = (over: Partial<Filters>): Filters => ({ ...EMPTY_FILTERS, ...over })
const names = (fs: Filters) => applyFilters(cards, fs).map((c) => c.name)

describe('filters', () => {
  it('ORs within a facet and ANDs across facets', () => {
    expect(names(f({ sides: ['L', 'D'] }))).toEqual(['Luke', 'Vader', 'X-wing'])
    expect(names(f({ sides: ['L', 'D'], types: ['Space'] }))).toEqual(['X-wing'])
  })
  it('searches name and text', () => {
    expect(names(f({ q: 'vader' }))).toEqual(['Vader'])
    expect(names(f({ q: 'pilot' }))).toEqual(['Luke'])
  })
  it('matches keywords on word boundaries and tags', () => {
    expect(names(f({ keywords: ['Evade'] }))).toEqual(['Luke'])
    expect(names(f({ tags: ['EP456'] }))).toEqual(['Luke', 'Vader'])
  })
  it('ranges exclude null and variable (-1) stats', () => {
    expect(names(f({ cost: { min: 4 } }))).toEqual(['Luke', 'Vader'])
    expect(names(f({ power: { max: 10 } }))).toEqual(['Luke', 'Vader'])
  })
  it('filters subtype by substring', () => {
    expect(names(f({ subtype: 'starfighter' }))).toEqual(['X-wing'])
  })
  it('sorts by cost with unknown cost last', () => {
    expect(names(f({ sort: 'cost' }))).toEqual(['X-wing', 'Luke', 'Vader', 'Mission A'])
  })
  it('counts a facet ignoring its own selection', () => {
    const counts = facetCounts(cards, f({ sides: ['D'], types: ['Space'] }))
    expect(counts.sides.get('L')).toBe(1) // Space cards on the Light side, though Dark is selected
    expect(counts.types.get('Character')).toBe(1) // Dark cards by type, ignoring the Space selection
  })
  it('round-trips through the URL and omits defaults', () => {
    const fs = f({ q: 'a', sides: ['L'], keywords: ['Critical Hit'], cost: { min: 2, max: 5 }, health: { max: 3 }, sort: 'cost' })
    expect(parseFilters(serializeFilters(fs))).toEqual(fs)
    expect(serializeFilters(EMPTY_FILTERS).toString()).toBe('')
  })
  it('ignores junk URL values', () => {
    const p = new URLSearchParams('side=L,X&type=Bogus&sort=nope&cost=abc')
    expect(parseFilters(p)).toEqual({ ...EMPTY_FILTERS, sides: ['L'] })
  })
  it('lists removable active filters', () => {
    const fs = f({ sides: ['L'], cost: { min: 2 } })
    const active = activeFilters(fs)
    expect(active.map((a) => a.label)).toEqual(['Light', 'Cost 2–…'])
    expect(active[0].remove(fs).sides).toEqual([])
  })
})
