export type Side = 'L' | 'D' | 'N'
export type Rarity = 'C' | 'U' | 'R' | 'P'
export type CardType =
  | 'Character'
  | 'Space'
  | 'Ground'
  | 'Battle'
  | 'Equipment'
  | 'Location'
  | 'Mission'

/** Card as returned by GET /api/v1/cards (kebab-case JSON keys). */
export interface Card {
  'card-id': string
  name: string
  type: CardType
  side: Side
  'set-code': string
  number: number | null
  rarity: Rarity
  subtype?: string | null
  /** -1 means a variable stat ("*"). */
  cost?: number | null
  speed?: number | null
  power?: number | null
  health?: number | null
  text?: string | null
  script?: string | null
  usage?: string | null
  classification?: string | null
  /** Bare basename, no path or extension. */
  'image-file'?: string | null
}

export interface DeckSummary {
  'deck-id': string
  name: string
  owner: string
  format: string
  side: 'L' | 'D'
}

export interface DeckCard {
  'card-id': string
  quantity: number
}

export interface Violation {
  rule: string
  message: string
}

export interface Validation {
  'valid?': boolean
  violations: Violation[]
  warnings: Violation[]
}

export interface Deck extends DeckSummary {
  cards: DeckCard[]
  validation: Validation
}

export interface NewDeck {
  name: string
  owner: string
  format: string
  side: 'L' | 'D'
}
