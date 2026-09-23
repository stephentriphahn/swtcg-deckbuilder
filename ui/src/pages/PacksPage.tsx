import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useOpenPack } from '../api/mutations'
import { usePacks } from '../api/queries'
import { PackTile } from '../components/PackTile'
import { readOwner, writeOwner } from '../lib/owner'

// Phase 2: pick a set, open it. The reveal itself (face-down -> flip) is Phase 3 —
// PackRevealPage currently just shows what was drawn.
export function PacksPage() {
  const { data: packs, isPending, error } = usePacks()
  const openPack = useOpenPack()
  const navigate = useNavigate()
  const [owner, setOwner] = useState(readOwner)

  if (isPending) return <p className="p-6">Loading packs…</p>
  if (error) return <p className="p-6 text-red-400">Failed to load packs: {error.message}</p>

  const open = (setCode: string) => {
    writeOwner(owner.trim())
    openPack.mutate(
      { owner: owner.trim(), setCode },
      { onSuccess: (opening) => navigate(`/packs/${opening['opening-id']}`) },
    )
  }

  return (
    <div className="mx-auto max-w-5xl p-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold">Packs</h1>
        <label className="flex items-center gap-2 text-sm text-slate-400">
          Opening as
          <input
            value={owner}
            onChange={(e) => setOwner(e.target.value)}
            placeholder="your name"
            className="w-40 rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-slate-100"
          />
        </label>
      </div>

      {openPack.error && (
        <p role="alert" className="mb-4 text-sm text-red-400">
          Couldn’t open that pack: {openPack.error.message}
        </p>
      )}
      {!owner.trim() && (
        <p className="mb-4 text-sm text-amber-400">Enter a name above before opening a pack.</p>
      )}

      <div className="grid grid-cols-3 gap-6 sm:grid-cols-5">
        {packs.map((pack) => (
          <PackTile
            key={pack['set-code']}
            pack={pack}
            disabled={!owner.trim() || openPack.isPending}
            onOpen={() => open(pack['set-code'])}
          />
        ))}
      </div>
    </div>
  )
}
