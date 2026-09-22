import { useState } from 'react'
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
 * Visible everywhere in the workspace, not just Build mode (P4): size, cost curve, type
 * minimums and save status, so browsing and building can be cross-referenced at a glance.
 */
export function DeckTray({ deck, cardIndex, onFilterType }: Props) {
  const [expanded, setExpanded] = useState(false)
  const count = deckCount(deck)
  const violations = precheckViolations(deck.side, deck.cards, cardIndex)
  const curve = costCurve(deck.cards, cardIndex)
  const typeCounts = deckTypeCounts(deck.cards, cardIndex)

  return (
    <div className="sticky bottom-0 z-10 border-t border-slate-800 bg-slate-950/95 backdrop-blur">
      <button
        type="button"
        onClick={() => setExpanded(!expanded)}
        aria-expanded={expanded}
        aria-controls="deck-tray-panel"
        className="flex w-full flex-wrap items-center gap-x-4 gap-y-1 px-6 py-2 text-left"
      >
        <span className="font-medium">{deck.name}</span>
        <span
          className={`rounded-full px-2 py-0.5 text-xs ${
            deck.side === 'L' ? 'bg-sky-500/20 text-sky-200' : 'bg-red-500/20 text-red-200'
          }`}
        >
          {SIDE_NAMES[deck.side]}
        </span>
        <span className={count === DECK_SIZE ? 'text-emerald-400' : 'text-slate-300'}>
          {count}/{DECK_SIZE}
        </span>
        {violations.length > 0 && (
          <span className="text-xs text-amber-400">
            {violations.length} {violations.length === 1 ? 'issue' : 'issues'}
          </span>
        )}
        <span className="ml-auto flex items-center gap-3">
          <SaveStatus />
          <span aria-hidden className={`text-xs transition-transform ${expanded ? 'rotate-180' : ''}`}>▴</span>
        </span>
      </button>
      {expanded && (
        <div id="deck-tray-panel" className="grid gap-6 border-t border-slate-800 px-6 py-4 sm:grid-cols-3">
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
      )}
    </div>
  )
}
