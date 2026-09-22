import { useEffect, useState } from 'react'

/** Tracks a CSS media query in JS, for layout choices Tailwind's responsive classes can't make
 * (e.g. mounting one of two different components instead of hiding one with CSS). */
export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => window.matchMedia(query).matches)
  useEffect(() => {
    const mql = window.matchMedia(query)
    const onChange = () => setMatches(mql.matches)
    onChange()
    mql.addEventListener('change', onChange)
    return () => mql.removeEventListener('change', onChange)
  }, [query])
  return matches
}
