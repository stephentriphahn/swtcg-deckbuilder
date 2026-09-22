import type { Violation } from '../api/types'
import type { CardType } from '../api/types'

const TYPE_BY_RULE: Partial<Record<string, CardType>> = {
  'min-characters': 'Character', 'min-space': 'Space', 'min-ground': 'Ground',
}

interface Props {
  violations: Violation[]
  /** Jumps Browse mode's filter to the type a min-count rule is short on. */
  onFilterType?: (type: CardType) => void
}

export function ValidationPanel({ violations, onFilterType }: Props) {
  if (violations.length === 0) return <p className="text-sm text-emerald-400">Legal deck — ready to play.</p>
  return (
    <ul className="space-y-1.5 text-sm">
      {violations.map((v, i) => {
        const type = TYPE_BY_RULE[v.rule]
        return (
          <li key={i} className="flex items-start justify-between gap-2 text-amber-300">
            <span>{v.message}</span>
            {type && onFilterType && (
              <button
                type="button"
                onClick={() => onFilterType(type)}
                className="shrink-0 text-xs text-sky-400 hover:underline"
              >
                Show {type}
              </button>
            )}
          </li>
        )
      })}
    </ul>
  )
}
