import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';
import apiClient from '../../api/apiClient';
import { useAuth } from '../../context/AuthContext';
import './Report.css';
import PrepaidTaxModal from './PrepaidTaxModal';
import { ComposedChart, Bar, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

function VatReport({ onQueueUpdated }) {
  const { loginId } = useAuth();
  const navigate = useNavigate();
  const currentYear = new Date().getFullYear();
  const [selectedMon, setSelectedMon] = useState(`${currentYear}-01`);

  const [isProcessing, setIsProcessing] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  const [monthlyReport, setMonthlyReport] = useState(null);
  const [accReport, setAccReport] = useState(null);
  const [trendData, setTrendData] = useState([]);

  const [isMonthlyNotFound, setIsMonthlyNotFound] = useState(false);
  const [isAccNotFound, setIsAccNotFound] = useState(false);
  const [isMonthlyOpen, setIsMonthlyOpen] = useState(false);
  const [isAccOpen, setIsAccOpen] = useState(false);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [prepaidTax, setPrepaidTax] = useState(0);
  const [userInfo, setUserInfo] = useState({ refreshCount: 0 });

  const isReportNotFound = isMonthlyNotFound || isAccNotFound;

  const getAccPeriod = (monStr) => {
    const month = parseInt(monStr.split('-')[1], 10);
    const year = monStr.split('-')[0];
    return month <= 6
      ? { startMon: `${year}-01`, endMon: `${year}-06`, label: '상반기' }
      : { startMon: `${year}-07`, endMon: `${year}-12`, label: '하반기' };
  };

  const fetchUserInfo = useCallback(async () => {
    if (!loginId) return;
    try {
      const response = await apiClient.get(`/business/${loginId}`);
      setUserInfo(response.data);
    } catch (err) {
      const errorMessage = err.response?.data?.message || "요청 처리에 실패했습니다.";
      toast.error(errorMessage);
    }
  }, [loginId]);

  useEffect(() => {
    fetchUserInfo();
  }, [fetchUserInfo]);

  const handleSearch = async () => {
    setIsProcessing(true);
    setMonthlyReport(null);
    setAccReport(null);
    setTrendData([]);
    setIsMonthlyNotFound(false);
    setIsAccNotFound(false);
    setIsMonthlyOpen(false);
    setIsAccOpen(false);

    try {
      const response = await apiClient.get(`/reports/get/${loginId}?reportType=VAT&endMon=${selectedMon}`);
      const dataList = response.data;

      const mReport = dataList.find(r => r.periodType === 'MONTHLY');
      const aReport = dataList.find(r => r.periodType === 'ACCUMULATED');

      if (mReport) setMonthlyReport(mReport); else setIsMonthlyNotFound(true);
      if (aReport) setAccReport(aReport); else setIsAccNotFound(true);

      const currYear = parseInt(selectedMon.split('-')[0], 10);
      const currMonthNum = parseInt(selectedMon.split('-')[1], 10);

      let prevYear = currYear;
      let prevEndMon = '';

      if (currMonthNum <= 6) {
        prevYear = currYear - 1;
        prevEndMon = `${prevYear}-12`;
      } else {
        prevEndMon = `${prevYear}-06`;
      }

      const prevLabel = currMonthNum <= 6 ? `${prevYear}년 하반기` : `${prevYear}년 상반기`;
      const currLabel = currMonthNum <= 6 ? `${currYear}년 상반기` : `${currYear}년 하반기`;

      const prevRes = await apiClient.get(`/reports/get/${loginId}?reportType=VAT&endMon=${prevEndMon}`).catch(() => null);

      const prevDataList = prevRes?.data || [];
      const prevAReport = prevDataList.find(r => r.periodType === 'ACCUMULATED');

      const newTrendData = [
        {
          name: prevLabel,
          sales: Math.floor((prevAReport?.calc?.sales || 0) / 10000),
          purchase: Math.floor((prevAReport?.calc?.purchases || 0) / 10000),
          tax: Math.floor((prevAReport?.calc?.beforeTax || 0) / 10000)
        },
        {
          name: currLabel,
          sales: Math.floor((aReport?.calc?.sales || 0) / 10000),
          purchase: Math.floor((aReport?.calc?.purchases || 0) / 10000),
          tax: Math.floor((aReport?.calc?.beforeTax || 0) / 10000)
        }
      ];
      setTrendData(newTrendData);

    } catch (err) {
      const errData = err.response?.data || {};

      if (errData.code === 'REPORT_NOT_FOUND') {
        setIsMonthlyNotFound(true);
        setIsAccNotFound(true);
        toast.warning("해당 기간의 리포트가 없습니다. 갱신을 진행해주세요.");
      } else {
        const errorMessage = errData.message || "요청 처리에 실패했습니다.";
        toast.error(errorMessage);
      }
    } finally {
      setIsProcessing(false);
    }
  };

  const handleUpdateReport = async (val) => {
    try {
      await apiClient.patch(`/reports/update/${loginId}`, {
        reportType: 'VAT',
        endMon: selectedMon,
        prepaidTax: val
      });
      toast.success("수정된 기납부세액이 반영되었습니다.");
      setIsModalOpen(false);
      await handleSearch();

      if (onQueueUpdated) onQueueUpdated();
    } catch (err) {
      const errorMessage = err.response?.data?.message || "요청 처리에 실패했습니다.";
      toast.error(errorMessage);
    }
  };

  const handleRefreshReport = async () => {
    if (userInfo.refreshCount <= 0) return toast.error("갱신권이 부족합니다. 갱신권을 충전해주세요.");

    const confirmMsg = isReportNotFound
      ? `세무 데이터 집계 전입니다. 그래도 생성하시겠습니까? (남은 갱신권: ${userInfo.refreshCount}회)`
      : `리포트를 갱신하시겠습니까? (남은 갱신권: ${userInfo.refreshCount}회)`;

    if (!window.confirm(confirmMsg)) return;

    setIsCreating(true);
    try {
      const currentPrepaidTax = accReport?.calc?.prepaidTax || monthlyReport?.calc?.prepaidTax || 0;

      await apiClient.post(`/reports/refresh/${loginId}`, {
        reportType: 'VAT',
        endMon: selectedMon,
        prepaidTax: currentPrepaidTax
      });

      toast.success("리포트가 성공적으로 갱신되었습니다.");
      await fetchUserInfo();
      await handleSearch();

      if (onQueueUpdated) onQueueUpdated();
    } catch (err) {
      const errorMessage = err.response?.data?.message || "요청 처리에 실패했습니다.";
      toast.error(errorMessage);
    } finally {
      setIsCreating(false);
    }
  };

  const fetchPrepaidTax = async () => {
    try {
      const response = await apiClient.get(`/reports/prepaid-tax/${loginId}`, {
        params: { reportType: 'VAT', endMon: selectedMon }
      });
      setPrepaidTax(response.data.prepaidTax || 0);
      setIsModalOpen(true);
    } catch (err) {
      const errorMessage = err.response?.data?.message || "요청 처리에 실패했습니다.";
      toast.error(errorMessage);
    }
  };

  const renderVatDetail = (report) => {
    if (!report || !report.calc) return null;
    const c = report.calc;
    return (
      <div className="calc-detail-box">
        {c.notice && (
          <div className="notice-banner">
            {c.notice} (마지막 갱신 일시: {report.updatedAt ? new Date(report.updatedAt).toLocaleString() : '데이터 없음'})
          </div>
        )}
        <div className="calc-row"><span>매출 공급가액</span><span>{c.sales?.toLocaleString()} 원</span></div>
        <div className="calc-row"><span>매출 세액 (+)</span><span className="text-red">{c.salesTax?.toLocaleString()} 원</span></div>
        <hr/>
        <div className="calc-row"><span>매입 공급가액</span><span>{c.purchases?.toLocaleString()} 원</span></div>
        <div className="calc-row"><span>매입 세액 (-)</span><span className="text-blue">{c.purchaseTax?.toLocaleString()} 원</span></div>
        <hr/>
        <div className="calc-row fw-bold"><span>산출세액</span><span>{c.beforeTax?.toLocaleString()} 원</span></div>
        <div className="calc-row">
          <div className="calc-prevTax-row">
            <span>기납부세액 (-)</span>
            <button
              className="small-btn edit"
              onClick={(e) => {
                e.stopPropagation();
                fetchPrepaidTax();
              }}
            >
              수정
            </button>
          </div>
          <span>{c.prepaidTax?.toLocaleString()} 원</span>
        </div>
        <div className="calc-total-row">
          <span>최종 {c.isRefund ? '환급' : '납부'} 세액</span>
          <span className={c.isRefund ? 'text-blue' : 'text-red'}>
            {c.pay?.toLocaleString()} 원
          </span>
        </div>
      </div>
    );
  };

  const renderGraph = () => {
    if (!trendData || trendData.length === 0) return null;

    return (
      <div className="graph-container">
        <h4>
          과세기간별 부가세 추이 <span>(단위: 만원)</span>
        </h4>
        <div className="graph-comparison">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={trendData} margin={{ top: 10, right: 10, bottom: 0, left: -20 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />
              <XAxis dataKey="name" tick={{ fontSize: 13, fill: '#6b7280' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 13, fill: '#6b7280' }} axisLine={false} tickLine={false} tickFormatter={(val) => val.toLocaleString()} />

              <Tooltip
                formatter={(value) => `${value.toLocaleString()} 만원`}
                contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}
                itemSorter={(item) => {
                  const order = ['매출공급가액', '매입공급가액', '산출세액'];
                  return order.indexOf(item.name);
                }}
              />

              <Legend
                wrapperStyle={{ paddingTop: '15px' }}
                content={(props) => {
                  const { payload } = props;
                  const order = ['매출공급가액', '매입공급가액', '산출세액'];

                  const sortedPayload = [...(payload || [])].sort(
                    (a, b) => order.indexOf(a.value) - order.indexOf(b.value)
                  );

                  return (
                    <ul className="recharts-default-legend" style={{ padding: 0, margin: 0, textAlign: 'center', listStyle: 'none' }}>
                      {sortedPayload.map((entry, index) => (
                        <li key={`item-${index}`} className="recharts-legend-item" style={{ display: 'inline-flex', alignItems: 'center', marginRight: '15px' }}>
                          {entry.value === '산출세액' ? (
                            <svg width="14" height="14" style={{ marginRight: '6px' }}>
                              <line x1="0" y1="7" x2="14" y2="7" stroke={entry.color} strokeWidth="2" />
                              <circle cx="7" cy="7" r="3" fill={entry.color} stroke="#fff" strokeWidth="1" />
                            </svg>
                          ) : (
                            <div style={{ width: '12px', height: '12px', backgroundColor: entry.color, marginRight: '6px', borderRadius: '2px' }}></div>
                          )}
                          <span style={{ color: '#4b5563', fontSize: '13px' }}>{entry.value}</span>
                        </li>
                      ))}
                    </ul>
                  );
                }}
              />

              <Bar dataKey="sales" name="매출공급가액" barSize={40} fill="#ef4444" radius={[4, 4, 0, 0]} />
              <Bar dataKey="purchase" name="매입공급가액" barSize={40} fill="#3b82f6" radius={[4, 4, 0, 0]} />
              <Line type="monotone" dataKey="tax" name="산출세액" stroke="#10b981" strokeWidth={3} dot={{ r: 5, fill: '#10b981', strokeWidth: 2, stroke: '#fff' }} />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </div>
    );
  };

  const accInfo = getAccPeriod(selectedMon);
  const hasSearched = monthlyReport !== null || accReport !== null || isMonthlyNotFound || isAccNotFound;

  return (
    <div className="report-section">
      <div className="filter-bar">
        <label className="fw-bold">과세기간</label>
        <input type="month" value={selectedMon} onChange={(e) => setSelectedMon(e.target.value)} />
        <div className="button-group-report">
          <button className="btn primary" onClick={handleSearch} disabled={isProcessing}>
            {isProcessing ? '처리 중...' : '조회'}
          </button>
          <button className="btn secondary" onClick={handleRefreshReport} disabled={isCreating}>
            {isCreating ? '처리 중...' : `리포트 갱신 (${userInfo.refreshCount}회)`}
          </button>
        </div>
      </div>

      <PrepaidTaxModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        initialValue={prepaidTax}
        onConfirm={handleUpdateReport}
      />

      {hasSearched && (
        <div className="report-card">
          <div className="report-card-header" onClick={() => !isMonthlyNotFound && setIsMonthlyOpen(!isMonthlyOpen)}>
            <div className="report-title">
              <span className="badge badge-monthly">월별</span>
              <strong>{selectedMon.split('-')[1]}월</strong> 예상 {monthlyReport?.calc?.isRefund ? '환급' : '납부'}세액
              {isMonthlyNotFound ? (
                <span className="highlight-tax text-gray"> - 원</span>
              ) : (
                <span className={`${monthlyReport?.calc?.isRefund ? 'text-blue' : 'text-red'}`}>
                  {' '}{monthlyReport?.calc?.pay?.toLocaleString()}원
                </span>
              )}
            </div>
            {!isMonthlyNotFound && <button className="toggle-btn">{isMonthlyOpen ? '▼' : '▶'}</button>}
          </div>
          {isMonthlyNotFound && (
            <div className="report-card-body empty-report-msg">
              생성된 리포트가 없습니다.
            </div>
          )}
          {(isMonthlyOpen && monthlyReport) && (
            <div className="report-card-body">
              {renderVatDetail(monthlyReport)}
            </div>
          )}
        </div>
      )}

      {hasSearched && (
        <div className="report-card">
          <div className="report-card-header" onClick={() => !isAccNotFound && setIsAccOpen(!isAccOpen)}>
            <div className="report-title">
              <span className="badge badge-acc">누적</span>
              <strong>{accInfo.startMon.split('-')[0]}년 {accInfo.label}</strong> 예상 {accReport?.calc?.isRefund ? '환급' : '납부'}세액
              {isAccNotFound ? (
                <span className="highlight-tax text-gray"> - 원</span>
              ) : (
                <span className={`${accReport?.calc?.isRefund ? 'text-blue' : 'text-red'}`}>
                  {' '}{accReport?.calc?.pay?.toLocaleString()}원
                </span>
              )}
            </div>
            {!isAccNotFound && <button className="toggle-btn">{isAccOpen ? '▼' : '▶'}</button>}
          </div>
          {isAccNotFound && (
            <div className="report-card-body empty-report-msg">
              생성된 리포트가 없습니다.
            </div>
          )}
          {(isAccOpen && accReport) && (
            <div className="report-card-body">
              {renderGraph(accReport)}
              {renderVatDetail(accReport)}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default VatReport;