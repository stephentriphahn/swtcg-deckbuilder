import type { PackSummary } from '../api/types'

interface Props {
  pack: PackSummary
  onOpen: () => void
  disabled?: boolean
}

// Wrapper art is tall (~436x800), unlike a card (~312x437) — its own aspect ratio, not CardTile's.
export function PackTile({ pack, onOpen, disabled }: Props) {
  return (
    <button
      type="button"
      onClick={onOpen}
      disabled={disabled}
      className="group flex flex-col items-center gap-2 disabled:opacity-50"
    >
      <span className="aspect-[436/800] w-full overflow-hidden rounded-lg bg-slate-800 shadow-lg transition group-enabled:group-hover:-translate-y-1 group-enabled:group-hover:shadow-xl">
        <img src={pack.image} alt="" loading="lazy" className="h-full w-full object-cover" />
      </span>
      <span className="text-sm text-slate-300 group-enabled:group-hover:text-white">{pack.name}</span>
    </button>
  )
}
