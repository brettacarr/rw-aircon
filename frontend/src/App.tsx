import { useState } from 'react'
import { Dashboard } from './pages/Dashboard'
import { History } from './pages/History'
import { Schedules } from './pages/Schedules'

type Page = 'dashboard' | 'history' | 'schedules'

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('dashboard')

  return (
    <div className="min-h-screen bg-background">
      {currentPage === 'dashboard' && (
        <Dashboard
          onNavigateToHistory={() => setCurrentPage('history')}
          onNavigateToSchedules={() => setCurrentPage('schedules')}
        />
      )}
      {currentPage === 'history' && (
        <History onBack={() => setCurrentPage('dashboard')} />
      )}
      {currentPage === 'schedules' && (
        <Schedules onBack={() => setCurrentPage('dashboard')} />
      )}
    </div>
  )
}

export default App
