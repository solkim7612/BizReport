import React, { useState } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../../api/apiClient';

function AdminTaxRate() {
  const [file, setFile] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const handleUploadRate = async (e) => {
    e.preventDefault();
    if (!file) return toast.warning("업로드할 CSV 파일을 선택해주세요.");

    setIsProcessing(true);
    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await apiClient.post('/business/upload/rate', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      toast.success(response.data || "세율 데이터가 업로드되었습니다.");
      setFile(null);
      document.getElementById('rate-file-input').value = '';
    } catch (error) {
      toast.error(error.response?.data?.message || "세율 파일 업로드에 실패했습니다.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleClearCache = async () => {
    if (!window.confirm("메모리에 캐시된 세율 데이터를 모두 초기화하시겠습니까?")) return;
    try {
      const res = await apiClient.post('/batch/cache');
      toast.success(res.data);
    } catch (error) {
      toast.error("캐시 초기화에 실패했습니다.");
    }
  };

  const handleDeleteOldRate = async () => {
    if (!window.confirm("5년이 지난 과거 세율 데이터를 DB에서 완전히 삭제하시겠습니까? (복구 불가)")) return;
    try {
      const res = await apiClient.post('/batch/job/rateDeleteJob');
      toast.success(res.data);
    } catch (error) {
      toast.error("과거 데이터 삭제에 실패했습니다.");
    }
  };

  return (
    <div className="admin-section">
      <div className="admin-card">
        <h3>1. 신규 세율 업로드</h3>
        <p className="admin-desc">국세청 기준 최신 세율 CSV 파일을 시스템에 적재합니다.</p>
        <form className="admin-upload-form" onSubmit={handleUploadRate}>
          <input
            id="rate-file-input"
            type="file"
            accept=".csv"
            onChange={(e) => setFile(e.target.files[0])}
          />
          <button type="submit" className="btn primary" disabled={isProcessing}>
            {isProcessing ? '처리 중...' : '업로드'}
          </button>
        </form>
      </div>

      <div className="admin-card-grid">
        <div className="admin-card warning-card">
          <h3>2. 세율 캐시 초기화</h3>
          <p className="admin-desc">업로드된 신규 세율이 즉시 반영되도록 인메모리 캐시를 비웁니다.</p>
          <button className="btn secondary" onClick={handleClearCache}>
            초기화
          </button>
        </div>

        <div className="admin-card danger-card">
          <h3>3. 과거 세율 정리</h3>
          <p className="admin-desc">DB 용량 확보를 위해 5년 이상 경과된 지난 데이터를 일괄 삭제합니다.</p>
          <button className="btn delete" onClick={handleDeleteOldRate}>
            삭제
          </button>
        </div>
      </div>
    </div>
  );
}

export default AdminTaxRate;