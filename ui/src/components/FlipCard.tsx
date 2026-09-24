interface Props {
  /** Shown face-down: the pack wrapper for the intro step, cardback.jpg for every card after. */
  backSrc: string
  backAlt: string
  frontSrc: string
  frontAlt: string
  flipped: boolean
  /** Extra classes for the outer box — e.g. a glow around the rare's reveal. */
  className?: string
}

/**
 * A single reusable 3D flip element (see PackFlipModal — one instance, re-triggered for each
 * card, not eleven). `backSrc`/`frontSrc` are named for face-down/face-up, not literal CSS
 * front/back faces: the "back" (face-down) side has no rotation, the "front" (revealed) side
 * is pre-rotated 180° so it lands right-side up exactly when the whole card flips.
 */
export function FlipCard({ backSrc, backAlt, frontSrc, frontAlt, flipped, className = '' }: Props) {
  return (
    <div className={`h-52 w-52 md:h-96 md:w-96 ${className}`} style={{ perspective: '1600px' }}>
      <div
        className="relative h-full w-full transition-transform duration-500 ease-in-out"
        style={{ transformStyle: 'preserve-3d', transform: flipped ? 'rotateY(180deg)' : undefined }}
      >
        <img
          src={backSrc}
          alt={backAlt}
          className="absolute inset-0 h-full w-full rounded-lg object-contain"
          style={{ backfaceVisibility: 'hidden' }}
        />
        <img
          src={frontSrc}
          alt={frontAlt}
          className="absolute inset-0 h-full w-full rounded-lg object-contain"
          style={{ backfaceVisibility: 'hidden', transform: 'rotateY(180deg)' }}
        />
      </div>
    </div>
  )
}
