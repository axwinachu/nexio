import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { connectGmail, extractJobs, getEmailStatus, syncEmails } from '../api/jobs'
import { deleteAccount, getProfile, updateProfile } from '../api/user'
import { useAuth } from '../context/useAuth'
import './Dashboard.css'

export default function Settings() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [profile, setProfile] = useState(null)
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [gmailStatus, setGmailStatus] = useState(null)
  const [message, setMessage] = useState(() =>
    searchParams.get('gmail') === 'connected' ? 'Gmail connected successfully!' : ''
  )
  const [error, setError] = useState('')
  const [loading, setLoading] = useState('')

  useEffect(() => {
    Promise.all([getProfile(), getEmailStatus()])
      .then(([p, g]) => {
        setProfile(p)
        setName(p.name)
        setGmailStatus(g)
      })
      .catch((err) => setError(err.message))
  }, [searchParams])

  const handleSaveProfile = async (e) => {
    e.preventDefault()
    setLoading('profile')
    setError('')
    try {
      const body = { name }
      if (password) body.password = password
      const updated = await updateProfile(body)
      setProfile(updated)
      setPassword('')
      setMessage('Profile updated')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading('')
    }
  }

  const handleConnectGmail = async () => {
    setLoading('gmail')
    try {
      const { authUrl } = await connectGmail()
      window.location.href = authUrl
    } catch (err) {
      setError(err.message)
      setLoading('')
    }
  }

  const handleSync = async () => {
    setLoading('sync')
    try {
      const result = await syncEmails()
      setMessage(`Synced ${result.newEmails} new email(s)`)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading('')
    }
  }

  const handleExtract = async () => {
    setLoading('extract')
    try {
      const result = await extractJobs()
      setMessage(`Created ${result.jobsCreated}, updated ${result.jobsUpdated || 0} application(s)`)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading('')
    }
  }

  const handleDeleteAccount = async () => {
    if (!confirm('Delete your account permanently? This cannot be undone.')) return
    try {
      await deleteAccount()
      logout()
      navigate('/login')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <header className="dashboard-header">
        <div>
          <h1>Settings</h1>
          <p className="welcome-text">Manage your account and integrations</p>
        </div>
      </header>

      {message && <div className="dashboard-message">{message}</div>}
      {error && <div className="auth-error settings-error">{error}</div>}

      <div className="settings-grid">
        <section className="applications-section settings-card">
          <h2>Profile</h2>
          <form className="settings-form" onSubmit={handleSaveProfile}>
            <label>
              Name
              <input value={name} onChange={(e) => setName(e.target.value)} required minLength={2} />
            </label>
            <label>
              Email
              <input value={profile?.email || user?.email || ''} disabled />
            </label>
            <label>
              New Password
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Leave blank to keep current" minLength={8} />
            </label>
            <button type="submit" className="btn-primary btn-sm" disabled={loading === 'profile'}>
              {loading === 'profile' ? 'Saving…' : 'Save Changes'}
            </button>
          </form>
        </section>

        <section className="applications-section settings-card">
          <h2>Gmail Integration</h2>
          <div className="gmail-status">
            <span className={`status-dot ${gmailStatus?.connected ? 'connected' : 'disconnected'}`} />
            {gmailStatus?.connected
              ? `Connected${gmailStatus.email ? ` (${gmailStatus.email})` : ''}`
              : 'Not connected'}
          </div>
          <div className="settings-actions">
            <button type="button" className="btn-outline" onClick={handleConnectGmail} disabled={!!loading}>
              {gmailStatus?.connected ? 'Reconnect Gmail' : 'Connect Gmail'}
            </button>
            <button type="button" className="btn-outline" onClick={handleSync} disabled={!!loading || !gmailStatus?.connected}>
              {loading === 'sync' ? 'Syncing…' : 'Sync Emails'}
            </button>
            <button type="button" className="btn-primary btn-sm" onClick={handleExtract} disabled={!!loading || !gmailStatus?.connected}>
              {loading === 'extract' ? 'Extracting…' : 'Extract Jobs'}
            </button>
          </div>
        </section>

        <section className="applications-section settings-card danger-zone">
          <h2>Danger Zone</h2>
          <p className="empty-hint">Permanently delete your account and all data.</p>
          <button type="button" className="btn-danger btn-sm" onClick={handleDeleteAccount}>
            Delete Account
          </button>
        </section>
      </div>
    </>
  )
}
