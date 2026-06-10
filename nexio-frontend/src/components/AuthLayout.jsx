import { Link } from 'react-router-dom'
import './Auth.css'

export default function AuthLayout({ title, subtitle, children }) {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">
          <div className="logo">N</div>
          <h1>Nexio</h1>
          <p className="auth-tagline">Job Application Tracker</p>
        </div>
        <h2>{title}</h2>
        {subtitle && <p className="auth-subtitle">{subtitle}</p>}
        {children}
      </div>
    </div>
  )
}

export function AuthFooter({ text, linkText, linkTo }) {
  return (
    <p className="auth-footer">
      {text}{' '}
      <Link to={linkTo}>{linkText}</Link>
    </p>
  )
}
