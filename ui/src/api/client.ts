import type { Card, Deck, DeckCard, DeckSummary, NewDeck, PackOpening, PackSummary } from './types'

const BASE = '/api/v1'

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })
  if (!res.ok) {
    let message = res.statusText
    try {
      message = (await res.json()).error ?? message
    } catch {
      /* non-JSON error body */
    }
    throw new ApiError(res.status, message)
  }
  return res.status === 204 ? (undefined as T) : res.json()
}

const PAGE_SIZE = 100 // API max

/** Fetches the whole catalog (the API has no total count, so page until a short page). */
export async function fetchAllCards(): Promise<Card[]> {
  const all: Card[] = []
  for (let skip = 0; ; skip += PAGE_SIZE) {
    const { cards } = await request<{ cards: Card[] }>(
      `/cards?limit=${PAGE_SIZE}&skip=${skip}`,
    )
    all.push(...cards)
    if (cards.length < PAGE_SIZE) return all
  }
}

export const listDecks = () => request<DeckSummary[]>('/decks')
export const getDeck = (id: string) => request<Deck>(`/decks/${id}`)
export const createDeck = (deck: NewDeck) =>
  request<DeckSummary>('/decks', { method: 'POST', body: JSON.stringify(deck) })
export const deleteDeck = (id: string) =>
  request<void>(`/decks/${id}`, { method: 'DELETE' })
export const setDeckCard = (deckId: string, cardId: string, quantity: number) =>
  request<DeckCard>(`/decks/${deckId}/cards/${cardId}`, {
    method: 'PUT',
    body: JSON.stringify({ quantity }),
  })
export const removeDeckCard = (deckId: string, cardId: string) =>
  request<void>(`/decks/${deckId}/cards/${cardId}`, { method: 'DELETE' })

/** Bulk add/set quantities. Quantity is set (upsert), not incremented. */
export const addDeckCards = (deckId: string, cards: DeckCard[]) =>
  request<DeckCard[]>(`/decks/${deckId}/cards`, {
    method: 'POST',
    body: JSON.stringify(cards),
  })

export const listPacks = () => request<PackSummary[]>('/packs')
export const openPack = (owner: string, setCode: string) =>
  request<PackOpening>('/packs/open', {
    method: 'POST',
    body: JSON.stringify({ owner, 'set-code': setCode }),
  })
export const getPackOpening = (openingId: string) =>
  request<PackOpening>(`/packs/openings/${openingId}`)
