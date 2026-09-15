import React, { useState } from 'react';
import BusinessMypage from './BusinessMypage';
import BusinessCharge from './BusinessCharge';
import '../../assets/css/PageLayout.css';

function Business() {
  const [activeTab, setActiveTab] = useState('mypage');

  return (
    <div className="page-layout">
      <div className="page-main" style={{ margin: '0 auto' }}>
        <div className="page-box">
          <div className="page-tabs">
            <button className={activeTab === 'mypage' ? 'active' : ''} onClick={() => setActiveTab('mypage')}>내 정보 수정</button>
            <button className={activeTab === 'charge' ? 'active' : ''} onClick={() => setActiveTab('charge')}>갱신권 구매</button>
          </div>
          <div className="tab-content">
            {activeTab === 'mypage' ? <BusinessMypage /> : <BusinessCharge />}
          </div>
        </div>
      </div>
    </div>
  );
}
export default Business;