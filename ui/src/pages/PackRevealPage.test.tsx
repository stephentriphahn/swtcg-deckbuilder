import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from '../api/client'
import type { Card, PackOpening } from '../api/types'
import { PackRevealPage } from './PackRevealPage'

vi.mock('../api/client')

const card = (over: Partial<Card>): Card => ({
  'card-id': 'a', name: 'Luke', type: 'Character', side: 'L', 'set-code': 'ANH', number: 1, rarity: 'C', ...over,
})

const opening: PackOpening = {
  'opening-id': 'op1', owner: 'steve', 'set-code': 'ANH', 'opened-at': 'now',
  cards: [card({ name: 'Luke' }), card({ 'card-id': 'b', name: 'Vader', side: 'D' })],
}

const renderPage = () =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={['/packs/op1']}>
        <Routes>
          <Route path="/packs/:openingId" element={<PackRevealPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(client.getPackOpening).mockResolvedValue(opening)
})

describe('PackRevealPage', () => {
  it('shows the set, owner, and every card drawn', async () => {
    renderPage()
    // PackFlipModal also shows "ANH pack" (as its intro step's h2), covering the page on
    // mount — this is the page's own h1, underneath.
    expect(await screen.findByRole('heading', { name: 'ANH pack', level: 1 })).toBeInTheDocument()
    expect(screen.getByText('Opened by steve')).toBeInTheDocument()
    expect(screen.getByText('Luke')).toBeInTheDocument()
    expect(screen.getByText('Vader')).toBeInTheDocument()
  })

  it('links back to the pack picker', async () => {
    renderPage()
    expect(await screen.findByRole('link', { name: 'Open another pack' })).toHaveAttribute('href', '/packs')
  })
})
