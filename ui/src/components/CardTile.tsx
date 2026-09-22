import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import type { Card } from '../api/types'
import { cardImageUrl, isLandscape } from '../lib/cards'

/**
 * Portrait cards take one grid column; landscape cards take two. At equal image height a
 * landscape card is ~2x as wide as a portrait one (437/312 vs 2 x 312/437), so both fit the
 * same rows. `footer`, if given, sits below the image (not over the art) and is the same for
 * every tile in a grid, so it doesn't disturb that row-height match. Place tiles in a
 * `CardGrid`, which packs them densely.
 */
export function CardTile({ card, to, footer }: { card: Card; to?: string; footer?: ReactNode }) {
  const src = cardImageUrl(card)
  const landscape = isLandscape(card)
  const image = src && (
    <img src={src} alt={card.name} loading="lazy" className="h-full w-full object-cover" />
  )
  const imageCls = `overflow-hidden rounded-lg bg-slate-800 ${
    landscape ? 'aspect-[437/312]' : 'aspect-[312/437]'
  }`
  const imageBox = to ? (
    <Link
      to={to}
      aria-label={`View ${card.name}`}
      className={`${imageCls} block focus-visible:outline-2 focus-visible:outline-sky-400`}
    >
      {image}
    </Link>
  ) : (
    <div className={imageCls}>{image}</div>
  )
  return (
    <div className={landscape ? 'col-span-2' : undefined}>
      {imageBox}
      {footer}
    </div>
  )
}
