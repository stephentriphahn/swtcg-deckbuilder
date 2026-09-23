import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from '../api/client'
import type { PackOpening, PackSummary } from '../api/types'
import { PacksPage } from './PacksPage'

vi.mock('../api/client')

const packs: PackSummary[] = [
  { 'set-code': 'ANH', name: 'A New Hope', image: '/packs/ANH.jpg' },
  { 'set-code': 'ESB', name: 'Empire Strikes Back', image: '/packs/ESB.jpg' },
]

const renderPage = () =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={['/packs']}>
        <Routes>
          <Route path="/packs" element={<PacksPage />} />
          <Route path="/packs/:openingId" element={<p>reveal page</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )

beforeEach(() => {
  vi.resetAllMocks()
  localStorage.clear()
  vi.mocked(client.listPacks).mockResolvedValue(packs)
})

describe('PacksPage', () => {
  it('lists every pack and requires a name before opening one', async () => {
    renderPage()
    expect(await screen.findByRole('button', { name: /A New Hope/ })).toBeDisabled()
    expect(screen.getByText('Enter a name above before opening a pack.')).toBeInTheDocument()
  })

  it('opens a pack and navigates to its reveal, remembering the name', async () => {
    const opening: PackOpening = {
      'opening-id': 'op1', owner: 'steve', 'set-code': 'ANH', 'opened-at': 'now', cards: [],
    }
    vi.mocked(client.openPack).mockResolvedValue(opening)
    renderPage()
    fireEvent.change(await screen.findByPlaceholderText('your name'), { target: { value: 'steve' } })
    fireEvent.click(screen.getByRole('button', { name: /A New Hope/ }))
    expect(await screen.findByText('reveal page')).toBeInTheDocument()
    expect(client.openPack).toHaveBeenCalledWith('steve', 'ANH')
    expect(localStorage.getItem('swtcg.owner')).toBe('steve')
  })

  it('shows an error if opening fails', async () => {
    vi.mocked(client.openPack).mockRejectedValue(new Error('no cards of this rarity'))
    renderPage()
    fireEvent.change(await screen.findByPlaceholderText('your name'), { target: { value: 'steve' } })
    fireEvent.click(screen.getByRole('button', { name: /A New Hope/ }))
    expect(await screen.findByRole('alert')).toHaveTextContent('no cards of this rarity')
  })
})
