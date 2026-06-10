import StatusBadge from './StatusBadge'
import StatusSelect from './StatusSelect'
import { formatDate } from '../utils/format'

export default function ApplicationsTable({
  jobs,
  loading,
  onStatusChange,
  onRowClick,
  showActions = true,
}) {
  if (loading) {
    return <p className="loading-text">Loading applications…</p>
  }

  if (jobs.length === 0) {
    return (
      <div className="empty-state">
        <p>No applications found.</p>
        <p className="empty-hint">Connect Gmail and sync emails, or add an application manually.</p>
      </div>
    )
  }

  return (
    <div className="table-wrapper">
      <table className="applications-table">
        <thead>
          <tr>
            <th>Company</th>
            <th>Job Role</th>
            <th>Status</th>
            <th>Applied</th>
            {showActions && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {jobs.map((job) => (
            <tr key={job.id} onClick={() => onRowClick?.(job)} className={onRowClick ? 'clickable-row' : ''}>
              <td className="company-cell">{job.company}</td>
              <td>{job.position || '—'}</td>
              <td onClick={(e) => e.stopPropagation()}>
                {onStatusChange ? (
                  <StatusSelect value={job.status} onChange={(s) => onStatusChange(job.id, s)} />
                ) : (
                  <StatusBadge status={job.status} />
                )}
              </td>
              <td className="date-cell">{formatDate(job.appliedAt)}</td>
              {showActions && (
                <td onClick={(e) => e.stopPropagation()}>
                  <button type="button" className="btn-link" onClick={() => onRowClick?.(job)}>
                    View
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
