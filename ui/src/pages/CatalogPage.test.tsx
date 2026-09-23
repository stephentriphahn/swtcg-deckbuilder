import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as client from '../api/client'
import type { Card } from '../api/types'
import { CatalogPage } from './CatalogPage'

vi.mock('../api/client')

let n = 0
const card = (over: Partial<Card>): Card => ({
  'card-id': `c${n++}`, name: `Card ${n}`, type: 'Character', side: 'L', 'set-code': 'ANH', number: n, rarity: 'C', ...over,
})

const luke = card({ name: 'Luke', type: 'Character' })
const xwing = card({ name: 'X-wing', type: 'Space' })
const catalog = [luke, xwing]

const renderCatalog = (path = '/cards') =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/cards/:cardId?" element={<CatalogPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(client.fetchAllCards).mockResolvedValue(catalog)
})

describe('CatalogPage type nav', () => {
  it('starts on All types, showing every card', async () => {
    renderCatalog()
    expect(await screen.findByRole('link', { name: 'View Luke' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View X-wing' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /All types/ })).toHaveAttribute('aria-current', 'true')
  })

  it('selecting a type filters the grid and marks the tab current', async () => {
    renderCatalog()
    await screen.findByRole('link', { name: 'View Luke' })
    fireEvent.click(screen.getByRole('button', { name: /^Space,/ }))
    await waitFor(() => expect(screen.queryByRole('link', { name: 'View Luke' })).not.toBeInTheDocument())
    expect(screen.getByRole('link', { name: 'View X-wing' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Space,/ })).toHaveAttribute('aria-current', 'true')
  })

  it('clicking the active type again returns to All types', async () => {
    renderCatalog('/cards?type=Space')
    await screen.findByRole('link', { name: 'View X-wing' })
    fireEvent.click(screen.getByRole('button', { name: /^Space,/ }))
    await waitFor(() => expect(screen.getByRole('link', { name: 'View Luke' })).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /All types/ })).toHaveAttribute('aria-current', 'true')
  })

  it('has no Type group in the Filters panel — the nav replaces it', async () => {
    renderCatalog()
    await screen.findByRole('link', { name: 'View Luke' })
    fireEvent.click(screen.getByRole('button', { name: 'Filters' }))
    expect(screen.queryByRole('group', { name: 'Type' })).toBeNull()
  })
})
