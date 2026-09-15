import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import apiClient from '../../api/apiClient';
import { useAuth } from '../../context/AuthContext';
import './Login.css';

function Login() {
  const [loginId, setLoginId] = useState('');
  const [registerId, setRegisterId] = useState('');
  const [isRegistering, setIsRegistering] = useState(false);
  const navigate = useNavigate();

  const { login } = useAuth();

  const validateBizId = (id) => {
    if (id === 'admin') return true;
    const regex = /^\d{10}$/;
    return regex.test(id);
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    const cleanId = loginId.trim();

    if (!validateBizId(cleanId)) {
      return toast.warning("사업자등록번호는 숫자 10자리여야 합니다.");
    }

    try {
      if (cleanId === 'admin') {
          login(cleanId);
          toast.success("관리자 페이지로 이동합니다.");
          navigate('/admin');
          return;
      }

      await apiClient.get(`/business/check/${cleanId}`);
      login(cleanId);
      toast.success(`[${cleanId}] 로그인 되었습니다.`);
      navigate('/');

    } catch (error) {
      const errMsg = error.response?.data?.message || "등록되지 않은 사업자입니다.";
      toast.error(errMsg);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    const cleanId = registerId.trim();

    if (!validateBizId(cleanId)) {
      return toast.warning("사업자등록번호 10자리를 정확히 입력해주세요.");
    }

    setIsRegistering(true);

    try {
      await apiClient.post('/business', { id: cleanId, indCd: "미분류" });

      toast.success(`[${cleanId}] 사업자 등록이 완료되었습니다.`);
      setRegisterId('');

    } catch (error) {
      const errMsg = error.response?.data?.message || "사업자 등록에 실패했습니다.";
      toast.error(errMsg);

    } finally {
      setIsRegistering(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">

        <div className="form-section">
          <h2>로그인</h2>
          <p className="section-desc">등록된 사업자 번호로 로그인하세요.</p>
          <form onSubmit={handleLogin}>
            <input
              type="text"
              placeholder="사업자등록번호 ( - 제외)"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
            />
            <button type="submit" className="btn primary">로그인</button>
          </form>
          <p className="test-guide">
              * test ID1 (일반과세자) : <strong>1234567890</strong> <br />
              * test ID2 (간이과세자) : <strong>1234567891</strong> <br />
              * admin ID : <strong>admin</strong> <br />
          </p>
        </div>

        <div className="divider"></div>

        <div className="form-section">
          <h2>신규 등록</h2>
          <p className="section-desc">처음이신가요? 사업자를 등록해주세요.</p>
          <form onSubmit={handleRegister}>
            <input
              type="text"
              placeholder="사업자등록번호 ( - 제외)"
              value={registerId}
              onChange={(e) => setRegisterId(e.target.value)}
              disabled={isRegistering}
            />
            <button
              type="submit"
              className="btn secondary"
              disabled={isRegistering}
            >
              {isRegistering ? '처리 중...' : '사업자 등록'}
            </button>
          </form>
        </div>

      </div>
    </div>
  );
}

export default Login;