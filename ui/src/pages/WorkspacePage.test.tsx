import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from '../api/client'
import type { Card, Deck } from '../api/types'
import { useDeckDraftStore } from '../stores/deckDraft'
import { WorkspacePage } from './WorkspacePage'

vi.mock('../api/client')

let n = 0
const card = (over: Partial<Card>): Card => ({
  'card-id': `c${n++}`, name: `Card ${n}`, type: 'Character', side: 'L', 'set-code': 'ANH', number: n, rarity: 'C', ...over,
})

const luke = card({ 'card-id': 'luke', name: 'Luke', side: 'L' })
const vader = card({ 'card-id': 'vader', name: 'Vader', side: 'D' })
const catalog = [luke, vader]

const deck: Deck = {
  'deck-id': 'd1', name: 'Rebels', owner: 'steve', format: 'Standard', side: 'L',
  cards: [{ 'card-id': 'luke', quantity: 2 }],
  validation: { 'valid?': false, warnings: [], violations: [] },
}

const renderWorkspace = (path = '/decks/d1') =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/decks/:deckId/:cardId?" element={<WorkspacePage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )

beforeEach(() => {
  vi.resetAllMocks()
  useDeckDraftStore.setState({ pending: {}, undo: null })
  vi.mocked(client.getDeck).mockResolvedValue(deck)
  vi.mocked(client.fetchAllCards).mockResolvedValue(catalog)
  vi.mocked(client.setDeckCard).mockResolvedValue({ 'card-id': 'x', quantity: 1 })
  vi.mocked(client.removeDeckCard).mockResolvedValue(undefined)
})

describe('WorkspacePage', () => {
  it('pre-filters Browse to the deck side plus Neutral', async () => {
    renderWorkspace()
    expect(await screen.findByRole('link', { name: 'View Luke' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'View Vader' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('Show the other side too'))
    await waitFor(() => expect(screen.getByRole('link', { name: 'View Vader' })).toBeInTheDocument())
  })

  it('adds a copy in place and shows it in Build mode without navigating', async () => {
    renderWorkspace()
    await screen.findByRole('link', { name: 'View Luke' })
    fireEvent.click(screen.getByRole('button', { name: 'Add a copy of Luke' }))
    // optimistic: tray count updates immediately, before the debounced network call fires
    expect(await screen.findByText('3/60')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'build' }))
    expect(await screen.findByText('Character (3)')).toBeInTheDocument()
  })

  it('shows an Undo toast after removing a card, which restores the quantity', async () => {
    renderWorkspace()
    await screen.findByRole('link', { name: 'View Luke' })
    fireEvent.click(screen.getByRole('button', { name: 'Remove a copy of Luke' }))
    // React Query notifies subscribers via a microtask, so the second click needs a tick to
    // see the first optimistic update (a real double-click has enough wall-clock time for this).
    await waitFor(() => expect(screen.getByText('1/60')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: 'Remove a copy of Luke' }))
    expect(await screen.findByText(/Removed Luke/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Undo' }))
    await waitFor(() => expect(screen.getByText('2/60')).toBeInTheDocument())
  })

  it('jumps to Browse pre-filtered by type from a validation violation', async () => {
    renderWorkspace()
    fireEvent.click(await screen.findByRole('button', { name: /issues?/ }))
    fireEvent.click(screen.getByRole('button', { name: 'Show Space' }))
    await waitFor(() => expect(screen.getByRole('button', { name: 'browse' })).toHaveAttribute('aria-pressed', 'true'))
  })
})
