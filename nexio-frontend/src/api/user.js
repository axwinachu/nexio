import { apiFetch } from './client'

export function getProfile() {
  return apiFetch('/user/me')
}

export function updateProfile(data) {
  return apiFetch('/user/me', { method: 'PUT', body: JSON.stringify(data) })
}

export function deleteAccount() {
  return apiFetch('/user/me', { method: 'DELETE' })
}
