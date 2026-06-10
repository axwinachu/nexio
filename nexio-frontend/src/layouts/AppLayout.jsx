import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import Sidebar from '../components/Sidebar'
import '../pages/Dashboard.css'

export default function AppLayout() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="dashboard-layout">
      <Sidebar onLogout={handleLogout} />
      <main className="dashboard-main">
        <Outlet />
      </main>
    </div>
  )
}
