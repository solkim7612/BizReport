import React, { useState, useEffect } from 'react';
import './Report.css';

function PrepaidTaxModal({ isOpen, onClose, onConfirm, initialValue }) {
  const [val, setVal] = useState(initialValue);

  useEffect(() => {
    setVal(initialValue);
  }, [initialValue]);

  const handleConfirm = async () => {
    await onConfirm(val);
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
            <p>
              홈택스 조회 금액: <strong className="text-blue">{initialValue.toLocaleString()}원</strong>
            </p>
            <label>반영할 기납부세액 (원)</label>
            <input
              type="number"
              value={val}
              onChange={(e) => setVal(Number(e.target.value))}
            />
          </div>

          <div className="button-group-modal">
            <button className="btn secondary" onClick={onClose}>취소</button>
            <button className="btn primary" onClick={handleConfirm}>확정</button>
          </div>
        </div>

      </div>
    </div>
  );
}

export default PrepaidTaxModal;