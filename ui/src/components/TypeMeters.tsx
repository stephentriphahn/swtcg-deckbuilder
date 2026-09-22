import { MIN_RULES } from '../lib/deckRules'

export function TypeMeters({ counts }: { counts: Record<string, number> }) {
  return (
    <div className="space-y-1.5">
      {MIN_RULES.map(({ type, min }) => {
        const count = counts[type] ?? 0
        const met = count >= min
        return (
          <div key={type} className="flex items-center gap-2 text-xs">
            <span className="w-16 shrink-0 text-slate-400">{type}</span>
            <div className="h-1.5 flex-1 rounded-full bg-slate-800">
              <div
                className={`h-full rounded-full ${met ? 'bg-emerald-500' : 'bg-amber-500'}`}
                style={{ width: `${Math.min(100, (count / min) * 100)}%` }}
              />
            </div>
            <span className={`w-10 text-right ${met ? 'text-emerald-400' : 'text-amber-400'}`}>
              {count}/{min}
            </span>
          </div>
        )
      })}
    </div>
  )
}
