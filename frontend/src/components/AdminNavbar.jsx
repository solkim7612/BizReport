import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

function AdminNavbar() {
  const navigate = useNavigate();
  const { isLoggedIn, logout } = useAuth();

  const handleLogout = () => {
      logout();
      toast.info("관리자 로그아웃 되었습니다.");
      navigate('/');
  };

  return (
    <nav className="navbar admin-navbar">
      <div className="nav-logo">
          <Link to="/admin">BizReport <span style={{ fontSize: '0.8rem', color: '#ef4444', marginLeft: '0.5rem' }}>ADMIN</span></Link>
      </div>

      <div className="nav-right">
          {!isLoggedIn ? (
            <button className="auth-btn login-btn" onClick={() => navigate('/login')}>
              로그인
            </button>
          ) : (
            <button className="btn secondary" onClick={handleLogout}>
              로그아웃
            </button>
          )}
      </div>
    </nav>
  );
}

export default AdminNavbar;