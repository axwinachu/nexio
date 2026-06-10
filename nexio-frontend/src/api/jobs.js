import { apiFetch } from './client'

export function getSummary() {
  return apiFetch('/dashboard/summary')
}

export function getAnalytics() {
  return apiFetch('/analytics')
}

export function getJobs(params = {}) {
  const query = new URLSearchParams()
  if (params.status) query.set('status', params.status)
  if (params.search) query.set('search', params.search)
  const qs = query.toString()
  return apiFetch(`/jobs${qs ? `?${qs}` : ''}`)
}

export function getJob(id) {
  return apiFetch(`/jobs/${id}`)
}

export function createJob(data) {
  return apiFetch('/jobs', { method: 'POST', body: JSON.stringify(data) })
}

export function updateJobStatus(id, status) {
  return apiFetch(`/jobs/${id}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  })
}

export function deleteJob(id) {
  return apiFetch(`/jobs/${id}`, { method: 'DELETE' })
}

export function connectGmail() {
  return apiFetch('/email/connect')
}

export function getEmailStatus() {
  return apiFetch('/email/status')
}

export function syncEmails() {
  return apiFetch('/email/sync', { method: 'POST' })
}

export function extractJobs() {
  return apiFetch('/jobs/extract', { method: 'POST' })
}

export function getEmails(jobOnly = false) {
  return apiFetch(`/email/list?jobOnly=${jobOnly}`)
}
