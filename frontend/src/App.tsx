import { useState } from 'react'
import { Dashboard } from './pages/Dashboard'
import { History } from './pages/History'

type Page = 'dashboard' | 'history'

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('dashboard')

  return (
    <div className="min-h-screen bg-background">
      {currentPage === 'dashboard' ? (
        <Dashboard onNavigateToHistory={() => setCurrentPage('history')} />
      ) : (
        <History onBack={() => setCurrentPage('dashboard')} />
      )}
    </div>
  )
}

export default App
