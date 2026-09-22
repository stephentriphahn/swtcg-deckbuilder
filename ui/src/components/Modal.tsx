import { useEffect, useRef, type ReactNode } from 'react'

/** Native <dialog> modal (focus trap, inert background). Closing is delegated to `onClose`. */
export function Modal({
  title, onClose, children,
}: { title: string; onClose: () => void; children: ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const dialog = ref.current
    dialog?.showModal()
    return () => dialog?.close() // returns focus to whatever opened it
  }, [])
  return (
    <dialog
      ref={ref}
      aria-labelledby="modal-title"
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      onClick={(e) => e.target === ref.current && onClose()}
      className="m-auto w-[min(28rem,92vw)] rounded-xl border border-slate-700 bg-slate-900 p-5 text-slate-100 backdrop:bg-black/70"
    >
      <h2 id="modal-title" className="mb-4 text-lg font-semibold">{title}</h2>
      {children}
    </dialog>
  )
}
