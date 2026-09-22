import { NavLink, Navigate, Route, Routes } from 'react-router-dom'
import { CatalogPage } from './pages/CatalogPage'
import { DecksPage } from './pages/DecksPage'
import { WorkspacePage } from './pages/WorkspacePage'

const link = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'text-white' : 'text-slate-400 hover:text-white'

export default function App() {
  return (
    <>
      <nav className="flex gap-6 border-b border-slate-800 px-6 py-3 text-sm font-medium">
        <span className="font-bold">SWTCG</span>
        <NavLink to="/cards" className={link}>Cards</NavLink>
        <NavLink to="/decks" className={link}>Decks</NavLink>
      </nav>
      <Routes>
        <Route path="/" element={<Navigate to="/cards" replace />} />
        <Route path="/cards/:cardId?" element={<CatalogPage />} />
        <Route path="/decks" element={<DecksPage />} />
        <Route path="/decks/:deckId/:cardId?" element={<WorkspacePage />} />
      </Routes>
    </>
  )
}
