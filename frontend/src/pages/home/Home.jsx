import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useAuth } from '../../context/AuthContext';
import './Home.css';

function Home() {
  const { isLoggedIn } = useAuth();
  const navigate = useNavigate();

  const handleProtectedClick = (e) => {
      if (!isLoggedIn) {
        e.preventDefault();
        toast.warning("로그인이 필요한 서비스입니다.");
        navigate('/login');
      }
  };

  return (
    <div className="home-container">
      <section className="hero-section">
        <div className="hero-content">
          <h1 className="hero-title">
            소상공인 맞춤형 <br />
            <span className="highlight">세금 예측 리포트</span>
          </h1>
          <p className="hero-subtitle">
            실시간 예상 부가세와 종합소득세를 한눈에 파악하세요. <br/>
            복잡한 세무 지식 없이도 현금흐름을 안전하게 관리할 수 있습니다.
          </p>

          <div className="hero-buttons">
            <Link
              to={isLoggedIn ? "/business" : "/login"}
              className="cta-button primary"
            >
              {isLoggedIn ? "내 사업자 수정하기" : "내 사업자 등록하기"}
            </Link>

            <Link
              to="/report"
              className="cta-button secondary"
              onClick={handleProtectedClick}
            >
              리포트 미리보기
            </Link>
          </div>

        </div>
      </section>
    </div>
  );
}

export default Home;