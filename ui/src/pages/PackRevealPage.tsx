import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { usePackOpening } from '../api/queries'
import { CardDetail } from '../components/CardDetail'
import { CardGrid } from '../components/CardGrid'
import { CardTile } from '../components/CardTile'
import { PackFlipModal } from '../components/PackFlipModal'

// The grid (all 11 cards, already face up) sits underneath from the start — PackFlipModal
// covers it until it's dismissed (finished or skipped), same as CardDetail overlays the
// catalog grid. A refresh replays the flip-through rather than resuming mid-reveal (see
// doc/pack-opening-design.md §7): nothing about the collection itself depends on it.
export function PackRevealPage() {
  const { openingId = '' } = useParams()
  const { data: opening, isPending, error } = usePackOpening(openingId)
  const [openIndex, setOpenIndex] = useState<number | null>(null)
  const [flipDone, setFlipDone] = useState(false)

  if (isPending) return <p className="p-6">Loading pack…</p>
  if (error) return <p className="p-6 text-red-400">Failed to load pack: {error.message}</p>

  const step = (offset: number) => {
    if (openIndex == null) return undefined
    const next = openIndex + offset
    return next >= 0 && next < opening.cards.length ? () => setOpenIndex(next) : undefined
  }

  return (
    <div className="@container mx-auto max-w-5xl p-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold">{opening['set-code']} pack</h1>
          <p className="text-sm text-slate-400">Opened by {opening.owner}</p>
        </div>
        <Link to="/packs" className="rounded-md bg-sky-600 px-4 py-2 text-sm font-medium hover:bg-sky-500">
          Open another pack
        </Link>
      </div>
      <CardGrid>
        {opening.cards.map((card, i) => (
          <CardTile
            key={`${card['card-id']}-${i}`}
            card={card}
            onClick={() => setOpenIndex(i)}
            footer={<p className="mt-1.5 truncate text-center text-xs text-slate-400">{card.name}</p>}
          />
        ))}
      </CardGrid>
      {openIndex != null && (
        <CardDetail
          card={opening.cards[openIndex]}
          onClose={() => setOpenIndex(null)}
          onPrev={step(-1)}
          onNext={step(1)}
        />
      )}
      {!flipDone && <PackFlipModal opening={opening} onClose={() => setFlipDone(true)} />}
    </div>
  )
}
