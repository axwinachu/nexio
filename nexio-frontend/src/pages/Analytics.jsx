import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAnalytics } from '../api/jobs'
import './Dashboard.css'

export default function Analytics() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getAnalytics()
      .then(setData)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="loading-text">Loading analytics…</p>
  if (!data) return <p className="loading-text">Failed to load analytics</p>

  const funnel = [
    { label: 'Applied', value: data.applied, color: '#2563eb' },
    { label: 'Assessment', value: data.assessment, color: '#ea580c' },
    { label: 'Interview', value: data.interview, color: '#7c3aed' },
    { label: 'Offer', value: data.offer, color: '#16a34a' },
    { label: 'Rejected', value: data.rejected, color: '#dc2626' },
  ]

  const maxFunnel = Math.max(...funnel.map((f) => f.value), 1)
  const maxWeekly = Math.max(...(data.weeklyApplications?.map((w) => w.count) || [1]), 1)

  return (
    <>
      <header className="dashboard-header">
        <div>
          <h1>Analytics</h1>
          <p className="welcome-text">Track your job search performance</p>
        </div>
      </header>

      <section className="stats-grid stats-grid-3">
        <div className="stat-card">
          <span className="stat-value">{data.totalApplications}</span>
          <span className="stat-label">Total Applications</span>
        </div>
        <div className="stat-card">
          <span className="stat-value">{data.responseRate}%</span>
          <span className="stat-label">Response Rate</span>
        </div>
        <div className="stat-card">
          <span className="stat-value">{data.offer}</span>
          <span className="stat-label">Offers Received</span>
        </div>
      </section>

      <div className="analytics-grid">
        <section className="applications-section">
          <h2>Application Funnel</h2>
          <div className="funnel-chart">
            {funnel.map((item) => (
              <Link key={item.label} to={`/applications?status=${item.label.toUpperCase()}`} className="funnel-row">
                <span className="funnel-label">{item.label}</span>
                <div className="funnel-bar-bg">
                  <div
                    className="funnel-bar"
                    style={{ width: `${(item.value / maxFunnel) * 100}%`, background: item.color }}
                  />
                </div>
                <span className="funnel-value">{item.value}</span>
              </Link>
            ))}
          </div>
        </section>

        <section className="applications-section">
          <h2>Applications per Week</h2>
          {data.weeklyApplications?.length === 0 ? (
            <p className="empty-hint">No data yet — sync and extract jobs to see trends.</p>
          ) : (
            <div className="weekly-chart">
              {data.weeklyApplications?.map((w) => (
                <div key={w.week} className="weekly-bar-col">
                  <div
                    className="weekly-bar"
                    style={{ height: `${(w.count / maxWeekly) * 120}px` }}
                    title={`${w.count} applications`}
                  />
                  <span className="weekly-label">{w.week.replace(/^\d+-W/, 'W')}</span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </>
  )
}
