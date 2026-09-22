import type { ReactNode } from 'react'

interface ChipProps {
  selected: boolean
  onClick: () => void
  count?: number
  children: ReactNode
}

/** Toggle chip. A chip that would match nothing is dimmed, not hidden, unless it is selected. */
export function Chip({ selected, onClick, count, children }: ChipProps) {
  const empty = count === 0 && !selected
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onClick}
      className={`rounded-full border px-3 py-1 text-sm transition ${
        selected
          ? 'border-sky-400 bg-sky-500/20 text-sky-200'
          : 'border-slate-700 text-slate-300 hover:border-slate-500'
      } ${empty ? 'opacity-40' : ''}`}
    >
      {children}
      {count != null && <span className="ml-1.5 text-xs text-slate-400">{count}</span>}
    </button>
  )
}
