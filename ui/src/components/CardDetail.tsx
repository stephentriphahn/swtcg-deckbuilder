import { useEffect, useRef } from 'react'
import type { Card } from '../api/types'
import { cardImageUrl } from '../lib/cards'
import { CardInfoPanel } from './CardInfoPanel'

interface Props {
  card: Card
  onClose: () => void
  /** Neighbours in the current filtered list; undefined when there is none. */
  onPrev?: () => void
  onNext?: () => void
}

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

  // Fixed dialog size: the image sits in a fixed box, the details scroll in the middle, and
  // Previous/Next are pinned bottom-right.
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
        {/* A fixed square, not a box shaped to either orientation: object-contain then sizes a
            portrait card by height and a landscape card by width, and since those aspect
            ratios are exact reciprocals (312:437 / 437:312), the two end up as literally the
            same rectangle, just transposed — same apparent size either way. */}
        <div className="flex h-52 w-52 shrink-0 items-center justify-center self-center rounded-lg bg-slate-800 md:h-96 md:w-96">
          {src && <img src={src} alt={card.name} className="h-full w-full object-contain" />}
        </div>

        <div className="flex min-h-0 min-w-0 flex-1 flex-col">
          <CardInfoPanel
            card={card}
            titleId="card-detail-title"
            headerActions={
              <button
                type="button"
                onClick={onClose}
                aria-label="Close"
                className="rounded-md px-2 py-1 text-xl leading-none text-slate-400 hover:bg-slate-800 hover:text-white"
              >
                ×
              </button>
            }
          />

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
