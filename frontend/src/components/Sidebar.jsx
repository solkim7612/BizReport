import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import apiClient from '../api/apiClient';
import { useAuth } from '../context/AuthContext';
import Pagination from './Pagination';
import './Sidebar.css';

function Sidebar() {
  const { loginId } = useAuth();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const [batchList, setBatchList] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
  const [previewContent, setPreviewContent] = useState('');

  const fetchBatchList = useCallback(async () => {
    if (!loginId) return;
    try {
      const response = await apiClient.get(`/batch/queue/${loginId}`);
      const sortedData = response.data.sort((a, b) => b.id - a.id);
      setBatchList(sortedData);
    } catch (error) {
    }
  }, [loginId]);

  useEffect(() => {
    const handleToggle = () => setIsSidebarOpen(prev => !prev);
    window.addEventListener('toggleSidebar', handleToggle);
    return () => window.removeEventListener('toggleSidebar', handleToggle);
  }, []);

  useEffect(() => {
    fetchBatchList();
    const interval = setInterval(() => fetchBatchList(), 10000);

    const handleRefresh = () => fetchBatchList();
    window.addEventListener('refreshBatchList', handleRefresh);

    return () => {
      clearInterval(interval);
      window.removeEventListener('refreshBatchList', handleRefresh);
    };
  }, [fetchBatchList]);

  useEffect(() => {
    const maxPage = Math.ceil(batchList.length / itemsPerPage);
    if (currentPage > maxPage && maxPage > 0) setCurrentPage(maxPage);
  }, [batchList, currentPage, itemsPerPage]);

  const handlePreview = async (batchId) => {
    try {
      const response = await apiClient.get(`/batch/queue/detail/${batchId}`);
      setPreviewContent(response.data);
      setIsPreviewModalOpen(true);
    } catch (error) {
      toast.error("파일 내용을 불러오는데 실패했습니다.");
    }
  };

  const getStatusLabel = (status) => {
    switch (status) {
      case 'READY':
      case 'PROCESSING': return { text: '진행 중', className: 'status-processing' };
      case 'COMPLETED': return { text: '완료', className: 'status-completed' };
      case 'FAILED': return { text: '실패', className: 'status-failed' };
      default: return { text: status, className: '' };
    }
  };

  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems = batchList.slice(indexOfFirstItem, indexOfLastItem);

  return (
    <>
      {isSidebarOpen && <div className="sidebar-backdrop" onClick={() => setIsSidebarOpen(false)}></div>}

      <div className={`page-sidebar ${isSidebarOpen ? 'open' : ''}`}>
        <div className="sidebar-box">
          <div className="sidebar-header">
            <h4>작업 대기열 현황</h4>
            <button className="sidebar-close-btn" onClick={() => setIsSidebarOpen(false)}>&times;</button>
          </div>

          <table className="history-table">
            <thead>
              <tr>
                <th>파일명</th>
                <th>상태</th>
                <th>미리보기</th>
                <th>일시</th>
              </tr>
            </thead>
            <tbody>
              {currentItems.length > 0 ? (
                currentItems.map((batch) => {
                  const statusInfo = getStatusLabel(batch.status);
                  const isPending = batch.status === 'READY' || batch.status === 'PROCESSING';

                  return (
                    <tr key={batch.id}>
                      <td className="truncate-text" title={batch.fileName || '-'}>
                        {batch.fileName || '-'}
                      </td>
                      <td>
                        <span className={`status-badge ${statusInfo.className}`}>
                          {statusInfo.text}
                        </span>
                      </td>
                      <td>
                        {batch.fileName ? (
                          <button
                            className="small-btn edit"
                            disabled={!isPending}
                            onClick={() => handlePreview(batch.id)}
                          >
                            확인
                          </button>
                        ) : (
                          <span>-</span>
                        )}
                      </td>
                      <td>
                          {batch.createdAt ? batch.createdAt.substring(2, 16).replace('T', ' ') : ''}
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan="4" className="empty-msg">현재 대기 중인 내역이 없습니다.</td>
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

      {isPreviewModalOpen && (
        <div className="modal-overlay" onClick={() => setIsPreviewModalOpen(false)}>
          <div className="modal-content file-preview-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <button className="close-btn" onClick={() => setIsPreviewModalOpen(false)}>&times;</button>
            </div>
            <div className="modal-body">
              <pre className="file-preview-content">{previewContent}</pre>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export default Sidebar;