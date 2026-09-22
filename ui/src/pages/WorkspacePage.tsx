import { useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useDeckCardControls } from '../api/mutations'
import { useCatalog, useDeck } from '../api/queries'
import type { Card, CardType, Deck } from '../api/types'
import { CardDetail } from '../components/CardDetail'
import { CardGrid } from '../components/CardGrid'
import { DeckCardTile } from '../components/DeckCardTile'
import { DeckSidebar } from '../components/DeckSidebar'
import { DeckTray } from '../components/DeckTray'
import { FilterBar } from '../components/FilterBar'
import { UndoToast } from '../components/UndoToast'
import { TYPES, applyFilters, facetCounts, parseFilters, serializeFilters, type Filters } from '../lib/filters'
import { useMediaQuery } from '../lib/useMediaQuery'

/** Above this, the deck sidebar replaces the bottom tray (P4's "responsive" bit). */
const DESKTOP_SIDEBAR_QUERY = '(min-width: 1024px)'

type Mode = 'browse' | 'build'
const MODE_PARAM = 'mode'

/** Groups a deck's own cards by type for the spatial Build view (P8). */
function buildOrder(cards: Card[]): Card[] {
  return [...cards].sort(
    (a, b) => TYPES.indexOf(a.type) - TYPES.indexOf(b.type) || a.name.localeCompare(b.name),
  )
}

function BuildGrid({
  cards, deck, basePath, search, onChange,
}: {
  cards: Card[]
  deck: Deck
  basePath: string
  search: string
  onChange: (cardId: string, quantity: number) => void
}) {
  const quantityOf = (id: string) => deck.cards.find((c) => c['card-id'] === id)?.quantity ?? 0
  const groups = TYPES.map((type) => ({ type, cards: cards.filter((c) => c.type === type) })).filter(
    (g) => g.cards.length > 0,
  )
  return (
    <div className="space-y-8">
      {groups.map(({ type, cards: typeCards }) => (
        <section key={type}>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-400">
            {type} ({typeCards.reduce((n, c) => n + quantityOf(c['card-id']), 0)})
          </h2>
          <CardGrid>
            {typeCards.map((c) => (
              <DeckCardTile
                key={c['card-id']}
                card={c}
                to={`${basePath}/${c['card-id']}${search}`}
                quantity={quantityOf(c['card-id'])}
                deckSide={deck.side}
                onChange={(q) => onChange(c['card-id'], q)}
              />
            ))}
          </CardGrid>
        </section>
      ))}
    </div>
  )
}

// One workspace, two modes (P1): Browse is the full catalog with an add/remove stepper on
// every tile; Build groups the deck's own cards by type. Both share the same FilterBar and
// CardTile, and filters/sort/mode all live in the URL, so switching between them keeps scroll
// position and state (P2). Adding or removing a card never navigates away (P3).
export function WorkspacePage() {
  const { deckId = '', cardId } = useParams()
  const { data: deck, isPending: deckPending, error: deckError } = useDeck(deckId)
  const { data: catalog, isPending: catalogPending, error: catalogError } = useCatalog()
  const [params, setParams] = useSearchParams()
  const { search, key } = useLocation()
  const navigate = useNavigate()
  const [showOffSide, setShowOffSide] = useState(false)
  const { setQuantity } = useDeckCardControls(deckId)
  const isDesktop = useMediaQuery(DESKTOP_SIDEBAR_QUERY)

  const mode: Mode = params.get(MODE_PARAM) === 'build' ? 'build' : 'browse'
  const filters = useMemo(() => parseFilters(params), [params])
  const cardIndex = useMemo(() => new Map((catalog ?? []).map((c) => [c['card-id'], c])), [catalog])

  // Pre-filter Browse to the deck's side + Neutral (P5), unless the user picked sides
  // themselves or opted to see the other side too.
  const deckSide = deck?.side
  const effectiveFilters: Filters = useMemo(
    () =>
      deckSide && filters.sides.length === 0 && !showOffSide
        ? { ...filters, sides: [deckSide, 'N'] }
        : filters,
    [deckSide, filters, showOffSide],
  )

  const browseResults = useMemo(
    () => (catalog ? applyFilters(catalog, effectiveFilters) : []),
    [catalog, effectiveFilters],
  )
  const counts = useMemo(
    () => (catalog ? facetCounts(catalog, effectiveFilters) : null),
    [catalog, effectiveFilters],
  )
  const buildResults = useMemo(() => {
    if (!deck) return []
    return buildOrder(
      deck.cards.map((dc) => cardIndex.get(dc['card-id'])).filter((c): c is Card => c != null),
    )
  }, [deck, cardIndex])

  if (deckPending || catalogPending) return <p className="p-6">Loading…</p>
  if (deckError) return <p className="p-6 text-red-400">Failed to load deck: {deckError.message}</p>
  if (catalogError) return <p className="p-6 text-red-400">Failed to load cards: {catalogError.message}</p>
  if (!deck || !catalog) return null

  const results = mode === 'build' ? buildResults : browseResults
  const openIndex = cardId ? results.findIndex((c) => c['card-id'] === cardId) : -1
  const openCard = cardId ? cardIndex.get(cardId) : undefined
  const basePath = `/decks/${deckId}`

  const onFilterChange = (next: Filters) => setParams(serializeFilters(next), { replace: true })
  const setMode = (next: Mode) =>
    setParams(
      (prev) => {
        const p = new URLSearchParams(prev)
        if (next === 'browse') p.delete(MODE_PARAM)
        else p.set(MODE_PARAM, next)
        return p
      },
      { replace: true },
    )

  // Opening a card pushes a history entry; stepping prev/next replaces it, so one Back closes.
  const close = () =>
    key === 'default' ? navigate({ pathname: basePath, search }, { replace: true }) : navigate(-1)
  const step = (offset: number) => {
    const next = results[openIndex + offset]
    return next
      ? () => navigate({ pathname: `${basePath}/${next['card-id']}`, search }, { replace: true })
      : undefined
  }
  const quantityOf = (id: string) => deck.cards.find((c) => c['card-id'] === id)?.quantity ?? 0
  const jumpToType = (type: CardType) => {
    setMode('browse')
    onFilterChange({ ...filters, types: [type] })
  }

  return (
    <div className="flex min-h-[calc(100vh-3.25rem)] flex-col">
      <div className="flex flex-wrap items-center gap-3 border-b border-slate-800 px-6 py-2">
        <div className="flex rounded-md border border-slate-700 p-0.5 text-sm">
          {(['browse', 'build'] satisfies Mode[]).map((m) => (
            <button
              key={m}
              type="button"
              aria-pressed={mode === m}
              onClick={() => setMode(m)}
              className={`rounded px-3 py-1 capitalize ${
                mode === m ? 'bg-sky-600 text-white' : 'text-slate-400 hover:text-white'
              }`}
            >
              {m}
            </button>
          ))}
        </div>
        {mode === 'browse' && filters.sides.length === 0 && (
          <label className="flex items-center gap-1.5 text-xs text-slate-400">
            <input
              type="checkbox"
              checked={showOffSide}
              onChange={(e) => setShowOffSide(e.target.checked)}
            />
            Show the other side too
          </label>
        )}
      </div>

      <div className="flex min-h-0 flex-1">
        <div className="min-w-0 flex-1">
          {mode === 'browse' ? (
            <>
              <FilterBar
                filters={filters}
                onChange={onFilterChange}
                counts={counts!}
                resultCount={browseResults.length}
                totalCount={catalog.length}
              />
              <div className="@container p-6">
                {browseResults.length === 0 ? (
                  <p className="text-slate-400">No cards match these filters.</p>
                ) : (
                  <CardGrid>
                    {browseResults.map((c) => (
                      <DeckCardTile
                        key={c['card-id']}
                        card={c}
                        to={`${basePath}/${c['card-id']}${search}`}
                        quantity={quantityOf(c['card-id'])}
                        deckSide={deck.side}
                        onChange={(q) => setQuantity(c['card-id'], q)}
                      />
                    ))}
                  </CardGrid>
                )}
              </div>
            </>
          ) : (
            <div className="@container p-6">
              {buildResults.length === 0 ? (
                <p className="text-slate-400">No cards in this deck yet. Switch to Browse to add some.</p>
              ) : (
                <BuildGrid
                  cards={buildResults}
                  deck={deck}
                  basePath={basePath}
                  search={search}
                  onChange={setQuantity}
                />
              )}
            </div>
          )}
        </div>

        {/* Experimental: deck info as a right rail instead of the bottom tray, so the grid
            reads as narrower and less overwhelming (P4). */}
        {isDesktop && (
          <aside className="w-1/5 min-w-64 shrink-0 self-start overflow-y-auto border-l border-slate-800 bg-slate-950/40 p-4 sticky top-0 max-h-screen">
            <DeckSidebar deck={deck} cardIndex={cardIndex} onFilterType={jumpToType} />
          </aside>
        )}
      </div>

      {!isDesktop && (
        <DeckTray deck={deck} cardIndex={cardIndex} onFilterType={jumpToType} />
      )}
      <UndoToast deckId={deckId} cardIndex={cardIndex} />

      {cardId &&
        (openCard ? (
          <CardDetail
            card={openCard}
            onClose={close}
            onPrev={openIndex >= 0 ? step(-1) : undefined}
            onNext={openIndex >= 0 ? step(1) : undefined}
          />
        ) : (
          <p className="px-6 text-red-400">Card not found.</p>
        ))}
    </div>
  )
}
