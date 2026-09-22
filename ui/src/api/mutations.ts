import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRef } from 'react'
import { copyName } from '../lib/decks'
import { useDeckDraftStore } from '../stores/deckDraft'
import { addDeckCards, createDeck, deleteDeck, getDeck, removeDeckCard, setDeckCard } from './client'
import type { Deck, DeckCard, NewDeck } from './types'

const useInvalidateDecks = () => {
  const qc = useQueryClient()
  return () => qc.invalidateQueries({ queryKey: ['decks'] })
}

export function useCreateDeck() {
  const invalidate = useInvalidateDecks()
  return useMutation({ mutationFn: (deck: NewDeck) => createDeck(deck), onSuccess: invalidate })
}

export function useDeleteDeck() {
  const invalidate = useInvalidateDecks()
  return useMutation({ mutationFn: (id: string) => deleteDeck(id), onSettled: invalidate })
}

/** Create + bulk add. If copying the cards fails, the half-made deck is removed. */
export function useDuplicateDeck() {
  const invalidate = useInvalidateDecks()
  return useMutation({
    mutationFn: async ({ id, existingNames }: { id: string; existingNames: string[] }) => {
      const source = await getDeck(id)
      const copy = await createDeck({
        name: copyName(source.name, existingNames),
        owner: source.owner,
        format: source.format,
        side: source.side,
      })
      if (source.cards.length > 0) {
        try {
          await addDeckCards(
            copy['deck-id'],
            source.cards.map((c) => ({ 'card-id': c['card-id'], quantity: c.quantity })),
          )
        } catch (e) {
          await deleteDeck(copy['deck-id']).catch(() => {})
          throw e
        }
      }
      return copy
    },
    onSettled: invalidate,
  })
}

// ---- deck card edits (autosave, P9) --------------------------------------------------

function upsertCard(cards: DeckCard[], cardId: string, quantity: number): DeckCard[] {
  if (quantity <= 0) return cards.filter((c) => c['card-id'] !== cardId)
  return cards.some((c) => c['card-id'] === cardId)
    ? cards.map((c) => (c['card-id'] === cardId ? { ...c, quantity } : c))
    : [...cards, { 'card-id': cardId, quantity }]
}

const DEBOUNCE_MS = 400

/**
 * Optimistic, debounced per-card quantity edits. The query cache updates instantly so the UI
 * never waits on the network; the write itself is debounced per card so a run of +/- clicks
 * sends one request with the final quantity. Every write, success or failure, is followed by
 * a refetch — only a full GET recomputes server-side validation, and the refetch also
 * self-corrects any optimistic drift if a write actually failed.
 */
export function useDeckCardControls(deckId: string) {
  const qc = useQueryClient()
  const setPending = useDeckDraftStore((s) => s.setPending)
  const setUndo = useDeckDraftStore((s) => s.setUndo)
  const timers = useRef(new Map<string, ReturnType<typeof setTimeout>>()).current

  const quantityOf = (cardId: string): number =>
    qc.getQueryData<Deck>(['decks', deckId])?.cards.find((c) => c['card-id'] === cardId)?.quantity ?? 0

  const send = (cardId: string, quantity: number) => {
    setPending(cardId, 'saving')
    const write = quantity <= 0 ? removeDeckCard(deckId, cardId) : setDeckCard(deckId, cardId, quantity)
    write
      .then(() => setPending(cardId, undefined))
      .catch(() => setPending(cardId, 'error'))
      .finally(() => qc.invalidateQueries({ queryKey: ['decks', deckId] }))
  }

  const setQuantity = (cardId: string, quantity: number) => {
    const previous = quantityOf(cardId)
    if (quantity === previous) return
    qc.setQueryData<Deck>(['decks', deckId], (old) =>
      old ? { ...old, cards: upsertCard(old.cards, cardId, quantity) } : old,
    )
    if (quantity === 0 && previous > 0) setUndo({ deckId, card: { 'card-id': cardId, quantity: previous } })
    clearTimeout(timers.get(cardId))
    timers.set(cardId, setTimeout(() => send(cardId, quantity), DEBOUNCE_MS))
  }

  const retry = (cardId: string) => send(cardId, quantityOf(cardId))

  return { quantityOf, setQuantity, retry }
}
