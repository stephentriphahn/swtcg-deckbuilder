import { useState, type FormEvent } from 'react'
import type { NewDeck } from '../api/types'
import { SIDE_NAMES } from '../lib/decks'
import { Modal } from './Modal'

interface Props {
  existingNames: string[]
  pending: boolean
  /** Server error message, if the last attempt failed. */
  error?: string
  onSubmit: (deck: NewDeck) => void
  onCancel: () => void
}

const OWNER_KEY = 'swtcg.owner'
const readOwner = () => {
  try {
    return localStorage.getItem(OWNER_KEY) ?? ''
  } catch {
    return ''
  }
}

const field = 'w-full rounded-md border border-slate-700 bg-slate-950 px-3 py-2 text-sm focus:border-sky-400 focus:outline-none'

export function NewDeckDialog({ existingNames, pending, error, onSubmit, onCancel }: Props) {
  const [name, setName] = useState('')
  const [owner, setOwner] = useState(readOwner)
  const [format, setFormat] = useState('Standard')
  const [side, setSide] = useState<'L' | 'D'>('L')

  const trimmed = name.trim()
  const duplicate = existingNames.includes(trimmed)
  const valid = trimmed !== '' && owner.trim() !== '' && format.trim() !== '' && !duplicate

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (!valid) return
    try {
      localStorage.setItem(OWNER_KEY, owner.trim())
    } catch {
      /* storage unavailable: owner just isn't remembered */
    }
    onSubmit({ name: trimmed, owner: owner.trim(), format: format.trim(), side })
  }

  return (
    <Modal title="New deck" onClose={onCancel}>
      <form onSubmit={submit} className="space-y-3">
        <label className="block text-sm">
          <span className="mb-1 block text-slate-400">Name</span>
          <input autoFocus value={name} onChange={(e) => setName(e.target.value)} className={field} />
          {duplicate && <span className="mt-1 block text-xs text-red-400">A deck with this name already exists.</span>}
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-slate-400">Owner</span>
          <input value={owner} onChange={(e) => setOwner(e.target.value)} className={field} />
        </label>
        <label className="block text-sm">
          <span className="mb-1 block text-slate-400">Format</span>
          <input value={format} onChange={(e) => setFormat(e.target.value)} className={field} />
        </label>
        <fieldset className="text-sm">
          <legend className="mb-1 text-slate-400">Side</legend>
          <div className="flex gap-4">
            {(['L', 'D'] as const).map((s) => (
              <label key={s} className="flex items-center gap-2">
                <input type="radio" name="side" checked={side === s} onChange={() => setSide(s)} />
                {SIDE_NAMES[s]}
              </label>
            ))}
          </div>
        </fieldset>
        {error && <p role="alert" className="text-sm text-red-400">{error}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onCancel} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:border-slate-500">
            Cancel
          </button>
          <button
            type="submit"
            disabled={!valid || pending}
            className="rounded-md bg-sky-600 px-3 py-1.5 text-sm font-medium hover:bg-sky-500 disabled:opacity-50"
          >
            Create deck
          </button>
        </div>
      </form>
    </Modal>
  )
}
