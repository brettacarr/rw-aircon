import { useState } from 'react'
import { Dashboard } from './pages/Dashboard'
import { History } from './pages/History'
import { Schedules } from './pages/Schedules'
import { AutoModeSettings } from './pages/AutoModeSettings'

type Page = 'dashboard' | 'history' | 'schedules' | 'automode'

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('dashboard')

  return (
    <div className="min-h-screen bg-background">
      {currentPage === 'dashboard' && (
        <Dashboard
          onNavigateToHistory={() => setCurrentPage('history')}
          onNavigateToSchedules={() => setCurrentPage('schedules')}
          onNavigateToAutoMode={() => setCurrentPage('automode')}
        />
      )}
      {currentPage === 'history' && (
        <History onBack={() => setCurrentPage('dashboard')} />
      )}
      {currentPage === 'schedules' && (
        <Schedules onBack={() => setCurrentPage('dashboard')} />
      )}
      {currentPage === 'automode' && (
        <AutoModeSettings onBack={() => setCurrentPage('dashboard')} />
      )}
    </div>
  )
}

export default App
