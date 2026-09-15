import React, { useState } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../../api/apiClient';
import { useAuth } from '../../context/AuthContext';
import './Data.css';

function DataUpload({ onQueueUpdated }) {
  const { loginId } = useAuth();
  const [isProcessing, setIsProcessing] = useState(false);
  const [uploadParams, setUploadParams] = useState({
    cardNum: '',
    startMon: new Date().getFullYear() + '-01',
    endMon: new Date().getFullYear() + '-12',
    file: null
  });

  const handleDownloadFormat = async () => {
    try {
      const response = await apiClient.get('/data/download/format', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'format.csv');
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
    } catch (error) {
      toast.error('서식 다운로드에 실패했습니다.');
    }
  };

  const handleUploadCard = async (e) => {
    e.preventDefault();
    if (!uploadParams.file) return toast.warning('업로드할 파일을 선택해주세요.');
    if (!uploadParams.cardNum.trim()) return toast.warning('카드 번호를 입력해주세요.');

    setIsProcessing(true);
    try {
      const formData = new FormData();
      formData.append('cardNum', uploadParams.cardNum);
      formData.append('startMon', uploadParams.startMon);
      formData.append('endMon', uploadParams.endMon);
      formData.append('file', uploadParams.file);

      const response = await apiClient.post(`/data/upload/card/${loginId}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      toast.success(response.data || '업로드 대기열에 등록되었습니다.');
      setUploadParams({ ...uploadParams, file: null, cardNum: '' });
      document.getElementById('file-upload-input').value = '';

      if (onQueueUpdated) onQueueUpdated();

    } catch (error) {
      toast.error(error.response?.data?.message || '업로드에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="upload-section">
      <form className="upload-form" onSubmit={handleUploadCard}>
        <div className="form-group">
          <label>카드 번호</label>
          <input
            type="text"
            placeholder="카드번호 입력 ( - 포함 가능)"
            value={uploadParams.cardNum}
            onChange={(e) => setUploadParams({...uploadParams, cardNum: e.target.value})}
            required
          />
        </div>

        <div className="form-group">
          <label>대상 기간</label>
          <div className="date-range">
            <input
              type="month"
              value={uploadParams.startMon}
              onChange={(e) => setUploadParams({...uploadParams, startMon: e.target.value})}
              required
            />
            <span>~</span>
            <input
              type="month"
              value={uploadParams.endMon}
              onChange={(e) => setUploadParams({...uploadParams, endMon: e.target.value})}
              required
            />
          </div>
        </div>

        <div className="form-group">
          <label>파일 선택</label>
          <input
            id="file-upload-input"
            type="file"
            accept=".csv"
            onChange={(e) => setUploadParams({...uploadParams, file: e.target.files[0]})}
            required
          />
        </div>

        <div className="button-group-data">
          <button type="button" className="btn secondary" onClick={handleDownloadFormat}>
            엑셀서식 다운받기
          </button>
          <button type="submit" className="btn primary" disabled={isProcessing}>
            {isProcessing ? '처리 중...' : '엑셀서식 업로드'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default DataUpload;