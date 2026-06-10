import { NavLink } from 'react-router-dom'

const NAV_ITEMS = [
  { label: 'Dashboard', icon: '◫', path: '/dashboard' },
  { label: 'Applications', icon: '☰', path: '/applications' },
  { label: 'Analytics', icon: '◔', path: '/analytics' },
  { label: 'Calendar', icon: '▦', path: '/calendar' },
  { label: 'Settings', icon: '⚙', path: '/settings' },
]

export default function Sidebar({ onLogout }) {
  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="logo">N</div>
        <span>Nexio</span>
      </div>

      <nav className="sidebar-nav">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
          >
            <span className="nav-icon">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>

      <button type="button" className="nav-item logout-btn" onClick={onLogout}>
        <span className="nav-icon">↩</span>
        Log out
      </button>
    </aside>
  )
}
