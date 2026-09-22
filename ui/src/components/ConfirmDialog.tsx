import { Modal } from './Modal'

interface Props {
  title: string
  message: string
  confirmLabel: string
  pending?: boolean
  error?: string
  onConfirm: () => void
  onCancel: () => void
}

/** For hard-to-reverse actions only (deleting a whole deck); card removal uses Undo instead. */
export function ConfirmDialog({ title, message, confirmLabel, pending, error, onConfirm, onCancel }: Props) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p className="text-sm text-slate-300">{message}</p>
      {error && <p role="alert" className="mt-3 text-sm text-red-400">{error}</p>}
      <div className="mt-5 flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="rounded-md border border-slate-700 px-3 py-1.5 text-sm hover:border-slate-500">
          Cancel
        </button>
        <button
          type="button"
          onClick={onConfirm}
          disabled={pending}
          className="rounded-md bg-red-600 px-3 py-1.5 text-sm font-medium hover:bg-red-500 disabled:opacity-50"
        >
          {confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
