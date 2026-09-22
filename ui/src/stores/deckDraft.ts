import { create } from 'zustand'
import type { DeckCard } from '../api/types'

export type CardStatus = 'saving' | 'error' | undefined

interface UndoEntry {
  deckId: string
  card: DeckCard
}

interface DeckDraftState {
  /** Per-card write status, for the ambient SaveStatus indicator (P9). */
  pending: Record<string, CardStatus>
  /** The most recently removed card, for the Undo toast (P7). Cleared on undo or timeout. */
  undo: UndoEntry | null
  setPending: (cardId: string, status: CardStatus) => void
  setUndo: (entry: UndoEntry | null) => void
}

export const useDeckDraftStore = create<DeckDraftState>((set) => ({
  pending: {},
  undo: null,
  setPending: (cardId, status) =>
    set((s) => {
      const pending = { ...s.pending }
      if (status) pending[cardId] = status
      else delete pending[cardId]
      return { pending }
    }),
  setUndo: (entry) => set({ undo: entry }),
}))

export const hasPendingWrites = (pending: Record<string, CardStatus>): boolean =>
  Object.values(pending).some((s) => s === 'saving')
export const hasErrors = (pending: Record<string, CardStatus>): boolean =>
  Object.values(pending).some((s) => s === 'error')
