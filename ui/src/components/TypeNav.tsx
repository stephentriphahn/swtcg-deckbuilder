import type { CardType } from '../api/types'
import { TYPES } from '../lib/filters'

interface Props {
  selected: CardType | null
  counts: Map<string, number>
  onSelect: (type: CardType | null) => void
}

/**
 * Single-select tab list, not a multi-select filter: exactly one type (or "All") is active at
 * a time. This groups the catalog by orientation as a side effect — Battle/Mission/Location/
 * Equipment are landscape, everything else portrait (see lib/cards.ts `isLandscape`) — and Type
 * is a natural axis to browse a card catalog by, so it gets its own nav instead of living
 * alongside the other chip filters.
 */
export function TypeNav({ selected, counts, onSelect }: Props) {
  const total = [...counts.values()].reduce((a, b) => a + b, 0)
  const item = (type: CardType | null, label: string, count: number) => (
    <button
      key={type ?? 'all'}
      type="button"
      aria-current={selected === type}
      aria-label={`${label}, ${count} cards`}
      onClick={() => onSelect(type)}
      className={`flex w-full items-center justify-between rounded-md px-3 py-1.5 text-left text-sm ${
        selected === type
          ? 'bg-sky-600 text-white'
          : 'text-slate-400 hover:bg-slate-800 hover:text-white'
      }`}
    >
      <span aria-hidden>{label}</span>
      <span aria-hidden className={selected === type ? 'text-sky-100' : 'text-slate-500'}>{count}</span>
    </button>
  )
  return (
    <nav aria-label="Card type" className="space-y-0.5">
      {item(null, 'All types', total)}
      {TYPES.map((type) => item(type, type, counts.get(type) ?? 0))}
    </nav>
  )
}
