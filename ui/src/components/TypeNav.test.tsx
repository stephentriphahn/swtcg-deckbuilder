import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { TypeNav } from './TypeNav'

const counts = new Map([['Character', 5], ['Space', 3]])

describe('TypeNav', () => {
  it('lists All types (summed) plus every type, marking the selected one current', () => {
    render(<TypeNav selected="Space" counts={counts} onSelect={() => {}} />)
    expect(screen.getByRole('button', { name: 'All types, 8 cards' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Character, 5 cards' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Space, 3 cards' })).toHaveAttribute('aria-current', 'true')
    expect(screen.getByRole('button', { name: 'Character, 5 cards' })).toHaveAttribute('aria-current', 'false')
  })
  it('selects a type, and All types with null', () => {
    const onSelect = vi.fn()
    render(<TypeNav selected={null} counts={counts} onSelect={onSelect} />)
    fireEvent.click(screen.getByRole('button', { name: 'Character, 5 cards' }))
    expect(onSelect).toHaveBeenCalledWith('Character')
    fireEvent.click(screen.getByRole('button', { name: /All types/ }))
    expect(onSelect).toHaveBeenCalledWith(null)
  })
})
