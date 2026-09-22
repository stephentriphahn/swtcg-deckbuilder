import type { ReactNode } from 'react'

/**
 * Even column counts only, so two-column landscape tiles always fit; `grid-flow-dense`
 * back-fills gaps left when a landscape tile doesn't fit at the end of a row. Columns key off
 * the grid's own container width (`@`-variants), not the viewport, so the grid narrows on its
 * own when it shares the page with the deck sidebar instead of assuming full-width. The parent
 * of `CardGrid` needs a `@container` class for these to take effect.
 */
export function CardGrid({ children }: { children: ReactNode }) {
  return (
    <div className="grid grid-flow-dense grid-cols-2 items-start gap-x-4 gap-y-6 @lg:grid-cols-3 @3xl:grid-cols-4 @5xl:grid-cols-6 @7xl:grid-cols-8">
      {children}
    </div>
  )
}
