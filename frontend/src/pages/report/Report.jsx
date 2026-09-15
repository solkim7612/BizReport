import React, { useState } from 'react';
import VatReport from './VatReport';
import CitReport from './CitReport';
import '../../assets/css/PageLayout.css';

function Report() {
  const [activeTab, setActiveTab] = useState('VAT');

  return (
    <div className="page-layout">
      <div className="page-main">
        <div className="page-box">
          <div className="page-tabs">
            <button
              className={activeTab === 'VAT' ? 'active' : ''}
              onClick={() => setActiveTab('VAT')}
            >
              부가세 리포트
            </button>
            <button
              className={activeTab === 'CIT' ? 'active' : ''}
              onClick={() => setActiveTab('CIT')}
            >
              종합소득세 리포트
            </button>
          </div>

          <div className="tab-content">
            {activeTab === 'VAT' && <VatReport />}
            {activeTab === 'CIT' && <CitReport />}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Report;