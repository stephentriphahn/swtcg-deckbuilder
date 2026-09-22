import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDeckDetails, useDecks } from '../api/queries'
import { useCreateDeck, useDeleteDeck, useDuplicateDeck } from '../api/mutations'
import type { Deck, DeckSummary } from '../api/types'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { NewDeckDialog } from '../components/NewDeckDialog'
import { DECK_SIZE, SIDE_NAMES, deckCount } from '../lib/decks'

// Rename is not here yet: the API has no way to update a deck (see doc/ui-plan.md, API gaps).
export function DecksPage() {
  const { data: summaries, isPending, error } = useDecks()
  const details = useDeckDetails(summaries?.map((d) => d['deck-id']) ?? [])
  const create = useCreateDeck()
  const duplicate = useDuplicateDeck()
  const remove = useDeleteDeck()
  const navigate = useNavigate()
  const [creating, setCreating] = useState(false)
  const [deleting, setDeleting] = useState<DeckSummary | null>(null)

  if (isPending) return <p className="p-6">Loading decks…</p>
  if (error) return <p className="p-6 text-red-400">Failed to load decks: {error.message}</p>

  const names = summaries.map((d) => d.name)
  const sorted = [...summaries].sort((a, b) => a.name.localeCompare(b.name))
  const detailOf = (id: string): Deck | undefined =>
    details.find((q) => q.data?.['deck-id'] === id)?.data

  return (
    <div className="mx-auto max-w-5xl p-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Decks</h1>
        <button
          type="button"
          onClick={() => {
            create.reset()
            setCreating(true)
          }}
          className="rounded-md bg-sky-600 px-4 py-2 text-sm font-medium hover:bg-sky-500"
        >
          New deck
        </button>
      </div>

      {duplicate.error && (
        <p role="alert" className="mb-4 text-sm text-red-400">Couldn’t duplicate deck: {duplicate.error.message}</p>
      )}

      {sorted.length === 0 ? (
        <div className="rounded-lg border border-dashed border-slate-700 p-10 text-center text-slate-400">
          <p>No decks yet.</p>
          <p className="text-sm">Create one, then pick cards straight from the catalog.</p>
        </div>
      ) : (
        <ul className="grid gap-3 sm:grid-cols-2">
          {sorted.map((d) => {
            const detail = detailOf(d['deck-id'])
            const count = detail ? deckCount(detail) : null
            const violations = detail?.validation.violations.length ?? 0
            return (
              <li key={d['deck-id']} className="flex flex-col gap-3 rounded-lg border border-slate-800 bg-slate-900 p-4">
                <div className="flex items-start justify-between gap-3">
                  <Link to={`/decks/${d['deck-id']}`} className="min-w-0 truncate text-lg font-medium text-sky-400 hover:underline">
                    {d.name}
                  </Link>
                  <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs ${d.side === 'L' ? 'bg-sky-500/20 text-sky-200' : 'bg-red-500/20 text-red-200'}`}>
                    {SIDE_NAMES[d.side]}
                  </span>
                </div>
                <p className="text-sm text-slate-400">{d.format} · {d.owner}</p>
                <div className="flex items-center justify-between text-sm">
                  <span>
                    {count == null ? '…' : `${count}/${DECK_SIZE} cards`}
                    {detail &&
                      (detail.validation['valid?'] ? (
                        <span className="ml-2 text-emerald-400">Legal</span>
                      ) : (
                        <span className="ml-2 text-amber-400" title={detail.validation.violations.map((v) => v.message).join('\n')}>
                          Incomplete · {violations} {violations === 1 ? 'issue' : 'issues'}
                        </span>
                      ))}
                  </span>
                  <span className="flex gap-3">
                    <button
                      type="button"
                      disabled={duplicate.isPending}
                      onClick={() => duplicate.mutate({ id: d['deck-id'], existingNames: names })}
                      className="text-slate-400 hover:text-white disabled:opacity-50"
                    >
                      Duplicate
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        remove.reset()
                        setDeleting(d)
                      }}
                      className="text-slate-400 hover:text-red-400"
                    >
                      Delete
                    </button>
                  </span>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      {creating && (
        <NewDeckDialog
          existingNames={names}
          pending={create.isPending}
          error={create.error?.message}
          onCancel={() => setCreating(false)}
          onSubmit={(deck) =>
            create.mutate(deck, { onSuccess: (made) => navigate(`/decks/${made['deck-id']}`) })
          }
        />
      )}
      {deleting && (
        <ConfirmDialog
          title="Delete deck?"
          message={`“${deleting.name}” and its card list will be permanently deleted.`}
          confirmLabel="Delete deck"
          pending={remove.isPending}
          error={remove.error?.message}
          onCancel={() => setDeleting(null)}
          onConfirm={() => remove.mutate(deleting['deck-id'], { onSuccess: () => setDeleting(null) })}
        />
      )}
    </div>
  )
}
