import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import type { Card } from '../api/types'
import { cardImageUrl, isLandscape } from '../lib/cards'

/**
 * Portrait cards take one grid column and scale with it, since the columns are already sized
 * for them. Landscape cards span two columns to fit their wider aspect ratio, but with the type
 * nav it's easy to end up viewing landscape types (Battle/Mission/Location/Equipment) alone, at
 * which point two wide columns is most of the row — so landscape is capped at a fixed max width
 * (`justify-self-start` keeps it from stretching to fill the rest of its two-column cell) rather
 * than growing however wide the grid's columns happen to be. `footer`, if given, sits below the
 * image (not over the art). Place tiles in a `CardGrid`, which packs them densely.
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
    <div className={landscape ? 'col-span-2 max-w-96 justify-self-start' : undefined}>
      {imageBox}
      {footer}
    </div>
  )
}
