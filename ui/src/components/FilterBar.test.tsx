import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { EMPTY_FILTERS, facetCounts } from '../lib/filters'
import { FilterBar } from './FilterBar'

const setup = (filters = EMPTY_FILTERS) => {
  const onChange = vi.fn()
  render(
    <FilterBar
      filters={filters}
      onChange={onChange}
      counts={facetCounts([], filters)}
      resultCount={3}
      totalCount={10}
    />,
  )
  return onChange
}

describe('FilterBar', () => {
  it('toggles a type chip', () => {
    const onChange = setup()
    fireEvent.click(screen.getByRole('button', { name: 'Filters' }))
    fireEvent.click(screen.getByRole('button', { name: /^Space/ }))
    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_FILTERS, types: ['Space'] })
  })
  it('keeps Side visible without opening the panel', () => {
    const onChange = setup()
    expect(screen.queryByRole('button', { name: /^Rare/ })).toBeNull() // Rarity stays in the panel
    fireEvent.click(screen.getByRole('button', { name: /^Light/ }))
    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_FILTERS, sides: ['L'] })
  })
  it('shows the live result count and removable active filters', () => {
    const onChange = setup({ ...EMPTY_FILTERS, sides: ['D'] })
    expect(screen.getByText('3 of 10 cards')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Remove filter Dark' }))
    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_FILTERS, sides: [] })
  })
  it('keeps search visible and hides filters until expanded', () => {
    setup()
    expect(screen.getByLabelText('Search cards')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Space/ })).toBeNull()
    expect(screen.queryByLabelText('cost min')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Filters' }))
    expect(screen.getByRole('button', { name: /^Space/ })).toBeInTheDocument()
    expect(screen.getByLabelText('cost min')).toBeInTheDocument()
  })
  it('closes the filter panel with Escape', () => {
    setup()
    fireEvent.click(screen.getByRole('button', { name: 'Filters' }))
    fireEvent.keyDown(screen.getByLabelText('cost min'), { key: 'Escape' })
    expect(screen.queryByLabelText('cost min')).toBeNull()
  })
})
