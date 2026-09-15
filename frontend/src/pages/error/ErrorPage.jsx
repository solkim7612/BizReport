import React from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import './ErrorPage.css';

function ErrorPage() {
  const { code } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const isAdminArea = location.pathname.startsWith('/admin');
  const homePath = isAdminArea ? '/admin' : '/';
  const homeLabel = isAdminArea ? '대시보드로 돌아가기' : '홈으로 돌아가기';

  let title = "오류가 발생했습니다";
  let description = "요청을 처리하는 중에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";

  if (code === '500') {
    title = "500 - 서버 내부 오류";
    description = "서버에서 예상치 못한 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
  } else if (code === '401') {
    title = "401 - 인증 필요";
    description = "로그인이 필요한 서비스이거나 인증이 만료되었습니다.";
  } else if (code === '403') {
    title = "403 - 접근 권한 없음";
    description = "해당 페이지에 접근할 수 있는 권한이 없습니다.";
  } else if (code === '404') {
    title = "404 - 페이지를 찾을 수 없습니다";
    description = "요청하신 페이지가 존재하지 않거나, 주소가 잘못 입력되었습니다.";
  }

  const customMessage = location.state?.message;
  console.log("전달받은 state 메시지:", customMessage);
  const finalDescription = customMessage || description;

  return (
    <div className="error-container">
      <div className="error-box">
        <h1 className="error-title">{title}</h1>
        <p className="error-desc">{finalDescription}</p>

        <div className="error-actions">
          <button className="btn secondary" onClick={() => navigate(-1)}>
            이전 페이지로
          </button>

          <button className="btn primary" onClick={() => navigate(homePath, { replace: true })}>
            {homeLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ErrorPage;