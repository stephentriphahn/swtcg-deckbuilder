import type { CardType, Deck } from '../api/types'
import { DECK_SIZE, SIDE_NAMES, deckCount } from '../lib/decks'
import { costCurve, deckTypeCounts, precheckViolations, type CardIndex } from '../lib/deckRules'
import { CostCurve } from './CostCurve'
import { SaveStatus } from './SaveStatus'
import { TypeMeters } from './TypeMeters'
import { ValidationPanel } from './ValidationPanel'

interface Props {
  deck: Deck
  cardIndex: CardIndex
  onFilterType?: (type: CardType) => void
}

/**
 * Desktop right-rail counterpart to `DeckTray`: the same size/curve/meters/validation info,
 * always expanded and visible alongside the grid instead of pinned under it (P4). Narrower
 * screens get `DeckTray` instead — see the `useMediaQuery` toggle in `WorkspacePage`.
 */
export function DeckSidebar({ deck, cardIndex, onFilterType }: Props) {
  const count = deckCount(deck)
  const violations = precheckViolations(deck.side, deck.cards, cardIndex)
  const curve = costCurve(deck.cards, cardIndex)
  const typeCounts = deckTypeCounts(deck.cards, cardIndex)

  return (
    <div className="space-y-6">
      <div>
        <div className="flex items-center justify-between gap-2">
          <h2 className="truncate text-lg font-semibold">{deck.name}</h2>
          <span
            className={`shrink-0 rounded-full px-2 py-0.5 text-xs ${
              deck.side === 'L' ? 'bg-sky-500/20 text-sky-200' : 'bg-red-500/20 text-red-200'
            }`}
          >
            {SIDE_NAMES[deck.side]}
          </span>
        </div>
        <div className="mt-1 flex items-center gap-3 text-sm">
          <span className={count === DECK_SIZE ? 'text-emerald-400' : 'text-slate-300'}>
            {count}/{DECK_SIZE} cards
          </span>
          <SaveStatus />
        </div>
      </div>
      <div>
        <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">Cost curve</h3>
        <CostCurve data={curve} />
      </div>
      <div>
        <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">Type minimums</h3>
        <TypeMeters counts={typeCounts} />
      </div>
      <div>
        <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">Validation</h3>
        <ValidationPanel violations={violations} onFilterType={onFilterType} />
      </div>
    </div>
  )
}
