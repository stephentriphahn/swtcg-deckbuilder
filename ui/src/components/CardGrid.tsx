import type { ReactNode } from 'react'

/**
 * Every tile is the same portrait-shaped box — landscape cards rotate to fit instead of
 * spanning extra columns (see CardTile) — so this is just a plain uniform grid. Columns key
 * off the grid's own container width (`@`-variants), not the viewport, so it narrows on its
 * own when it shares the page with the deck sidebar instead of assuming full-width. The parent
 * of `CardGrid` needs a `@container` class for these to take effect.
 */
export function CardGrid({ children }: { children: ReactNode }) {
  return (
    <div className="grid grid-cols-2 items-start gap-x-4 gap-y-6 @lg:grid-cols-3 @3xl:grid-cols-4 @6xl:grid-cols-5 @[100rem]:grid-cols-6">
      {children}
    </div>
  )
}
