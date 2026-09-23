const OWNER_KEY = 'swtcg.owner'

/**
 * The remembered player name, shared between decks and packs. This app has no auth — just a
 * free-text "owner" string per doc/pack-opening-design.md §1 — so this is the closest thing to
 * "who's using it right now," remembered locally so it doesn't have to be retyped everywhere.
 */
export function readOwner(): string {
  try {
    return localStorage.getItem(OWNER_KEY) ?? ''
  } catch {
    return ''
  }
}

export function writeOwner(owner: string): void {
  try {
    localStorage.setItem(OWNER_KEY, owner)
  } catch {
    /* storage unavailable: owner just isn't remembered */
  }
}
