import type { MouseEvent } from 'react'
import type { Card, Side } from '../api/types'
import { canAddCopy } from '../lib/deckRules'
import { CardTile } from './CardTile'

interface Props {
  card: Card
  to?: string
  quantity: number
  deckSide: Side
  onChange: (quantity: number) => void
}

// The stepper is a sibling below the tile's <Link>, not laid over the art; stopPropagation
// just guards against a click bubbling to an ancestor link if this is ever nested in one.
const stop = (e: MouseEvent) => {
  e.preventDefault()
  e.stopPropagation()
}

/** CardTile plus an always-visible +/- stepper and copy count, for the deck workspace (P3, P5). */
export function DeckCardTile({ card, to, quantity, deckSide, onChange }: Props) {
  const add = canAddCopy(card, quantity, deckSide)
  return (
    <CardTile
      card={card}
      to={to}
      footer={
        <div className="mt-1.5 flex items-center justify-center gap-2 rounded-md bg-slate-800 py-1">
          <button
            type="button"
            aria-label={`Remove a copy of ${card.name}`}
            disabled={quantity === 0}
            onClick={(e) => {
              stop(e)
              onChange(quantity - 1)
            }}
            className="h-6 w-6 rounded bg-slate-800 text-sm leading-none enabled:hover:bg-slate-700 disabled:opacity-30"
          >
            −
          </button>
          <span className={`w-5 text-center text-sm font-medium ${quantity > 0 ? 'text-white' : 'text-slate-500'}`}>
            {quantity}
          </span>
          <button
            type="button"
            aria-label={`Add a copy of ${card.name}`}
            title={add.ok ? undefined : add.reason}
            disabled={!add.ok}
            onClick={(e) => {
              stop(e)
              onChange(quantity + 1)
            }}
            // Gold, not the same slate as −, so the affordance to add is easy to spot at a
            // glance; it fades to plain slate when disabled so "can't add" still reads clearly.
            className="h-6 w-6 rounded text-sm font-bold leading-none text-slate-900 enabled:bg-amber-400 enabled:hover:bg-amber-300 disabled:bg-slate-800 disabled:text-slate-500 disabled:opacity-50"
          >
            +
          </button>
        </div>
      }
    />
  )
}
