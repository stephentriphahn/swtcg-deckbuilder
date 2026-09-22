import { beforeEach, describe, expect, it } from 'vitest'
import { hasErrors, hasPendingWrites, useDeckDraftStore } from './deckDraft'

beforeEach(() => useDeckDraftStore.setState({ pending: {}, undo: null }))

describe('deckDraft store', () => {
  it('tracks and clears per-card status', () => {
    const { setPending } = useDeckDraftStore.getState()
    setPending('a', 'saving')
    expect(hasPendingWrites(useDeckDraftStore.getState().pending)).toBe(true)
    setPending('a', 'error')
    expect(hasErrors(useDeckDraftStore.getState().pending)).toBe(true)
    setPending('a', undefined)
    expect(useDeckDraftStore.getState().pending).toEqual({})
  })
  it('holds one undo entry at a time', () => {
    const { setUndo } = useDeckDraftStore.getState()
    setUndo({ deckId: 'd1', card: { 'card-id': 'a', quantity: 2 } })
    expect(useDeckDraftStore.getState().undo?.card.quantity).toBe(2)
    setUndo(null)
    expect(useDeckDraftStore.getState().undo).toBeNull()
  })
})
