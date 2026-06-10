import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { connectGmail, extractJobs, getJobs, getSummary, syncEmails } from '../api/jobs'
import { useAuth } from '../context/useAuth'
import ApplicationsTable from '../components/ApplicationsTable'
import JobDetailModal from '../components/JobDetailModal'
import './Dashboard.css'

export default function Dashboard() {
  const { user } = useAuth()
  const [summary, setSummary] = useState(null)
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState('')
  const [message, setMessage] = useState('')
  const [selectedJob, setSelectedJob] = useState(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const [summaryData, jobsData] = await Promise.all([getSummary(), getJobs()])
        if (!cancelled) {
          setSummary(summaryData)
          setJobs(jobsData.slice(0, 8))
        }
      } catch {
        if (!cancelled) setMessage('Failed to load dashboard')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [])

  const runAction = async (key, fn, successMsg) => {
    setActionLoading(key)
    setMessage('')
    try {
      const result = await fn()
      if (successMsg) {
        const extra = result.jobsCreated != null
          ? ` (${result.jobsCreated} created${result.jobsUpdated ? `, ${result.jobsUpdated} updated` : ''})`
          : result.newEmails != null ? ` (${result.newEmails} new)` : ''
        setMessage(successMsg + extra)
      }
      const [summaryData, jobsData] = await Promise.all([getSummary(), getJobs()])
      setSummary(summaryData)
      setJobs(jobsData.slice(0, 8))
    } catch (err) {
      setMessage(err.message)
    } finally {
      setActionLoading('')
    }
  }

  const stats = [
    { label: 'Total', value: summary?.totalApplications ?? 0, filter: null },
    { label: 'Applied', value: summary?.applied ?? 0, filter: 'APPLIED' },
    { label: 'Assessment', value: summary?.assessment ?? 0, filter: 'ASSESSMENT' },
    { label: 'Interview', value: summary?.interview ?? 0, filter: 'INTERVIEW' },
    { label: 'Offers', value: summary?.offer ?? 0, filter: 'OFFER' },
    { label: 'Rejected', value: summary?.rejected ?? 0, filter: 'REJECTED' },
  ]

  return (
    <>
      <header className="dashboard-header">
        <div>
          <h1>Dashboard</h1>
          <p className="welcome-text">Welcome back, {user?.name}</p>
        </div>
        <div className="header-actions">
          <button type="button" className="btn-outline" disabled={!!actionLoading}
            onClick={() => runAction('connect', async () => { const { authUrl } = await connectGmail(); window.location.href = authUrl }, null)}>
            {actionLoading === 'connect' ? 'Connecting…' : 'Connect Gmail'}
          </button>
          <button type="button" className="btn-outline" disabled={!!actionLoading}
            onClick={() => runAction('sync', syncEmails, 'Synced emails')}>
            {actionLoading === 'sync' ? 'Syncing…' : 'Sync Emails'}
          </button>
          <button type="button" className="btn-primary btn-sm" disabled={!!actionLoading}
            onClick={() => runAction('extract', extractJobs, 'Extraction complete')}>
            {actionLoading === 'extract' ? 'Extracting…' : 'Extract Jobs'}
          </button>
        </div>
      </header>

      {message && <div className="dashboard-message">{message}</div>}

      <section className="stats-grid stats-grid-6">
        {stats.map((stat) => (
          <Link key={stat.label} to={stat.filter ? `/applications?status=${stat.filter}` : '/applications'} className="stat-card stat-card-link">
            <span className="stat-value">{stat.value}</span>
            <span className="stat-label">{stat.label}</span>
          </Link>
        ))}
      </section>

      <section className="applications-section">
        <div className="section-header">
          <h2>Recent Applications</h2>
          <Link to="/applications" className="btn-link">View all →</Link>
        </div>
        <ApplicationsTable jobs={jobs} loading={loading} onRowClick={setSelectedJob} showActions={false} />
      </section>

      {selectedJob && (
        <JobDetailModal job={selectedJob} onClose={() => setSelectedJob(null)} />
      )}
    </>
  )
}
