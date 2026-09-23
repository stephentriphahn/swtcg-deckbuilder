import { useState } from 'react'
import {
  KEYWORDS, RARITIES, RARITY_LABELS, SETS, SIDES, SIDE_LABELS, SORTS, STAT_KEYS, TAGS, TYPES,
  EMPTY_FILTERS, activeFilters, type FacetKey, type Filters, type Range, type SortKey, type StatKey,
} from '../lib/filters'
import { Chip } from './Chip'

interface Props {
  filters: Filters
  onChange: (next: Filters) => void
  counts: Record<FacetKey, Map<string, number>>
  resultCount: number
  totalCount: number
  /** Hide the Type group — set this where a dedicated type nav (e.g. TypeNav) replaces it. */
  showTypeFilter?: boolean
}

const toggle = <T,>(xs: T[], x: T) => (xs.includes(x) ? xs.filter((y) => y !== x) : [...xs, x])

export function FilterBar({
  filters, onChange, counts, resultCount, totalCount, showTypeFilter = true,
}: Props) {
  const [open, setOpen] = useState(false)
  const active = activeFilters(filters)
  const set = (patch: Partial<Filters>) => onChange({ ...filters, ...patch })

  const group = <T extends string>(
    label: string,
    facet: FacetKey,
    values: readonly T[],
    selected: T[],
    key: keyof Filters,
    text: (v: T) => string = (v) => v,
  ) => (
    <div className="flex flex-wrap items-center gap-1.5" role="group" aria-label={label}>
      <span className="w-16 text-xs uppercase tracking-wide text-slate-500">{label}</span>
      {values.map((v) => (
        <Chip
          key={v}
          selected={selected.includes(v)}
          count={counts[facet].get(v) ?? 0}
          onClick={() => set({ [key]: toggle(selected, v) })}
        >
          {text(v)}
        </Chip>
      ))}
    </div>
  )

  return (
    <div
      // The open panel is an overlay under the bar, so opening it never pushes the grid down
      // or makes the sticky bar taller than the viewport.
      onKeyDown={(e) => e.key === 'Escape' && open && setOpen(false)}
      className="sticky top-0 z-10 space-y-2 border-b border-slate-800 bg-slate-950/95 px-6 py-3 backdrop-blur"
    >
      <div className="flex flex-wrap items-center gap-3">
        <input
          type="search"
          value={filters.q}
          onChange={(e) => set({ q: e.target.value })}
          placeholder="Search name or card text…"
          aria-label="Search cards"
          className="w-56 rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm placeholder:text-slate-500 focus:border-sky-400 focus:outline-none"
        />
        {/* Side is small (3 chips) and common enough to stay visible; Type (7), Set (10) and
            Rarity (4) would crowd the bar, so they stay in the Filters panel below. */}
        {group('Side', 'sides', SIDES, filters.sides, 'sides', (v) => SIDE_LABELS[v])}
        <div className="ml-auto flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-slate-400">
            Sort
            <select
              value={filters.sort}
              onChange={(e) => set({ sort: e.target.value as SortKey })}
              className="rounded-md border border-slate-700 bg-slate-900 px-2 py-2 text-slate-100"
            >
              {SORTS.map((s) => (
                <option key={s.key} value={s.key}>{s.label}</option>
              ))}
            </select>
          </label>
          <button
            type="button"
            aria-expanded={open}
            aria-controls="filter-panel"
            onClick={() => setOpen(!open)}
            className="flex items-center gap-1.5 rounded-md border border-slate-700 px-3 py-2 text-sm hover:border-slate-500"
          >
            Filters
            {active.length > 0 && (
              <span className="rounded-full bg-sky-500/30 px-1.5 text-xs text-sky-200">{active.length}</span>
            )}
            <span aria-hidden className={`text-xs transition-transform ${open ? 'rotate-180' : ''}`}>▾</span>
          </button>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span aria-live="polite" className="text-slate-300">
          {resultCount === totalCount ? `${totalCount} cards` : `${resultCount} of ${totalCount} cards`}
        </span>
        {active.map((a) => (
          <button
            key={a.id}
            type="button"
            onClick={() => onChange(a.remove(filters))}
            aria-label={`Remove filter ${a.label}`}
            className="rounded-full bg-slate-800 px-2.5 py-0.5 text-xs text-slate-200 hover:bg-slate-700"
          >
            {a.label} ×
          </button>
        ))}
        {active.length > 0 && (
          <button
            type="button"
            onClick={() => onChange({ ...EMPTY_FILTERS, sort: filters.sort })}
            className="text-xs text-sky-400 hover:underline"
          >
            Clear all
          </button>
        )}
      </div>
      {open && (
        <div
          id="filter-panel"
          className="absolute inset-x-0 top-full max-h-[60vh] space-y-3 overflow-y-auto border-b border-slate-700 bg-slate-900 px-6 py-4 shadow-2xl"
        >
          <div className="space-y-1.5">
            {showTypeFilter && group('Type', 'types', TYPES, filters.types, 'types')}
            {group('Set', 'sets', SETS, filters.sets, 'sets')}
            {group('Rarity', 'rarities', RARITIES, filters.rarities, 'rarities', (v) => RARITY_LABELS[v])}
          </div>
          <div className="flex flex-wrap gap-4">
            {STAT_KEYS.map((k) => (
              <RangeInput key={k} stat={k} value={filters[k]} onChange={(r) => set({ [k]: r })} />
            ))}
            <label className="flex items-center gap-2 text-sm text-slate-400">
              Subtype
              <input
                value={filters.subtype}
                onChange={(e) => set({ subtype: e.target.value })}
                placeholder="e.g. Jedi"
                className="w-40 rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-slate-100"
              />
            </label>
          </div>
          {group('Tags', 'tags', TAGS, filters.tags, 'tags')}
          {group('Keywords', 'keywords', KEYWORDS, filters.keywords, 'keywords')}
          <p className="text-xs text-slate-500">
            Cost, power and health ranges skip cards with a variable (*) or missing value.
          </p>
        </div>
      )}
    </div>
  )
}

function RangeInput({ stat, value, onChange }: { stat: StatKey; value: Range; onChange: (r: Range) => void }) {
  const parse = (s: string) => (s === '' ? undefined : Math.max(0, Number(s)))
  const input = (side: 'min' | 'max') => (
    <input
      type="number"
      min={0}
      value={value[side] ?? ''}
      placeholder={side}
      aria-label={`${stat} ${side}`}
      onChange={(e) => onChange({ ...value, [side]: parse(e.target.value) })}
      className="w-16 rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-slate-100"
    />
  )
  return (
    <div className="flex items-center gap-2 text-sm text-slate-400">
      <span className="capitalize">{stat}</span>
      {input('min')}–{input('max')}
    </div>
  )
}
