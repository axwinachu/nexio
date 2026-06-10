import { useEffect, useMemo, useState } from 'react'
import { getJobs } from '../api/jobs'
import StatusBadge from '../components/StatusBadge'
import { formatDate } from '../utils/format'
import './Dashboard.css'

const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

export default function Calendar() {
  const [jobs, setJobs] = useState([])
  const [current, setCurrent] = useState(new Date())
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getJobs()
      .then(setJobs)
      .finally(() => setLoading(false))
  }, [])

  const year = current.getFullYear()
  const month = current.getMonth()

  const jobsByDate = useMemo(() => {
    const map = {}
    for (const job of jobs) {
      if (!job.appliedAt) continue
      const key = job.appliedAt.slice(0, 10)
      if (!map[key]) map[key] = []
      map[key].push(job)
    }
    return map
  }, [jobs])

  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const cells = []

  for (let i = 0; i < firstDay; i++) cells.push(null)
  for (let d = 1; d <= daysInMonth; d++) cells.push(d)

  const prevMonth = () => setCurrent(new Date(year, month - 1, 1))
  const nextMonth = () => setCurrent(new Date(year, month + 1, 1))

  const monthLabel = current.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })

  const upcoming = jobs
    .filter((j) => ['INTERVIEW', 'ASSESSMENT'].includes(j.status))
    .slice(0, 5)

  return (
    <>
      <header className="dashboard-header">
        <div>
          <h1>Calendar</h1>
          <p className="welcome-text">Application timeline</p>
        </div>
        <div className="calendar-nav">
          <button type="button" className="btn-outline btn-sm" onClick={prevMonth}>←</button>
          <span className="calendar-month">{monthLabel}</span>
          <button type="button" className="btn-outline btn-sm" onClick={nextMonth}>→</button>
        </div>
      </header>

      <div className="calendar-grid-layout">
        <section className="applications-section calendar-section">
          {loading ? (
            <p className="loading-text">Loading…</p>
          ) : (
            <>
              <div className="calendar-weekdays">
                {DAYS.map((d) => <span key={d}>{d}</span>)}
              </div>
              <div className="calendar-grid">
                {cells.map((day, i) => {
                  if (day === null) return <div key={`e-${i}`} className="calendar-cell empty" />
                  const key = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
                  const dayJobs = jobsByDate[key] || []
                  return (
                    <div key={key} className={`calendar-cell ${dayJobs.length ? 'has-events' : ''}`}>
                      <span className="calendar-day">{day}</span>
                      {dayJobs.slice(0, 2).map((j) => (
                        <div key={j.id} className={`calendar-event status-${j.status.toLowerCase()}`} title={`${j.company} — ${j.position || ''}`}>
                          {j.company}
                        </div>
                      ))}
                      {dayJobs.length > 2 && <span className="calendar-more">+{dayJobs.length - 2}</span>}
                    </div>
                  )
                })}
              </div>
            </>
          )}
        </section>

        <section className="applications-section">
          <h2>Upcoming Interviews & Assessments</h2>
          {upcoming.length === 0 ? (
            <p className="empty-hint">No interviews or assessments yet.</p>
          ) : (
            <ul className="upcoming-list">
              {upcoming.map((job) => (
                <li key={job.id} className="upcoming-item">
                  <div>
                    <strong>{job.company}</strong>
                    <span className="upcoming-role">{job.position || '—'}</span>
                  </div>
                  <div className="upcoming-meta">
                    <StatusBadge status={job.status} />
                    <span className="date-cell">{formatDate(job.appliedAt)}</span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </>
  )
}
