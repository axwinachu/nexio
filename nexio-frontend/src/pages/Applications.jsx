import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { createJob, deleteJob, getJobs, updateJobStatus } from '../api/jobs'
import ApplicationsTable from '../components/ApplicationsTable'
import JobDetailModal from '../components/JobDetailModal'
import { STATUSES, STATUS_LABELS } from '../utils/format'
import './Dashboard.css'

export default function Applications() {
  const [searchParams, setSearchParams] = useSearchParams()
  const statusFilter = searchParams.get('status') || ''
  const [search, setSearch] = useState('')
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [selectedJob, setSelectedJob] = useState(null)
  const [showAddForm, setShowAddForm] = useState(false)
  const [newJob, setNewJob] = useState({ company: '', position: '', status: 'APPLIED' })

  const loadJobs = useCallback(async () => {
    setLoading(true)
    try {
      const params = {}
      if (statusFilter) params.status = statusFilter
      if (search.trim()) params.search = search.trim()
      setJobs(await getJobs(params))
    } catch (err) {
      setMessage(err.message)
    } finally {
      setLoading(false)
    }
  }, [statusFilter, search])

  useEffect(() => {
    const timer = setTimeout(loadJobs, search ? 300 : 0)
    return () => clearTimeout(timer)
  }, [loadJobs, search])

  const handleStatusChange = async (id, status) => {
    try {
      const updated = await updateJobStatus(id, status)
      setJobs((prev) => prev.map((j) => (j.id === id ? updated : j)))
      if (selectedJob?.id === id) setSelectedJob(updated)
    } catch (err) {
      setMessage(err.message)
    }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this application?')) return
    try {
      await deleteJob(id)
      setJobs((prev) => prev.filter((j) => j.id !== id))
      setSelectedJob(null)
      setMessage('Application deleted')
    } catch (err) {
      setMessage(err.message)
    }
  }

  const handleAddJob = async (e) => {
    e.preventDefault()
    try {
      const created = await createJob(newJob)
      setJobs((prev) => [created, ...prev])
      setNewJob({ company: '', position: '', status: 'APPLIED' })
      setShowAddForm(false)
      setMessage('Application added')
    } catch (err) {
      setMessage(err.message)
    }
  }

  const setFilter = (status) => {
    if (status) setSearchParams({ status })
    else setSearchParams({})
  }

  return (
    <>
      <header className="dashboard-header">
        <div>
          <h1>Applications</h1>
          <p className="welcome-text">{jobs.length} application{jobs.length !== 1 ? 's' : ''}</p>
        </div>
        <button type="button" className="btn-primary btn-sm" onClick={() => setShowAddForm(!showAddForm)}>
          {showAddForm ? 'Cancel' : '+ Add Application'}
        </button>
      </header>

      {message && <div className="dashboard-message">{message}</div>}

      {showAddForm && (
        <form className="add-job-form" onSubmit={handleAddJob}>
          <input placeholder="Company *" value={newJob.company} onChange={(e) => setNewJob({ ...newJob, company: e.target.value })} required />
          <input placeholder="Position" value={newJob.position} onChange={(e) => setNewJob({ ...newJob, position: e.target.value })} />
          <select value={newJob.status} onChange={(e) => setNewJob({ ...newJob, status: e.target.value })}>
            {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
          </select>
          <button type="submit" className="btn-primary btn-sm">Save</button>
        </form>
      )}

      <div className="filter-bar">
        <div className="filter-tabs">
          <button type="button" className={`filter-tab ${!statusFilter ? 'active' : ''}`} onClick={() => setFilter('')}>All</button>
          {STATUSES.map((s) => (
            <button key={s} type="button" className={`filter-tab ${statusFilter === s ? 'active' : ''}`} onClick={() => setFilter(s)}>
              {STATUS_LABELS[s]}
            </button>
          ))}
        </div>
        <input
          type="search"
          className="search-input"
          placeholder="Search company, role, email…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <section className="applications-section">
        <ApplicationsTable
          jobs={jobs}
          loading={loading}
          onStatusChange={handleStatusChange}
          onRowClick={setSelectedJob}
        />
      </section>

      {selectedJob && (
        <JobDetailModal
          job={selectedJob}
          onClose={() => setSelectedJob(null)}
          onStatusChange={handleStatusChange}
          onDelete={handleDelete}
        />
      )}
    </>
  )
}
