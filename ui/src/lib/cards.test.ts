import { describe, expect, it } from 'vitest'
import type { Card } from '../api/types'
import { cardImageUrl, compareCards, formatStat, isLandscape, splitText } from './cards'

const card = (over: Partial<Card>): Card => ({
  'card-id': 'x', name: 'A', type: 'Character', side: 'L',
  'set-code': 'AOTC', number: 1, rarity: 'C', ...over,
})

describe('cards helpers', () => {
  it('builds the image url', () => {
    expect(cardImageUrl(card({ 'image-file': 'Anakin_Skywalker_A' }))).toBe(
      '/setimages/AOTC/Anakin_Skywalker_A.jpg',
    )
    expect(cardImageUrl(card({}))).toBeNull()
  })
  it('formats variable stats', () => {
    expect(formatStat(-1)).toBe('*')
    expect(formatStat(0)).toBe('0')
    expect(formatStat(null)).toBe('–')
  })
  it('sorts by set, number (null last), name', () => {
    const list = [
      card({ name: 'P', 'set-code': 'AOTC', number: null }),
      card({ name: 'B', 'set-code': 'AOTC', number: 2 }),
      card({ name: 'A', 'set-code': 'ANH', number: 9 }),
    ].sort(compareCards)
    expect(list.map((c) => c.name)).toEqual(['A', 'B', 'P'])
  })
  it('knows which types are landscape', () => {
    for (const type of ['Battle', 'Mission', 'Location', 'Equipment'] as const)
      expect(isLandscape(card({ type }))).toBe(true)
    for (const type of ['Character', 'Space', 'Ground'] as const)
      expect(isLandscape(card({ type }))).toBe(false)
  })
})

describe('splitText', () => {
  it('splits abilities on pipes and drops blanks', () => {
    expect(splitText('Armor | Bounty: 1 build point. |  ')).toEqual(['Armor', 'Bounty: 1 build point.'])
    expect(splitText(null)).toEqual([])
  })
})
