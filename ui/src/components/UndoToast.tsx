import { useEffect } from 'react'
import { useDeckCardControls } from '../api/mutations'
import type { Card } from '../api/types'
import { useDeckDraftStore } from '../stores/deckDraft'

const AUTO_DISMISS_MS = 6000

/** Undo over confirm (P7): removing a card is instant, with a brief window to reverse it. */
export function UndoToast({ deckId, cardIndex }: { deckId: string; cardIndex: Map<string, Card> }) {
  const undo = useDeckDraftStore((s) => s.undo)
  const setUndo = useDeckDraftStore((s) => s.setUndo)
  const { setQuantity } = useDeckCardControls(deckId)

  useEffect(() => {
    if (!undo) return
    const t = setTimeout(() => setUndo(null), AUTO_DISMISS_MS)
    return () => clearTimeout(t)
  }, [undo, setUndo])

  if (!undo || undo.deckId !== deckId) return null
  const card = cardIndex.get(undo.card['card-id'])

  return (
    <div className="fixed bottom-20 left-1/2 z-20 -translate-x-1/2 rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-sm shadow-xl">
      Removed {card?.name ?? 'card'}
      <button
        type="button"
        onClick={() => {
          setQuantity(undo.card['card-id'], undo.card.quantity)
          setUndo(null)
        }}
        className="ml-3 font-medium text-sky-400 hover:underline"
      >
        Undo
      </button>
    </div>
  )
}
