import { useMemo } from 'react'
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useCatalog } from '../api/queries'
import type { CardType } from '../api/types'
import { CardDetail } from '../components/CardDetail'
import { CardGrid } from '../components/CardGrid'
import { CardTile } from '../components/CardTile'
import { FilterBar } from '../components/FilterBar'
import { TypeNav } from '../components/TypeNav'
import { applyFilters, facetCounts, parseFilters, serializeFilters, type Filters } from '../lib/filters'

// The open card is the `:cardId` route param, so a card is deep-linkable and back closes it.
// Filters live in the URL (shareable, survives reload, restored by back/forward) and all
// filtering happens client-side against the fully loaded catalog.
export function CatalogPage() {
  const { data, isPending, error } = useCatalog()
  const [params, setParams] = useSearchParams()
  const filters = useMemo(() => parseFilters(params), [params])
  const { cardId } = useParams()
  const { search, key } = useLocation()
  const navigate = useNavigate()

  const results = useMemo(() => (data ? applyFilters(data, filters) : []), [data, filters])
  const counts = useMemo(() => (data ? facetCounts(data, filters) : null), [data, filters])

  const openIndex = cardId ? results.findIndex((c) => c['card-id'] === cardId) : -1
  const open = cardId ? data?.find((c) => c['card-id'] === cardId) : undefined

  if (isPending) return <p className="p-6">Loading cards…</p>
  if (error) return <p className="p-6 text-red-400">Failed to load cards: {error.message}</p>

  // replace: filter tweaks shouldn't fill the history stack with one entry per keystroke
  const onChange = (next: Filters) => setParams(serializeFilters(next), { replace: true })
  // A tab, not a chip: picking a type replaces the selection instead of adding to it, and
  // clicking the active one again (or "All types") clears it.
  const selectType = (type: CardType | null) =>
    onChange({ ...filters, types: type && filters.types[0] !== type ? [type] : [] })

  // Opening pushes a history entry; prev/next replace it so one Back closes the modal.
  const close = () =>
    key === 'default' ? navigate({ pathname: '/cards', search }, { replace: true }) : navigate(-1)
  const step = (offset: number) => {
    const next = results[openIndex + offset]
    return next ? () => navigate({ pathname: `/cards/${next['card-id']}`, search }, { replace: true }) : undefined
  }

  return (
    <div className="flex flex-1">
      <aside className="sticky top-0 max-h-screen w-40 shrink-0 self-start overflow-y-auto border-r border-slate-800 p-3">
        <TypeNav
          selected={filters.types[0] ?? null}
          counts={counts!.types}
          onSelect={selectType}
        />
      </aside>

      <div className="min-w-0 flex-1">
        <FilterBar
          filters={filters}
          onChange={onChange}
          counts={counts!}
          resultCount={results.length}
          totalCount={data.length}
          showTypeFilter={false}
        />
        <div className="@container p-6">
          {results.length === 0 ? (
            <p className="text-slate-400">No cards match these filters.</p>
          ) : (
            <CardGrid>
              {results.map((c) => (
                <CardTile key={c['card-id']} card={c} to={`/cards/${c['card-id']}${search}`} />
              ))}
            </CardGrid>
          )}
        </div>
      </div>

      {cardId &&
        (open ? (
          <CardDetail
            card={open}
            onClose={close}
            // neighbours only exist when the card is in the current filtered list
            onPrev={openIndex >= 0 ? step(-1) : undefined}
            onNext={openIndex >= 0 ? step(1) : undefined}
          />
        ) : (
          <p className="px-6 text-red-400">
            Card not found.{' '}
            <Link className="underline" to={{ pathname: '/cards', search }}>Back to cards</Link>
          </p>
        ))}
    </div>
  )
}
