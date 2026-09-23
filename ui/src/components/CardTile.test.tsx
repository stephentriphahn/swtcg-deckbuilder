import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { Card } from '../api/types'
import { CardTile } from './CardTile'

const portrait: Card = {
  'card-id': 'a', name: 'Luke', type: 'Character', side: 'L', 'set-code': 'ANH', number: 1, rarity: 'C',
  'image-file': 'luke',
}
const landscape: Card = { ...portrait, 'card-id': 'b', name: 'Battle Fatigue', type: 'Battle' }

describe('CardTile', () => {
  it('renders every tile as the same portrait-shaped box', () => {
    const { container: c1 } = render(<CardTile card={portrait} />)
    const { container: c2 } = render(<CardTile card={landscape} />)
    expect(c1.querySelector('.aspect-\\[312\\/437\\]')).not.toBeNull()
    expect(c2.querySelector('.aspect-\\[312\\/437\\]')).not.toBeNull()
    expect(c1.querySelector('.col-span-2')).toBeNull()
    expect(c2.querySelector('.col-span-2')).toBeNull()
  })

  it('rotates and scales landscape art to fill the portrait frame, leaves portrait art alone', () => {
    render(<CardTile card={portrait} />)
    expect(screen.getByRole('img')).not.toHaveClass('-rotate-90')
    render(<CardTile card={landscape} />)
    const img = screen.getByRole('img', { name: 'Battle Fatigue' })
    expect(img).toHaveClass('-rotate-90')
    expect(img.className).toMatch(/scale-\[calc\(437\/312\)\]/)
  })

  it('renders a Link when `to` is given', () => {
    render(
      <MemoryRouter>
        <CardTile card={portrait} to="/cards/a" />
      </MemoryRouter>,
    )
    expect(screen.getByRole('link', { name: 'View Luke' })).toHaveAttribute('href', '/cards/a')
  })

  it('renders a button and calls onClick when there is no `to` (e.g. the pack reveal)', () => {
    const onClick = vi.fn()
    render(<CardTile card={portrait} onClick={onClick} />)
    fireEvent.click(screen.getByRole('button', { name: 'View Luke' }))
    expect(onClick).toHaveBeenCalled()
  })

  it('falls back to a plain, non-interactive box when neither is given', () => {
    render(<CardTile card={portrait} />)
    expect(screen.queryByRole('button')).toBeNull()
    expect(screen.queryByRole('link')).toBeNull()
  })
})
