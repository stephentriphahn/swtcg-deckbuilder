import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { CostCurve } from './CostCurve'

describe('CostCurve', () => {
  it('shows a count label on every bar, and no bar for an omitted (zero-count) cost', () => {
    render(<CostCurve data={[{ label: '3', count: 17 }, { label: '6', count: 1 }]} />)
    expect(screen.getByText('17')).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.queryByText('0')).toBeNull() // no bucket was passed in for cost 0
    expect(screen.getAllByText(/^[0-9]+$/)).toHaveLength(4) // 2 count labels + 2 cost labels
  })
  it('shows a message instead of an empty chart', () => {
    render(<CostCurve data={[]} />)
    expect(screen.getByText('No cards yet.')).toBeInTheDocument()
  })
  it('spells out "*" and "–" in the tooltip instead of leaving the bare symbol', () => {
    render(<CostCurve data={[{ label: '*', count: 2 }, { label: '–', count: 1 }]} />)
    expect(document.querySelector('[title="Variable cost: 2"]')).not.toBeNull()
    expect(document.querySelector('[title="No cost: 1"]')).not.toBeNull()
  })
})
