import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

function Navbar() {
  const navigate = useNavigate();
  const { isLoggedIn, logout } = useAuth();

  const handleLogout = () => {
      logout();
      toast.info("로그아웃 되었습니다.");
      navigate('/');
  };

  const handleProtectedMenuClick = (e) => {
    if (!isLoggedIn) {
      e.preventDefault();
      toast.warning("로그인이 필요한 서비스입니다.");
      navigate('/login');
    }
  };

  const handleToggleSidebar = () => {
    window.dispatchEvent(new Event('toggleSidebar'));
  };

  return (
    <nav className="navbar">
      <div className="nav-logo">
              <Link to="/">BizReport</Link>
      </div>

      <div className="nav-menu">
          <Link to="/business" onClick={handleProtectedMenuClick}>Business</Link>
          <Link to="/data" onClick={handleProtectedMenuClick}>Data</Link>
          <Link to="/report" onClick={handleProtectedMenuClick}>Report</Link>
      </div>

      <div className="nav-right">
        {!isLoggedIn ? (
          <button className="btn primary" onClick={() => navigate('/login')}>
            로그인
          </button>
        ) : (
          <>
            <button className="sidebar-btn" onClick={handleToggleSidebar}>
              ☰
            </button>
            <button className="btn secondary" onClick={handleLogout}>
              로그아웃
            </button>
          </>
        )}
      </div>
    </nav>
  );
}

export default Navbar;