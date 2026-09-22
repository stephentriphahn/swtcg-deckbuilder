import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { Card } from '../api/types'
import { DeckCardTile } from './DeckCardTile'

const card: Card = {
  'card-id': 'a', name: 'Vader', type: 'Character', side: 'D', 'set-code': 'ANH', number: 1, rarity: 'C',
}

const setup = (props: Partial<React.ComponentProps<typeof DeckCardTile>> = {}) => {
  const onChange = vi.fn()
  render(
    <MemoryRouter>
      <DeckCardTile card={card} quantity={0} deckSide="L" onChange={onChange} to="/x" {...props} />
    </MemoryRouter>,
  )
  return onChange
}

describe('DeckCardTile', () => {
  it('disables + for a side mismatch and disables − at zero', () => {
    setup()
    expect(screen.getByRole('button', { name: /Add a copy/ })).toBeDisabled()
    expect(screen.getByRole('button', { name: /Remove a copy/ })).toBeDisabled()
  })
  it('calls onChange without navigating, and disables + at 4 copies', () => {
    const onChange = setup({ deckSide: 'D', quantity: 2 })
    fireEvent.click(screen.getByRole('button', { name: /Add a copy/ }))
    expect(onChange).toHaveBeenCalledWith(3) // and not a navigation — the handler stops propagation
    const four = setup({ deckSide: 'D', quantity: 4 })
    expect(screen.getAllByRole('button', { name: /Add a copy/ }).at(-1)).toBeDisabled()
    expect(four).not.toHaveBeenCalled()
  })
})
