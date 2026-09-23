import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import type { Card } from '../api/types'
import { cardImageUrl, isLandscape } from '../lib/cards'

/**
 * Every tile is the same portrait-shaped box, even for landscape cards (Battle, Mission,
 * Location, Equipment) — mixing a wider landscape tile in with portrait ones made rows uneven.
 * Landscape art is rotated to sit inside the portrait frame instead: portrait (312:437) and
 * landscape (437:312) are exact reciprocal aspect ratios, so the rotated image fits with no
 * cropping. Rotating alone centers it at ~71% of the tile (it's already the tile's own aspect
 * ratio at that point, just smaller), so `scale(437/312)` blows it back up to fill the tile
 * exactly — still zero cropping, since it was already the right shape. A thumbnail is for
 * browsing, not close reading; `CardDetail` shows the same card at its real, unrotated,
 * landscape-oriented size for that. `footer`, if given, sits below the tile.
 */
export function CardTile({
  card, to, onClick, footer,
}: { card: Card; to?: string; onClick?: () => void; footer?: ReactNode }) {
  const src = cardImageUrl(card)
  const landscape = isLandscape(card)
  const image = src && (
    <img
      src={src}
      alt={card.name}
      loading="lazy"
      className={
        landscape
          ? 'absolute left-1/2 top-1/2 aspect-[437/312] w-full -translate-x-1/2 -translate-y-1/2 -rotate-90 scale-[calc(437/312)] object-cover'
          : 'h-full w-full object-cover'
      }
    />
  )
  const boxCls = 'relative aspect-[312/437] overflow-hidden rounded-lg bg-slate-800'
  const label = `View ${card.name}`
  const imageBox = to ? (
    <Link to={to} aria-label={label} className={`${boxCls} block focus-visible:outline-2 focus-visible:outline-sky-400`}>
      {image}
    </Link>
  ) : onClick ? (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className={`${boxCls} block w-full text-left focus-visible:outline-2 focus-visible:outline-sky-400`}
    >
      {image}
    </button>
  ) : (
    <div className={boxCls}>{image}</div>
  )
  return (
    <div>
      {imageBox}
      {footer}
    </div>
  )
}
