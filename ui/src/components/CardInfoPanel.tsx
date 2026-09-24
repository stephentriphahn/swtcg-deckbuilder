import type { ReactNode } from 'react'
import type { Card } from '../api/types'
import { formatStat, splitText } from '../lib/cards'
import { RARITY_LABELS, SIDE_LABELS, tagsOf } from '../lib/filters'

const STATS = [
  ['Cost', 'cost'], ['Speed', 'speed'], ['Power', 'power'], ['Health', 'health'],
] as const

interface Props {
  card: Card
  /** id for the <h2>, matched by the enclosing dialog's aria-labelledby. */
  titleId?: string
  /** Rendered next to the title — each caller supplies its own close/skip button. */
  headerActions?: ReactNode
}

/**
 * The card's name, stats and rules text — everything CardDetail shows except the image and
 * its own Prev/Next controls. Shared with PackFlipModal's revealed-card view, so a card looks
 * the same whether you got to it by browsing the catalog or by opening a pack.
 */
export function CardInfoPanel({ card, titleId, headerActions }: Props) {
  const stats = STATS.filter(([, k]) => card[k] != null)
  const abilities = splitText(card.text)
  const tags = tagsOf(card)
  const subtitle = [card.type, card.subtype].filter(Boolean).join(' · ')

  return (
    <>
      <div className="flex shrink-0 items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 id={titleId} className="text-2xl font-semibold leading-tight">{card.name}</h2>
          <p className="text-slate-400">{subtitle}</p>
        </div>
        {headerActions}
      </div>

      <div className="mt-3 min-h-0 flex-1 space-y-4 overflow-y-auto pr-1">
        <p className="text-sm text-slate-300">
          {card['set-code']}
          {card.number != null && ` #${card.number}`} · {RARITY_LABELS[card.rarity]} · {SIDE_LABELS[card.side]}
          {tags.length > 0 && <span className="text-slate-500"> · {tags.join(', ')}</span>}
        </p>

        {stats.length > 0 && (
          <dl className="flex flex-wrap gap-2">
            {stats.map(([label, k]) => (
              <div key={k} className="min-w-16 rounded-md bg-slate-800 px-3 py-1.5 text-center">
                <dt className="text-xs uppercase tracking-wide text-slate-400">{label}</dt>
                <dd className="text-lg font-semibold">{formatStat(card[k])}</dd>
              </div>
            ))}
          </dl>
        )}

        {abilities.length > 0 && (
          <ul className="space-y-1.5">
            {abilities.map((a, i) => (
              <li key={i} className="rounded-md bg-slate-800/60 px-3 py-1.5 text-sm">{a}</li>
            ))}
          </ul>
        )}

        {card.usage && (
          <p className="border-l-2 border-slate-700 pl-3 text-sm text-slate-400">{card.usage}</p>
        )}
      </div>
    </>
  )
}
