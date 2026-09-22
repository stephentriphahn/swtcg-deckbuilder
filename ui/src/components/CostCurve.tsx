// costCurve() (lib/deckRules.ts) already omits any cost with zero cards, so every bar here is
// real. But a 1-2 copy bar and a 3-4 copy bar can render at the same floor height (below, a
// bar's height is floored at 2px so it stays visible/hoverable) — the count label above each
// bar is what actually disambiguates "there's 1 copy" from "there's 0", not the bar's height.
// costCurve() labels a variable cost "*" and no cost at all "–" (same symbols as formatStat)
// — spell both out in the tooltip since the symbols alone are easy to misread as the same thing.
const costLabel = (label: string) =>
  label === '*' ? 'Variable cost' : label === '–' ? 'No cost' : `Cost ${label}`

export function CostCurve({ data }: { data: { label: string; count: number }[] }) {
  if (data.length === 0) return <p className="text-xs text-slate-500">No cards yet.</p>
  const max = Math.max(...data.map((d) => d.count))
  return (
    <div className="flex items-end gap-1.5" role="img" aria-label="Cost curve">
      {data.map((d) => (
        <div key={d.label} className="flex flex-col items-center gap-0.5">
          <span className="text-[10px] text-slate-400">{d.count}</span>
          <div
            className="w-3 rounded-t bg-sky-500"
            style={{ height: `${Math.max(2, (d.count / max) * 32)}px` }}
            title={`${costLabel(d.label)}: ${d.count}`}
          />
          <span className="text-[10px] text-slate-500">{d.label}</span>
        </div>
      ))}
    </div>
  )
}
