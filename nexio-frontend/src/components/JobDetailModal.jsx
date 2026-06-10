import StatusBadge from './StatusBadge'
import { formatDate, formatDateTime } from '../utils/format'

export default function JobDetailModal({ job, onClose, onStatusChange, onDelete }) {
  if (!job) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{job.company}</h2>
          <button type="button" className="modal-close" onClick={onClose}>×</button>
        </div>

        <div className="modal-body">
          <div className="detail-row">
            <span className="detail-label">Position</span>
            <span>{job.position || '—'}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Status</span>
            <StatusBadge status={job.status} />
          </div>
          <div className="detail-row">
            <span className="detail-label">Applied</span>
            <span>{formatDate(job.appliedAt)}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Last Updated</span>
            <span>{formatDateTime(job.updatedAt)}</span>
          </div>
          {job.emailSubject && (
            <div className="detail-row">
              <span className="detail-label">Source Email</span>
              <span className="detail-email">{job.emailSubject}</span>
            </div>
          )}
        </div>

        <div className="modal-footer">
          {onStatusChange && (
            <div className="status-actions">
              {['APPLIED', 'ASSESSMENT', 'INTERVIEW', 'OFFER', 'REJECTED'].map((s) => (
                <button
                  key={s}
                  type="button"
                  className={`btn-outline btn-sm ${job.status === s ? 'active' : ''}`}
                  onClick={() => onStatusChange(job.id, s)}
                >
                  {s.charAt(0) + s.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
          )}
          {onDelete && (
            <button type="button" className="btn-danger btn-sm" onClick={() => onDelete(job.id)}>
              Delete
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
