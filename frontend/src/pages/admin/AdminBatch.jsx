import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../../api/apiClient';
import Pagination from '../../components/Pagination';
import ReportCreateModal from './EndMonthModal';

function AdminBatch() {
  const [batchList, setBatchList] = useState([]);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const [manualBatchList, setManualBatchList] = useState([]);
  const [manualStatusFilter, setManualStatusFilter] = useState('ALL');
  const [manualCurrentPage, setManualCurrentPage] = useState(1);
  const manualItemsPerPage = 5;

  const [isReportModalOpen, setIsReportModalOpen] = useState(false);

const fetchAllBatches = useCallback(async () => {
    try {
      const response = await apiClient.get('/batch/queue');
      let data = response.data.sort((a, b) => b.id - a.id);

      if (statusFilter !== 'ALL') {
        data = data.filter(b => b.status === statusFilter);
      }
      setBatchList(data);
    } catch (error) {
      console.error("배치 목록 조회 실패");
    }
  }, [statusFilter]);

  useEffect(() => {
    fetchAllBatches();
  }, [fetchAllBatches]);

  const handleReapQueue = async () => {
    if (!window.confirm("좀비 큐를 READY 상태로 롤백하시겠습니까?")) return;
    try {
      const res = await apiClient.post('/batch/queue/reap');
      toast.success(res.data);
      fetchAllBatches();
    } catch (error) {
      toast.error("좀비 큐 처리에 실패했습니다.");
    }
  };

  const handleManualJob = async (jobEndpoint, jobName) => {
    if (!window.confirm(`[${jobName}] 배치를 수동으로 즉시 실행하시겠습니까? 시스템 부하가 발생할 수 있습니다.`)) return;
    try {
      const res = await apiClient.post(jobEndpoint);
      toast.success(res.data);
      fetchAllBatches();
    } catch (error) {
      toast.error(`${jobName} 실행 실패`);
    }
  };

  const handleReportCreateConfirm = async (endMon) => {
    if (!window.confirm(`[${endMon}] 기준 전체 리포트 생성 배치를 실행하시겠습니까?`)) return;
    try {
      const res = await apiClient.post(`/batch/job/reportCreateJob?endMon=${endMon}`);
      toast.success(res.data);
      setIsReportModalOpen(false);
      fetchAllBatches();
    } catch (error) {
      toast.error("리포트 생성 배치 실행 실패");
    }
  };

  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems = batchList.slice(indexOfFirstItem, indexOfLastItem);

  const manualIndexOfLastItem = manualCurrentPage * manualItemsPerPage;
  const manualIndexOfFirstItem = manualIndexOfLastItem - manualItemsPerPage;
  const currentManualItems = manualBatchList.slice(manualIndexOfFirstItem, manualIndexOfLastItem);

  return (
    <div className="admin-section">
      <div className="admin-card danger-card">
        <div className="admin-card-header-flex">
          <div>
            <h3>1. 배치 수동 트리거 및 장애 복구</h3>
            <p className="admin-desc">스케줄러와 무관하게 시스템 배치를 강제 실행합니다.</p>
          </div>
          <button className="btn delete" onClick={handleReapQueue}>
            장애 복구
          </button>
        </div>

        <div className="manual-trigger-grid">
          <button className="btn secondary" onClick={() => handleManualJob('/batch/job/statusUpdateJob', '국세청 상태 동기화')}>
            국세청 상태 동기화
          </button>
          <button className="btn secondary" onClick={() => handleManualJob('/batch/job/statusClosedJob', '폐업 사업자 마감')}>
            폐업 사업자 마감
          </button>
          <button className="btn secondary" onClick={() => handleManualJob('/batch/job/dataClosedJob', '세무 데이터 잠금')}>
            세무 데이터 마감
          </button>
          <button className="btn secondary" onClick={() => setIsReportModalOpen(true)}>
            리포트 생성
          </button>
        </div>
      </div>

      <ReportCreateModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        onConfirm={handleReportCreateConfirm}
      />

        <div className="admin-card">
          <div className="admin-card-header-flex">
            <h3>2. 자동 배치 이력</h3>
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value); setCurrentPage(1); }}
              className="admin-select"
            >
              <option value="ALL">전체</option>
              <option value="READY">READY</option>
              <option value="PROCESSING">PROCESSING</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="FAILED">FAILED</option>
            </select>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>작업명</th>
                <th>상태</th>
                <th>일시</th>
              </tr>
            </thead>
            <tbody>
              {currentItems.length > 0 ? currentItems.map(batch => (
                <tr key={batch.id}>
                  <td>{batch.id}</td>
                  <td className="truncate-text" title={batch.fileName}>{batch.fileName || batch.jobName}</td>
                  <td>
                    <span className={`status-badge status-${batch.status.toLowerCase()}`}>
                      {batch.status}
                    </span>
                  </td>
                  <td>
                    {batch.createdAt ? batch.createdAt.substring(2, 16).replace('T', ' ') : '-'}
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan="4" className="empty-msg">내역이 없습니다.</td>
                </tr>
              )}
            </tbody>
          </table>

          <Pagination
            totalItems={batchList.length}
            itemsPerPage={itemsPerPage}
            currentPage={currentPage}
            onPageChange={setCurrentPage}
          />
        </div>
    </div>
  );
}

export default AdminBatch;