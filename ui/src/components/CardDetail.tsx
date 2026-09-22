import { useEffect, useRef } from 'react'
import type { Card } from '../api/types'
import { cardImageUrl, formatStat, isLandscape, splitText } from '../lib/cards'
import { RARITY_LABELS, SIDE_LABELS, tagsOf } from '../lib/filters'

interface Props {
  card: Card
  onClose: () => void
  /** Neighbours in the current filtered list; undefined when there is none. */
  onPrev?: () => void
  onNext?: () => void
}

const STATS = [
  ['Cost', 'cost'], ['Speed', 'speed'], ['Power', 'power'], ['Health', 'health'],
] as const

/** Modal card view built on a native <dialog>: focus trap, Escape and inert background for free. */
export function CardDetail({ card, onClose, onPrev, onNext }: Props) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    dialog?.showModal()
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = ''
      dialog?.close() // returns focus to the tile that opened it
    }
  }, [])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement) return
      if (e.key === 'ArrowLeft') onPrev?.()
      if (e.key === 'ArrowRight') onNext?.()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onPrev, onNext])

  const src = cardImageUrl(card)
  const landscape = isLandscape(card)
  const stats = STATS.filter(([, k]) => card[k] != null)
  const abilities = splitText(card.text)
  const tags = tagsOf(card)
  const subtitle = [card.type, card.subtype].filter(Boolean).join(' · ')

  const imgShape = landscape ? 'w-full aspect-[437/312]' : 'h-full aspect-[312/437]'

  // Fixed dialog size: the image sits in a fixed box (portrait or landscape centred in it), the
  // details scroll in the middle, and Previous/Next are pinned bottom-right.
  return (
    <dialog
      ref={ref}
      aria-labelledby="card-detail-title"
      onCancel={(e) => {
        e.preventDefault() // the URL is the source of truth; closing means navigating
        onClose()
      }}
      onClick={(e) => e.target === ref.current && onClose()}
      className="m-auto h-[min(34rem,92vh)] w-[min(56rem,94vw)] overflow-hidden rounded-xl border border-slate-700 bg-slate-900 p-0 text-slate-100 backdrop:bg-black/70"
    >
      <div className="flex h-full flex-col gap-4 p-5 md:flex-row md:gap-6">
        <div className="flex h-52 shrink-0 items-center justify-center md:h-full md:w-[24rem]">
          {src ? (
            <img src={src} alt={card.name} className={`rounded-lg ${imgShape}`} />
          ) : (
            <div className={`rounded-lg bg-slate-800 ${imgShape}`} />
          )}
        </div>

        <div className="flex min-h-0 min-w-0 flex-1 flex-col">
          <div className="flex shrink-0 items-start justify-between gap-3">
            <div className="min-w-0">
              <h2 id="card-detail-title" className="text-2xl font-semibold leading-tight">{card.name}</h2>
              <p className="text-slate-400">{subtitle}</p>
            </div>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close"
              className="rounded-md px-2 py-1 text-xl leading-none text-slate-400 hover:bg-slate-800 hover:text-white"
            >
              ×
            </button>
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

          <div className="mt-3 flex shrink-0 justify-end gap-2">
            <button
              type="button"
              onClick={onPrev}
              disabled={!onPrev}
              className="rounded-md border border-slate-700 px-3 py-1.5 text-sm enabled:hover:border-slate-500 disabled:opacity-40"
            >
              ← Previous
            </button>
            <button
              type="button"
              onClick={onNext}
              disabled={!onNext}
              className="rounded-md border border-slate-700 px-3 py-1.5 text-sm enabled:hover:border-slate-500 disabled:opacity-40"
            >
              Next →
            </button>
          </div>
        </div>
      </div>
    </dialog>
  )
}
