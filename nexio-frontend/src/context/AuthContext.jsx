import { useMemo, useState } from 'react'
import { clearAuth, getToken } from '../api/client'
import { AuthContext } from './auth-context'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = getToken()
    if (!token) return null
    return {
      token,
      userId: localStorage.getItem('userId'),
      name: localStorage.getItem('name'),
      email: localStorage.getItem('email'),
    }
  })

  const loginUser = (data) => {
    setUser({
      token: data.token,
      userId: data.userId,
      name: data.name,
      email: data.email,
    })
  }

  const logout = () => {
    clearAuth()
    setUser(null)
  }

  const value = useMemo(
    () => ({ user, loginUser, logout, isAuthenticated: !!user }),
    [user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
