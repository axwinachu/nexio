import { STATUSES, STATUS_LABELS } from '../utils/format'

export default function StatusSelect({ value, onChange, disabled }) {
  return (
    <select
      className={`status-select status-${value?.toLowerCase()}`}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
    >
      {STATUSES.map((s) => (
        <option key={s} value={s}>
          {STATUS_LABELS[s]}
        </option>
      ))}
    </select>
  )
}
