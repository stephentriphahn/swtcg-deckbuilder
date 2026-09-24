import { useEffect, useRef, useState } from 'react'
import type { PackOpening } from '../api/types'
import { cardImageUrl } from '../lib/cards'
import { CardInfoPanel } from './CardInfoPanel'
import { FlipCard } from './FlipCard'

interface Props {
  opening: PackOpening
  /** Called once, whether the player clicks through all 11 or skips early. */
  onClose: () => void
}

const TITLE_ID = 'pack-flip-title'
const CARD_BACK = '/cardback.jpg'

/**
 * Simulates thumbing through a freshly opened pack: the wrapper art, then a single flip that
 * turns the whole (imagined) face-down stack over to reveal the first card, in reveal order
 * (commons, uncommons, the rare last — see pack-service/open-pack). Every card after that is
 * already "face up" — you flipped the stack once — so advancing just slides the next one into
 * view instead of flipping again; only the first card uses FlipCard. Once flipped, a card is
 * shown with the same CardInfoPanel as the catalog's CardDetail — "closer inspection" is a
 * later pass, this is the first one. Closing (finishing or skipping) reveals the plain grid
 * underneath (PackRevealPage), which already has all 11 cards from the same API response.
 */
export function PackFlipModal({ opening, onClose }: Props) {
  const ref = useRef<HTMLDialogElement>(null)
  const [index, setIndex] = useState<number | 'intro'>('intro')
  const [hasFlipped, setHasFlipped] = useState(false)

  useEffect(() => {
    const dialog = ref.current
    dialog?.showModal()
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = ''
      dialog?.close()
    }
  }, [])

  const isLast = index !== 'intro' && index === opening.cards.length - 1
  // Revealed the instant we're past the first card, or once the first one has been flipped.
  const revealed = index !== 'intro' && (index > 0 || hasFlipped)

  const advance = () => {
    if (index === 'intro') {
      setIndex(0)
    } else if (index === 0 && !hasFlipped) {
      setHasFlipped(true)
    } else if (isLast) {
      onClose()
    } else {
      setIndex(index + 1)
    }
  }

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowRight') {
        e.preventDefault()
        advance()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [index, hasFlipped])

  const card = index === 'intro' ? undefined : opening.cards[index]
  const packImage = `/packs/${opening['set-code']}.jpg`
  const isRareReveal = revealed && card?.rarity === 'R'
  const glowCls = isRareReveal ? 'ring-4 ring-amber-400 shadow-[0_0_50px_rgba(251,191,36,0.6)]' : ''

  const buttonLabel =
    index === 'intro'
      ? 'Open pack'
      : index === 0 && !hasFlipped
        ? 'Reveal'
        : isLast
          ? 'Done — view all 11'
          : 'Next card →'

  return (
    <dialog
      ref={ref}
      aria-labelledby={TITLE_ID}
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      onClick={(e) => e.target === ref.current && onClose()}
      className="m-auto h-[min(34rem,92vh)] w-[min(56rem,94vw)] overflow-hidden rounded-xl border border-slate-700 bg-slate-900 p-0 text-slate-100 backdrop:bg-black/80"
    >
      <div className="relative flex h-full flex-col gap-4 p-5 md:flex-row md:gap-6">
        <button
          type="button"
          onClick={onClose}
          aria-label="Skip to the full reveal"
          className="absolute right-4 top-4 z-10 rounded-md px-2 py-1 text-xl leading-none text-slate-400 hover:bg-slate-800 hover:text-white"
        >
          ×
        </button>

        {/* Clicking the art is a mouse/touch shortcut for the same action as the button below
            it — kept out of the accessibility tree (aria-hidden, not a <button>) so there's
            exactly one named control per step, not two with an identical label. */}
        {index === 'intro' || index === 0 ? (
          <div aria-hidden onClick={advance} className="cursor-pointer self-center">
            <FlipCard
              backSrc={index === 'intro' ? packImage : CARD_BACK}
              backAlt={index === 'intro' ? `${opening['set-code']} pack` : 'Face-down card'}
              frontSrc={card ? (cardImageUrl(card) ?? CARD_BACK) : CARD_BACK}
              frontAlt={card?.name ?? ''}
              flipped={hasFlipped}
              className={glowCls}
            />
          </div>
        ) : (
          <div
            aria-hidden
            onClick={advance}
            key={card?.['card-id']}
            className={`flex h-52 w-52 shrink-0 cursor-pointer items-center justify-center self-center rounded-lg bg-slate-800 [animation:pack-card-in_0.3s_ease-out] md:h-96 md:w-96 ${glowCls}`}
          >
            {card && (
              <img src={cardImageUrl(card) ?? undefined} alt={card.name} className="h-full w-full rounded-lg object-contain" />
            )}
          </div>
        )}

        <div className="flex min-h-0 min-w-0 flex-1 flex-col justify-center">
          {revealed && card ? (
            <CardInfoPanel card={card} titleId={TITLE_ID} />
          ) : (
            <div className="text-center md:text-left">
              <h2 id={TITLE_ID} className="text-2xl font-semibold">
                {index === 'intro' ? `${opening['set-code']} pack` : `Card ${(index as number) + 1} of ${opening.cards.length}`}
              </h2>
              <p className="mt-1 text-slate-400">
                {index === 'intro' ? 'Click to open it.' : 'Click the card to flip the pack over.'}
              </p>
            </div>
          )}

          <div className="mt-4 flex justify-center md:justify-start">
            <button
              type="button"
              onClick={advance}
              className="rounded-md bg-sky-600 px-4 py-2 text-sm font-medium hover:bg-sky-500"
            >
              {buttonLabel}
            </button>
          </div>
        </div>
      </div>
    </dialog>
  )
}
