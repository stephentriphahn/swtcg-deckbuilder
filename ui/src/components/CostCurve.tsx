export function CostCurve({ data }: { data: { label: string; count: number }[] }) {
  if (data.length === 0) return <p className="text-xs text-slate-500">No cards yet.</p>
  const max = Math.max(...data.map((d) => d.count))
  return (
    <div className="flex items-end gap-1.5" role="img" aria-label="Cost curve">
      {data.map((d) => (
        <div key={d.label} className="flex flex-col items-center gap-1">
          <div
            className="w-3 rounded-t bg-sky-500"
            style={{ height: `${Math.max(4, (d.count / max) * 32)}px` }}
            title={`Cost ${d.label}: ${d.count}`}
          />
          <span className="text-[10px] text-slate-500">{d.label}</span>
        </div>
      ))}
    </div>
  )
}
