import React, { useState } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../../api/apiClient';
import { useAuth } from '../../context/AuthContext';
import Pagination from '../../components/Pagination';
import './Data.css';

function DataScraping() {
  const { loginId } = useAuth();
  const [isProcessing, setIsProcessing] = useState(false);

  const [scrapingParams, setScrapingParams] = useState({
    startMon: new Date().getFullYear() + '-01',
    endMon: new Date().getFullYear() + '-12',
    type: 'ALL'
  });
  const [inputCount, setInputCount] = useState('');

  const [generatedDataList, setGeneratedDataList] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const handleGenerateMock = async () => {
    setIsProcessing(true);
    try {
      const countToGenerate = inputCount && !isNaN(inputCount)
        ? parseInt(inputCount, 10)
        : Math.floor(Math.random() * 91) + 10;

      if (countToGenerate <= 0) {
        toast.warning("생성할 데이터 건수는 1건 이상이어야 합니다.");
        setIsProcessing(false);
        return;
      }

      const payload = {
        startMon: scrapingParams.startMon,
        endMon: scrapingParams.endMon,
        count: countToGenerate
      };
      if (scrapingParams.type !== 'ALL') {
        payload.type = scrapingParams.type;
      }

      const response = await apiClient.post(`/data/generate/mock/${loginId}`, payload);

      const newList = (response.data || []).sort((a, b) => b.id - a.id);
      setGeneratedDataList(newList);
      setCurrentPage(1);

      toast.success(`가상 세무 데이터 ${newList.length}건 불러오기 완료!`);
      setInputCount('');

    } catch (error) {
      toast.error(error.response?.data?.message || '스크래핑에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const getTypeLabel = (type) => ({ SALES: '매출', PURCHASE: '매입' }[type] || type);
  const getMethodLabel = (method) => ({ INVOICE: '계산서', CARD: '카드', RECEIPT: '현금영수증', CASH: '현금' }[method] || method);

  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems = generatedDataList.slice(indexOfFirstItem, indexOfLastItem);

  return (
    <div>
      <div className="filter-bar">
        <div className="filter-group">
          <select
            value={scrapingParams.type}
            onChange={(e) => setScrapingParams({...scrapingParams, type: e.target.value})}
          >
            <option value="ALL">구분 (전체)</option>
            <option value="SALES">매출</option>
            <option value="PURCHASE">매입</option>
          </select>
        </div>

        <div className="filter-group">
          <input
            type="month"
            value={scrapingParams.startMon}
            onChange={(e) => setScrapingParams({...scrapingParams, startMon: e.target.value})}
          />
          <span>~</span>
          <input
            type="month"
            value={scrapingParams.endMon}
            onChange={(e) => setScrapingParams({...scrapingParams, endMon: e.target.value})}
          />
        </div>

        <div className="filter-group">
          <input
            type="number"
            placeholder="스크래핑 건수"
            value={inputCount}
            onChange={(e) => setInputCount(e.target.value)}
            min="1"
          />
        </div>

        <button className="btn primary" onClick={handleGenerateMock} disabled={isProcessing}>
          {isProcessing ? '처리 중...' : '불러오기'}
        </button>
      </div>

      <div className="data-container">
        <div className="summary-box">
          <p>스크래핑 결과 (총 <strong>{generatedDataList.length}</strong>건)</p>
        </div>

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
            </tr>
          </thead>
          <tbody>
            {currentItems.length > 0 ? (
              currentItems.map((item) => (
                <tr key={item.id}>
                  <td>{item.transDt ? item.transDt.substring(5) : '-'}</td>
                  <td>{getTypeLabel(item.type)}</td>
                  <td>{getMethodLabel(item.method)}</td>
                  <td>{item.netValue?.toLocaleString()}원</td>
                  <td>{item.vatValue?.toLocaleString()}원</td>
                  <td>{item.totalPrice?.toLocaleString()}원</td>
                  <td>{item.vendorId}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="7" className="empty-msg">불러온 세무 데이터가 없습니다.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Pagination
        totalItems={generatedDataList.length}
        itemsPerPage={itemsPerPage}
        currentPage={currentPage}
        onPageChange={setCurrentPage}
      />
    </div>
  );
}

export default DataScraping;