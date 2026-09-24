import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { FlipCard } from './FlipCard'

describe('FlipCard', () => {
  it('shows the face-down side unrotated and the revealed side pre-rotated 180°', () => {
    render(
      <FlipCard backSrc="/cardback.jpg" backAlt="Face-down" frontSrc="/card.jpg" frontAlt="Luke" flipped={false} />,
    )
    expect(screen.getByRole('img', { name: 'Face-down' })).not.toHaveStyle({ transform: 'rotateY(180deg)' })
    expect(screen.getByRole('img', { name: 'Luke' })).toHaveStyle({ transform: 'rotateY(180deg)' })
  })

  it('rotates the whole card 180° when flipped', () => {
    const { container } = render(
      <FlipCard backSrc="/cardback.jpg" backAlt="Face-down" frontSrc="/card.jpg" frontAlt="Luke" flipped={true} />,
    )
    expect(container.querySelector('[style*="preserve-3d"]')).toHaveStyle({ transform: 'rotateY(180deg)' })
  })
})
