import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Card } from '../api/types'
import { CardDetail } from './CardDetail'

const card: Card = {
  'card-id': 'a', name: 'Boba Fett (G)', type: 'Character', subtype: 'Bounty Hunter', side: 'N',
  'set-code': 'RAS', number: 2, rarity: 'R', cost: 9, speed: 70, power: 7, health: -1,
  text: 'Upkeep: Pay 1 build point. | Armor | Bounty: 1 build point.',
  usage: 'Only triggers after damage counters are placed.',
  classification: 'WOTC, REB, EP456', 'image-file': 'RAS002_Boba_Fett_G',
}

describe('CardDetail', () => {
  it('shows the card details', () => {
    render(<CardDetail card={card} onClose={() => {}} />)
    expect(screen.getByRole('heading', { name: 'Boba Fett (G)' })).toBeInTheDocument()
    expect(screen.getByText('Character · Bounty Hunter')).toBeInTheDocument()
    expect(screen.getByText(/RAS #2 · Rare · Neutral/)).toBeInTheDocument()
    expect(screen.getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'Upkeep: Pay 1 build point.', 'Armor', 'Bounty: 1 build point.',
    ])
    expect(screen.getByText('*')).toBeInTheDocument() // health -1
    expect(screen.getByRole('img', { name: 'Boba Fett (G)' })).toHaveAttribute(
      'src', '/setimages/RAS/RAS002_Boba_Fett_G.jpg',
    )
  })
  it('closes via the button and disables missing neighbours', () => {
    const onClose = vi.fn()
    const onNext = vi.fn()
    render(<CardDetail card={card} onClose={onClose} onNext={onNext} />)
    fireEvent.click(screen.getByRole('button', { name: 'Close' }))
    expect(onClose).toHaveBeenCalled()
    expect(screen.getByRole('button', { name: /Previous/ })).toBeDisabled()
    fireEvent.keyDown(window, { key: 'ArrowRight' })
    expect(onNext).toHaveBeenCalled()
  })
})
