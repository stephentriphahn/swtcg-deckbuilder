import { useQueries, useQuery } from '@tanstack/react-query'
import { compareCards } from '../lib/cards'
import { fetchAllCards, getDeck, getPackOpening, listDecks, listPacks } from './client'

/** Whole catalog, loaded once and sorted; filtering happens client-side. */
export const useCatalog = () =>
  useQuery({
    queryKey: ['catalog'],
    queryFn: async () => (await fetchAllCards()).sort(compareCards),
    staleTime: Infinity,
  })

export const useDecks = () => useQuery({ queryKey: ['decks'], queryFn: listDecks })

export const useDeck = (id: string) =>
  useQuery({ queryKey: ['decks', id], queryFn: () => getDeck(id) })

/** Full deck (cards + validation) for each id; the list endpoint only returns summaries. */
export const useDeckDetails = (ids: string[]) =>
  useQueries({
    queries: ids.map((id) => ({ queryKey: ['decks', id], queryFn: () => getDeck(id) })),
  })

export const usePacks = () => useQuery({ queryKey: ['packs'], queryFn: listPacks })

export const usePackOpening = (openingId: string) =>
  useQuery({ queryKey: ['packs', 'openings', openingId], queryFn: () => getPackOpening(openingId) })
