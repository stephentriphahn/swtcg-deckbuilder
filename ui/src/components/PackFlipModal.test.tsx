import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Card, PackOpening } from '../api/types'
import { PackFlipModal } from './PackFlipModal'

const card = (over: Partial<Card>): Card => ({
  'card-id': 'a', name: 'Luke', type: 'Character', side: 'L', 'set-code': 'ANH', number: 1, rarity: 'C', ...over,
})

const opening: PackOpening = {
  'opening-id': 'op1', owner: 'steve', 'set-code': 'ANH', 'opened-at': 'now',
  cards: [
    card({ 'card-id': 'c1', name: 'Common One' }),
    card({ 'card-id': 'c2', name: 'Common Two' }),
    card({ 'card-id': 'r1', name: 'The Rare', rarity: 'R' }),
  ],
}

describe('PackFlipModal', () => {
  it('flips only the first card; every card after that is already face up', () => {
    const onClose = vi.fn()
    render(<PackFlipModal opening={opening} onClose={onClose} />)

    // intro
    expect(screen.getByRole('heading', { name: 'ANH pack' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Open pack' }))

    // card 1, face-down — the only card that needs a click-to-reveal step
    expect(screen.getByRole('heading', { name: 'Card 1 of 3' })).toBeInTheDocument()
    expect(screen.queryByText('Common One')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Reveal' }))
    expect(screen.getByRole('heading', { name: 'Common One' })).toBeInTheDocument()

    // card 2: no face-down interstitial, no "Reveal" — already showing once you advance
    fireEvent.click(screen.getByRole('button', { name: 'Next card →' }))
    expect(screen.getByRole('heading', { name: 'Common Two' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reveal' })).toBeNull()

    expect(onClose).not.toHaveBeenCalled()
  })

  it('finishes after the last card (the rare) and calls onClose', () => {
    const onClose = vi.fn()
    render(<PackFlipModal opening={opening} onClose={onClose} />)
    fireEvent.click(screen.getByRole('button', { name: 'Open pack' }))
    fireEvent.click(screen.getByRole('button', { name: 'Reveal' })) // card 1
    fireEvent.click(screen.getByRole('button', { name: 'Next card →' })) // -> card 2
    fireEvent.click(screen.getByRole('button', { name: 'Next card →' })) // -> card 3, the rare
    expect(screen.getByRole('heading', { name: 'The Rare' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Done — view all 11' }))
    expect(onClose).toHaveBeenCalled()
  })

  it('can be skipped at any point', () => {
    const onClose = vi.fn()
    render(<PackFlipModal opening={opening} onClose={onClose} />)
    fireEvent.click(screen.getByRole('button', { name: 'Skip to the full reveal' }))
    expect(onClose).toHaveBeenCalled()
  })

  it('advances on Enter and ArrowRight, not on unrelated keys', () => {
    const onClose = vi.fn()
    render(<PackFlipModal opening={opening} onClose={onClose} />)
    fireEvent.keyDown(window, { key: 'a' })
    expect(screen.getByRole('heading', { name: 'ANH pack' })).toBeInTheDocument() // unmoved
    fireEvent.keyDown(window, { key: 'Enter' })
    expect(screen.getByRole('heading', { name: 'Card 1 of 3' })).toBeInTheDocument()
    fireEvent.keyDown(window, { key: 'ArrowRight' })
    expect(screen.getByRole('heading', { name: 'Common One' })).toBeInTheDocument()
    fireEvent.keyDown(window, { key: 'ArrowRight' })
    expect(screen.getByRole('heading', { name: 'Common Two' })).toBeInTheDocument() // straight through
  })
})
