import React, { useState } from 'react';
import '../../assets/css/PageLayout.css';

function EndMonthModal({ isOpen, onClose, onConfirm }) {
  const today = new Date();
  today.setMonth(today.getMonth() - 1);
  const prevMonth = today.toISOString().slice(0, 7);

  const [endMon, setEndMon] = useState(prevMonth);

  const handleConfirm = async () => {
    if (!endMon) return alert("생성할 과세 연월을 선택해주세요.");
    await onConfirm(endMon);
  };

  if (!isOpen) return null;
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>

        <div className="modal-header">
          <button className="close-btn" onClick={onClose}>&times;</button>
        </div>

        <div className="modal-body">
          <div className="form-group">
            <p className="admin-desc">생성할 리포트의 과세 연월을 선택하세요.</p>
            <input
              type="month"
              value={endMon}
              onChange={(e) => setEndMon(e.target.value)}
            />
          </div>

          <div className="button-group-modal">
            <button className="btn secondary" onClick={onClose}>취소</button>
            <button className="btn primary" onClick={handleConfirm}>생성</button>
          </div>
        </div>

      </div>
    </div>
  );
}

export default EndMonthModal;