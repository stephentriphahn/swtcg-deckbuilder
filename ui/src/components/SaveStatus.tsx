import { hasErrors, hasPendingWrites, useDeckDraftStore } from '../stores/deckDraft'

/** Ambient autosave indicator (P9) — no Save button, just a quiet status. */
export function SaveStatus() {
  const pending = useDeckDraftStore((s) => s.pending)
  if (hasErrors(pending))
    return <span className="text-sm text-red-400">Couldn't save — check your connection</span>
  if (hasPendingWrites(pending)) return <span className="text-sm text-slate-400">Saving…</span>
  return <span className="text-sm text-slate-500">Saved</span>
}
