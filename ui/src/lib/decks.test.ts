import { describe, expect, it } from 'vitest'
import { copyName, deckCount } from './decks'

describe('deck helpers', () => {
  it('sums quantities', () => {
    expect(deckCount({ cards: [{ 'card-id': 'a', quantity: 4 }, { 'card-id': 'b', quantity: 2 }] })).toBe(6)
    expect(deckCount({ cards: [] })).toBe(0)
  })
  it('picks a unique copy name', () => {
    expect(copyName('Rebels', ['Rebels'])).toBe('Copy of Rebels')
    expect(copyName('Rebels', ['Rebels', 'Copy of Rebels'])).toBe('Copy of Rebels (2)')
    expect(copyName('Rebels', ['Copy of Rebels', 'Copy of Rebels (2)'])).toBe('Copy of Rebels (3)')
  })
})
