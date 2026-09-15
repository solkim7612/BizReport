import React, { useState } from 'react';
import DataInquiry from './DataInquiry';
import DataScraping from './DataScraping';
import DataUpload from './DataUpload';
import '../../assets/css/PageLayout.css';

function Data() {
  const [activeTab, setActiveTab] = useState('inquiry');

  const handleQueueUpdated = () => {
    window.dispatchEvent(new Event('refreshBatchList'));
  };

  return (
    <div className="page-layout">
      <div className="page-main">
        <div className="page-box">
          <div className="page-tabs">
            <button className={activeTab === 'inquiry' ? 'active' : ''} onClick={() => setActiveTab('inquiry')}>
              세무 데이터 조회
            </button>
            <button className={activeTab === 'scraping' ? 'active' : ''} onClick={() => setActiveTab('scraping')}>
              국세청 스크래핑
            </button>
            <button className={activeTab === 'upload' ? 'active' : ''} onClick={() => setActiveTab('upload')}>
              카드 내역 업로드
            </button>
          </div>

          <div className="tab-content">
            {activeTab === 'inquiry' && <DataInquiry />}
            {activeTab === 'scraping' && <DataScraping />}
            {activeTab === 'upload' && <DataUpload onQueueUpdated={handleQueueUpdated} />}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Data;