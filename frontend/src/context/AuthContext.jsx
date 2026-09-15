import React, { createContext, useState, useEffect, useContext } from 'react';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [loginId, setLoginId] = useState(null);

  useEffect(() => {
    const userId = localStorage.getItem('bizUserId');
    if (userId) {
      setIsLoggedIn(true);
      setLoginId(userId);
    }
  }, []);

  const login = (bizId) => {
    localStorage.setItem('bizUserId', bizId);
    setIsLoggedIn(true);
    setLoginId(bizId);
  };

  const logout = () => {
    localStorage.removeItem('bizUserId');
    setIsLoggedIn(false);
    setLoginId(null);
  };

  const isAdmin = loginId === 'admin';

  return (
    <AuthContext.Provider value={{ isLoggedIn, loginId, isAdmin, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};