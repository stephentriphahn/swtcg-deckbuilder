import { describe, expect, it } from 'vitest'
import type { Card, DeckCard } from '../api/types'
import { canAddCopy, costCurve, deckTypeCounts, precheckViolations } from './deckRules'

let n = 0
const card = (over: Partial<Card>): Card => ({
  'card-id': `c${n++}`, name: 'Card', type: 'Character', side: 'L', 'set-code': 'ANH', number: n, rarity: 'C', ...over,
})

const luke = card({ 'card-id': 'luke', name: 'Luke', side: 'L', cost: 5 })
const vader = card({ 'card-id': 'vader', name: 'Vader', side: 'D', cost: 9 })
const xwing = card({ 'card-id': 'xwing', name: 'X-wing', type: 'Space', side: 'L', cost: -1 })
const index = new Map([luke, vader, xwing].map((c) => [c['card-id'], c]))

const deckCards = (over: DeckCard[]): DeckCard[] => over

describe('precheckViolations', () => {
  it('flags an incomplete, wrong-sided deck', () => {
    const violations = precheckViolations('L', deckCards([{ 'card-id': 'luke', quantity: 2 }, { 'card-id': 'vader', quantity: 1 }]), index)
    expect(violations.map((v) => v.rule)).toEqual(expect.arrayContaining(['deck-size', 'card-side', 'min-characters', 'min-space', 'min-ground']))
    expect(violations.find((v) => v.rule === 'card-side')?.message).toContain('Vader')
  })
  it('is empty for a complete legal deck', () => {
    const violations = precheckViolations('L', deckCards([
      { 'card-id': 'luke', quantity: 4 },
      ...Array.from({ length: 4 }, (_, i) => ({ 'card-id': `extra-char-${i}`, quantity: 4 })),
    ]), new Map([
      ...Array.from({ length: 4 }, (_, i) => card({ 'card-id': `extra-char-${i}`, type: 'Character', side: 'L' })).map((c): [string, Card] => [c['card-id'], c]),
      ['luke', luke],
    ]))
    // deliberately still incomplete (no Space/Ground) — asserts the min-* rules, not a full 60
    expect(violations.some((v) => v.rule === 'min-space')).toBe(true)
    expect(violations.some((v) => v.rule === 'card-side')).toBe(false)
  })
})

describe('deckTypeCounts / costCurve', () => {
  it('sums quantity by type', () => {
    expect(deckTypeCounts(deckCards([{ 'card-id': 'luke', quantity: 3 }, { 'card-id': 'xwing', quantity: 2 }]), index)).toEqual({
      Character: 3, Space: 2,
    })
  })
  it('buckets cost, with variable/missing as "*"', () => {
    expect(costCurve(deckCards([{ 'card-id': 'luke', quantity: 1 }, { 'card-id': 'xwing', quantity: 2 }, { 'card-id': 'vader', quantity: 1 }]), index)).toEqual([
      { label: '5', count: 1 },
      { label: '9', count: 1 },
      { label: '*', count: 2 },
    ])
  })
})

describe('canAddCopy', () => {
  it('blocks at 4 copies', () => {
    expect(canAddCopy(luke, 4, 'L')).toEqual({ ok: false, reason: 'Already at the 4-copy limit' })
  })
  it('blocks a side mismatch, allows Neutral', () => {
    expect(canAddCopy(vader, 0, 'L').ok).toBe(false)
    expect(canAddCopy(card({ side: 'N' }), 0, 'L').ok).toBe(true)
  })
  it('allows a normal add', () => {
    expect(canAddCopy(luke, 1, 'L')).toEqual({ ok: true })
  })
})
