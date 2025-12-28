import React from 'react'
import { LoadingState } from './components/states/LoadingState'
import { EmptyState } from './components/states/EmptyState'
import { ErrorState } from './components/states/ErrorState'
import './App.css'
import { Navbar } from './layout/Navbar'
import { AppShell } from './layout/AppShell'

function App() {
  return (
    <>
    <AppShell>
      <div className="App">
        <header className="App-header">
          <h1>State Components Demo</h1>
        </header>
        <main className="App-main">
          <section className="state-section">
            <h2>Loading State</h2>
            <LoadingState label="Loading data, please wait..." />
          </section>
          <section className="state-section">
            <h2>Empty State</h2>
            <EmptyState 
              title="No items found." 
              actionLabel="Add Item" 
              onAction={() => alert('Add Item clicked')} 
            />
          </section>
          <section className="state-section">
            <h2>Error State</h2>
            <ErrorState 
              message="Failed to load data." 
              onRetry={() => alert('Retry clicked')} 
            />
          </section>
        </main>
      </div>  
    </AppShell>
    </>
  )
}

export default App
