import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../../api/apiClient';
import { useAuth } from '../../context/AuthContext';
import Pagination from '../../components/Pagination';
import './Data.css';

function DataInquiry({ initParams }) {
  const { loginId } = useAuth();
  const [isProcessing, setIsProcessing] = useState(false);

  const [inquiryParams, setInquiryParams] = useState({
    startMon: new Date().getFullYear() + '-01',
    endMon: new Date().getFullYear() + '-12',
    type: 'ALL',
    method: 'ALL'
  });

  const [dataList, setDataList] = useState([]);
  const [summary, setSummary] = useState(null);

  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
  const [updateTarget, setUpdateTarget] = useState(null);
  const [updateForm, setUpdateForm] = useState({ vatValue: 0, totalPrice: 0 });

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [receiptFile, setReceiptFile] = useState(null);
  const [isOcrProcessing, setIsOcrProcessing] = useState(false);
  const [ocrResult, setOcrResult] = useState(null);

  const fetchTaxData = useCallback(async (paramsToUse = inquiryParams) => {
    setIsProcessing(true);
    try {
      const params = { startMon: paramsToUse.startMon, endMon: paramsToUse.endMon };
      if (paramsToUse.type !== 'ALL') params.type = paramsToUse.type;
      if (paramsToUse.method !== 'ALL') params.method = paramsToUse.method;

      const response = await apiClient.get(`/data/${loginId}`, { params });
      setDataList(response.data.dataList);
      setSummary(response.data.summary);
      setCurrentPage(1);
    } catch (error) {
      toast.error('데이터를 불러오는데 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  }, [inquiryParams, loginId]);

  useEffect(() => {
    if (initParams) {
      const newParams = { ...inquiryParams, startMon: initParams.startMon, endMon: initParams.endMon };
      setInquiryParams(newParams);
      fetchTaxData(newParams);
    }
  }, [initParams, fetchTaxData]);

  const handleDelete = async (dataId) => {
    if (!window.confirm('정말 삭제하시겠습니까?')) return;
    setIsProcessing(true);
    try {
      await apiClient.delete(`/data/${loginId}/${dataId}`);
      toast.success('삭제되었습니다.');
      fetchTaxData();
    } catch (error) {
      toast.error(error.response?.data?.message || '삭제에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const openUpdateModal = (item) => {
    setUpdateTarget(item);
    setUpdateForm({ vatValue: item.vatValue, totalPrice: item.totalPrice });
    setIsUpdateModalOpen(true);
  };
  const closeUpdateModal = () => { setIsUpdateModalOpen(false); setUpdateTarget(null); };

  const handleUpdateSubmit = async (e) => {
    e.preventDefault();
    setIsProcessing(true);
    try {
      await apiClient.patch(`/data/${loginId}/${updateTarget.id}`, {
        vatValue: updateForm.vatValue,
        totalPrice: updateForm.totalPrice
      });
      toast.success('수정되었습니다.');
      closeUpdateModal();
      fetchTaxData();
    } catch (error) {
      toast.error(error.response?.data?.message || '수정에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const openCreateModal = () => { setIsCreateModalOpen(true); setReceiptFile(null); setOcrResult(null); };
  const closeCreateModal = () => { setIsCreateModalOpen(false); setReceiptFile(null); setOcrResult(null); };

  const handleExtractText = async () => {
    if (!receiptFile) return toast.warning("영수증 사진을 먼저 첨부해주세요.");

    setIsOcrProcessing(true);
    try {
      const formData = new FormData();
      formData.append('file', receiptFile);
      const response = await apiClient.post(`/data/extract/text/${loginId}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setOcrResult(response.data);
      toast.success('텍스트 추출 성공!');
    } catch (error) {
      toast.error(error.response?.data?.message || '텍스트 추출에 실패했습니다.');
    } finally {
      setIsOcrProcessing(false);
    }
  };

  const handleCreateData = async () => {
    if (!ocrResult.transDt) return toast.warning("결제 일시를 입력해주세요.");

    setIsProcessing(true);
    try {
      await apiClient.post(`/data/${loginId}`, {
        vendorId: ocrResult.vendorId,
        transDt: ocrResult.transDt,
        totalPrice: ocrResult.totalPrice,
        vatValue: ocrResult.vatValue
      });
      toast.success('세무 데이터가 성공적으로 추가되었습니다.');
      closeCreateModal();
      fetchTaxData();
    } catch (error) {
      toast.error(error.response?.data?.message || '데이터 추가에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems = dataList.slice(indexOfFirstItem, indexOfLastItem);

  const getTypeLabel = (type) => ({ SALES: '매출', PURCHASE: '매입' }[type] || type);
  const getMethodLabel = (method) => ({ INVOICE: '계산서', CARD: '카드', RECEIPT: '현금영수증', CASH: '현금' }[method] || method);

  return (
    <div>
      <div className="filter-bar">
        <div className="filter-group">
          <input type="month" value={inquiryParams.startMon} onChange={(e) => setInquiryParams({...inquiryParams, startMon: e.target.value})} />
          <span>~</span>
          <input type="month" value={inquiryParams.endMon} onChange={(e) => setInquiryParams({...inquiryParams, endMon: e.target.value})} />
        </div>
        <div className="filter-group">
          <select value={inquiryParams.type} onChange={(e) => setInquiryParams({...inquiryParams, type: e.target.value})}>
            <option value="ALL">구분 (전체)</option>
            <option value="SALES">매출</option>
            <option value="PURCHASE">매입</option>
          </select>
        </div>
        <div className="filter-group">
          <select value={inquiryParams.method} onChange={(e) => setInquiryParams({...inquiryParams, method: e.target.value})}>
            <option value="ALL">유형 (전체)</option>
            <option value="INVOICE">계산서</option>
            <option value="CARD">카드</option>
            <option value="RECEIPT">현금영수증</option>
            <option value="CASH">현금</option>
          </select>
        </div>
        <div className="filter-actions">
          <button className="btn primary" onClick={() => fetchTaxData(inquiryParams)} disabled={isProcessing}>
            {isProcessing ? '처리 중...' : '조회'}
          </button>
          <button className="btn secondary" onClick={openCreateModal}>추가</button>
        </div>
      </div>

      <div className="summary-box">
        <p>조회 결과 (총 <strong>{summary ? summary.count : dataList.length}건)</strong></p>
        <div className="summary-values">
          <span>공급가액: {summary ? summary.totalNetValue.toLocaleString() + '원' : '-'}</span>
          <span>부가세: {summary ? summary.totalVatValue.toLocaleString() + '원' : '-'}</span>
          <span className="total-price">합계: {summary ? summary.totalPrice.toLocaleString() + '원' : '-'}</span>
        </div>
      </div>

      <div className="data-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>월일</th>
              <th>구분</th>
              <th>유형</th>
              <th>공급가액</th>
              <th>부가세</th>
              <th>합계</th>
              <th>공급처</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            {currentItems.length > 0 ? (
              currentItems.map((item) => (
                <tr key={item.id}>
                  <td>{item.transDt.substring(5)}</td>
                  <td>{getTypeLabel(item.type)}</td>
                  <td>{getMethodLabel(item.method)}</td>
                  <td>{item.netValue.toLocaleString()}원</td>
                  <td>{item.vatValue.toLocaleString()}원</td>
                  <td>{item.totalPrice.toLocaleString()}원</td>
                  <td>{item.vendorId}</td>
                  <td className="action-cell">
                    <button className="small-btn edit" disabled={!item.mod} onClick={() => openUpdateModal(item)}>수정</button>
                    <button className="small-btn delete" disabled={!item.mod} onClick={() => handleDelete(item.id)}>삭제</button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="8" className="empty-msg">조회된 세무 데이터가 없습니다.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Pagination
        totalItems={dataList.length}
        itemsPerPage={itemsPerPage}
        currentPage={currentPage}
        onPageChange={setCurrentPage}
      />

      {isUpdateModalOpen && updateTarget && (
        <div className="modal-overlay" onClick={closeUpdateModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <button className="close-btn" onClick={closeUpdateModal}>&times;</button>
            </div>
            <div className="modal-body">
              <div className="info-row"><span>일자:</span> <strong>{updateTarget.transDt}</strong></div>
              <div className="info-row"><span>구분:</span> <strong>{getTypeLabel(updateTarget.type)} ({getMethodLabel(updateTarget.method)})</strong></div>
              <div className="info-row"><span>공급처:</span> <strong>{updateTarget.vendorId}</strong></div>
              <hr />
              <form onSubmit={handleUpdateSubmit} className="modal-update-form">
                <div className="form-group">
                  <label>부가세 (원)</label>
                  <input type="number" value={updateForm.vatValue} onChange={(e) => setUpdateForm({...updateForm, vatValue: e.target.value})} required />
                </div>
                <div className="form-group">
                  <label>합계 (원)</label>
                  <input type="number" value={updateForm.totalPrice} onChange={(e) => setUpdateForm({...updateForm, totalPrice: e.target.value})} required />
                </div>
                <button type="submit" className="btn primary" disabled={isProcessing}>
                  {isProcessing ? '처리 중...' : '수정 완료'}
                </button>
              </form>
            </div>
          </div>
        </div>
      )}

      {isCreateModalOpen && (
        <div className="modal-overlay" onClick={closeCreateModal}>
          <div className="modal-content receipt-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <button className="close-btn" onClick={closeCreateModal}>&times;</button>
            </div>
            <div className="modal-body">
              <div className="ocr-upload-group">
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => setReceiptFile(e.target.files[0])}
                />
                <button className="btn secondary" onClick={handleExtractText} disabled={isOcrProcessing}>
                  {isOcrProcessing ? '처리 중...' : '텍스트 추출'}
                </button>
              </div>

              {ocrResult && (
                <div className="receipt-preview-container">
                  <div className="receipt-paper">
                    <h4 className="receipt-title">영 수 증</h4>
                    <div className="receipt-divider">========================================</div>

                    <div className="receipt-row">
                      <span>결제일시</span>
                      <input
                        type="date"
                        className="receipt-input"
                        value={ocrResult.transDt}
                        onChange={(e) => setOcrResult({...ocrResult, transDt: e.target.value})}
                      />
                    </div>

                    <div className="receipt-divider">----------------------------------------</div>

                    <div className="receipt-row">
                      <span>사업자번호</span>
                      <input
                        type="text"
                        className="receipt-input"
                        value={ocrResult.vendorId}
                        onChange={(e) => setOcrResult({...ocrResult, vendorId: e.target.value})}
                      />
                    </div>

                    <div className="receipt-row">
                      <span>부가세</span>
                      <input
                        type="number"
                        className="receipt-input"
                        value={ocrResult.vatValue}
                        onChange={(e) => setOcrResult({...ocrResult, vatValue: Number(e.target.value)})}
                      />
                    </div>

                    <div className="receipt-row">
                      <span>합계금액</span>
                      <input
                        type="number"
                        className="receipt-input"
                        value={ocrResult.totalPrice}
                        onChange={(e) => setOcrResult({...ocrResult, totalPrice: Number(e.target.value)})}
                      />
                    </div>

                    <div className="receipt-divider">========================================</div>
                  </div>

                  <button className="btn primary" onClick={handleCreateData} disabled={isProcessing}>
                    {isProcessing ? '처리 중...' : '등록'}
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default DataInquiry;