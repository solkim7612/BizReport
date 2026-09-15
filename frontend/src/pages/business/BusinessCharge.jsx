import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../../api/apiClient';
import { useAuth } from '../../context/AuthContext';
import Pagination from '../../components/Pagination';
import './Business.css';

function BusinessCharge() {
  const { loginId } = useAuth();
  const [isCharging, setIsCharging] = useState(false);

  const [history, setHistory] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const fetchHistory = useCallback(async () => {
    if (!loginId) return;
    try {
      const res = await apiClient.get(`/business/history/refresh/${loginId}`);
      setHistory(res.data.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)));
    } catch (e) {
      toast.error("갱신권 내역을 불러오는데 실패했습니다.");
    }
  }, [loginId]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);


  const handleCharge = async (count) => {
    if (!window.confirm(`갱신권 ${count}회를 충전하시겠습니까?`)) return;

    setIsCharging(true);
    try {
      await apiClient.patch(`/business/charge/refresh?id=${loginId}&count=${count}`);
      toast.success(`${count}회 충전이 완료되었습니다.`);
      fetchHistory();
    } catch (err) {
      toast.error("충전에 실패했습니다.");
    } finally {
      setIsCharging(false);
    }
  };

  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems = history.slice(indexOfFirstItem, indexOfLastItem);

  return (
    <div>
      <div className="charge-box">
        <div className="charge-options">
          {[1, 5, 10].map(c => (
            <button key={c} className="charge-btn" onClick={() => handleCharge(c)} disabled={isCharging}>
              {c}회 충전
            </button>
          ))}
        </div>
      </div>

      <div className="sidebar-box">
        <table className="history-table">
          <thead>
            <tr>
              <th>구분</th>
              <th>잔여</th>
              <th>일시</th>
            </tr>
          </thead>
          <tbody>
            {currentItems.length > 0 ? (
              currentItems.map((h, i) => (
                <tr key={i}>
                  <td>
                    <span className={h.type === 'CHARGE' ? 'text-blue' : 'text-red'}>
                      {h.type === 'CHARGE' ? '충전' : '사용'} {h.amount}회
                    </span>
                  </td>
                  <td className="history-balance-td">
                    {h.balance}회
                  </td>
                  <td className="history-date-td">
                    {h.createdAt ? h.createdAt.substring(2, 16).replace('T', ' ') : ''}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="3" className="empty-msg">내역이 없습니다.</td>
              </tr>
            )}
          </tbody>
        </table>

        <Pagination
          totalItems={history.length}
          itemsPerPage={itemsPerPage}
          currentPage={currentPage}
          onPageChange={setCurrentPage}
        />
      </div>
    </div>
  );
}
export default BusinessCharge;