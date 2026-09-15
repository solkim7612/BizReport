import React, { useState } from 'react';
import AdminTaxRate from './AdminTaxRate';
import AdminBatch from './AdminBatch';
import '../../assets/css/PageLayout.css';
import './Admin.css';

function Admin() {
  const [activeTab, setActiveTab] = useState('tax');

  return (
    <div className="page-layout admin-layout">
      <div className="page-main" style={{ maxWidth: '900px' }}>
        <div className="page-box">
          <div className="admin-header">
            <h2>시스템 관리자 대시보드</h2>
            <p className="section-desc">BizReport 서비스의 핵심 데이터와 배치를 관리합니다.</p>
          </div>

          <div className="page-tabs">
            <button
              className={activeTab === 'tax' ? 'active' : ''}
              onClick={() => setActiveTab('tax')}
            >
              세율 데이터 관리
            </button>
            <button
              className={activeTab === 'batch' ? 'active' : ''}
              onClick={() => setActiveTab('batch')}
            >
              배치 시스템 관리
            </button>
          </div>

          <div className="tab-content">
            {activeTab === 'tax' && <AdminTaxRate />}
            {activeTab === 'batch' && <AdminBatch />}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Admin;