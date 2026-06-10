const STATUS_CONFIG = {
  APPLIED: { label: 'Applied', className: 'badge-applied' },
  ASSESSMENT: { label: 'Assessment', className: 'badge-assessment' },
  INTERVIEW: { label: 'Interview', className: 'badge-interview' },
  OFFER: { label: 'Offer', className: 'badge-offer' },
  REJECTED: { label: 'Rejected', className: 'badge-rejected' },
}

export default function StatusBadge({ status }) {
  const config = STATUS_CONFIG[status] || { label: status, className: 'badge-applied' }
  return <span className={`status-badge ${config.className}`}>{config.label}</span>
}
