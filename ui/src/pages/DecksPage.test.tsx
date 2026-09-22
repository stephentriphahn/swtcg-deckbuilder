import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from '../api/client'
import type { Deck } from '../api/types'
import { DecksPage } from './DecksPage'

vi.mock('../api/client')

const deck: Deck = {
  'deck-id': 'd1', name: 'Rebels', owner: 'steve', format: 'Standard', side: 'L',
  cards: [{ 'card-id': 'a', quantity: 4 }, { 'card-id': 'b', quantity: 2 }],
  validation: {
    'valid?': false, warnings: [],
    violations: [{ rule: 'deck-size', message: 'Deck must contain exactly 60 cards' }],
  },
}

const renderPage = () =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter>
        <DecksPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(client.listDecks).mockResolvedValue([deck])
  vi.mocked(client.getDeck).mockResolvedValue(deck)
})

describe('DecksPage', () => {
  it('lists decks with card count and validity', async () => {
    renderPage()
    expect(await screen.findByRole('link', { name: 'Rebels' })).toHaveAttribute('href', '/decks/d1')
    expect(await screen.findByText('6/60 cards')).toBeInTheDocument()
    expect(screen.getByText(/Incomplete · 1 issue/)).toBeInTheDocument()
  })

  it('shows an empty state', async () => {
    vi.mocked(client.listDecks).mockResolvedValue([])
    renderPage()
    expect(await screen.findByText('No decks yet.')).toBeInTheDocument()
  })

  it('asks for confirmation before deleting', async () => {
    vi.mocked(client.deleteDeck).mockResolvedValue()
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Delete' }))
    expect(client.deleteDeck).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Delete deck' }))
    await waitFor(() => expect(client.deleteDeck).toHaveBeenCalledWith('d1'))
  })

  it('duplicates a deck as create + bulk add', async () => {
    vi.mocked(client.createDeck).mockResolvedValue({ ...deck, 'deck-id': 'd2', name: 'Copy of Rebels' })
    vi.mocked(client.addDeckCards).mockResolvedValue([])
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Duplicate' }))
    await waitFor(() => expect(client.addDeckCards).toHaveBeenCalled())
    expect(client.createDeck).toHaveBeenCalledWith({
      name: 'Copy of Rebels', owner: 'steve', format: 'Standard', side: 'L',
    })
    expect(client.addDeckCards).toHaveBeenCalledWith('d2', deck.cards)
  })

  it('rolls back the copy if adding cards fails', async () => {
    vi.mocked(client.createDeck).mockResolvedValue({ ...deck, 'deck-id': 'd2', name: 'Copy of Rebels' })
    vi.mocked(client.addDeckCards).mockRejectedValue(new Error('boom'))
    vi.mocked(client.deleteDeck).mockResolvedValue()
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Duplicate' }))
    await waitFor(() => expect(client.deleteDeck).toHaveBeenCalledWith('d2'))
    expect(await screen.findByRole('alert')).toHaveTextContent('boom')
  })

  it('blocks a duplicate deck name in the new deck form', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'New deck' }))
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Rebels' } })
    fireEvent.change(screen.getByLabelText('Owner'), { target: { value: 'steve' } })
    expect(screen.getByText('A deck with this name already exists.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create deck' })).toBeDisabled()
  })
})
