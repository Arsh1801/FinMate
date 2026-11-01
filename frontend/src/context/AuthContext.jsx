import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';
import axiosClient from '../api/axiosClient';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('finmate_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('finmate_access_token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // If token exists, fetch current user profile to verify validity
    const checkAuth = async () => {
      const storedToken = localStorage.getItem('finmate_access_token');
      if (storedToken) {
        try {
          const resp = await axiosClient.get('/users/me');
          setUser(resp.data);
          localStorage.setItem('finmate_user', JSON.stringify(resp.data));
        } catch (err) {
          console.warn('Initial session check failed:', err.message);
        }
      }
      setLoading(false);
    };

    checkAuth();
  }, []);

  const login = async (email, password) => {
    const resp = await authApi.login({ email, password });
    const { accessToken, refreshToken, user: authUser } = resp.data;

    localStorage.setItem('finmate_access_token', accessToken);
    localStorage.setItem('finmate_refresh_token', refreshToken);
    localStorage.setItem('finmate_user', JSON.stringify(authUser));

    setToken(accessToken);
    setUser(authUser);
    return authUser;
  };

  const register = async data => {
    await authApi.register(data);
    return await login(data.email, data.password);
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      // Ignore network errors on logout
    } finally {
      localStorage.removeItem('finmate_access_token');
      localStorage.removeItem('finmate_refresh_token');
      localStorage.removeItem('finmate_user');
      setToken(null);
      setUser(null);
      window.location.href = '/login';
    }
  };

  const updateUser = updatedUser => {
    setUser(updatedUser);
    localStorage.setItem('finmate_user', JSON.stringify(updatedUser));
  };

  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        isAuthenticated: !!token && !!user,
        isAdmin,
        login,
        register,
        logout,
        updateUser
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
