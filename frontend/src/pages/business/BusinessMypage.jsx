import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import apiClient from '../../api/apiClient';
import { useAuth } from '../../context/AuthContext';
import './Business.css';

function BusinessMypage() {
  const { loginId } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({ nm: '', indCd: '', indNm: '', taxType: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchType, setSearchType] = useState('name');

  useEffect(() => {
    const fetchBusinessInfo = async () => {
      try {
        const response = await apiClient.get(`/business/${loginId}`);
        setFormData({
          nm: response.data.nm || '',
          indCd: response.data.indCd || '',
          indNm: response.data.indNm || '',
          taxType: response.data.taxType || ''
        });
      } catch (error) {
        toast.error("정보를 불러오는데 실패했습니다.");
      }
    };
    if (loginId) fetchBusinessInfo();
  }, [loginId]);

  const handleUpdate = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      await apiClient.patch(`/business/${loginId}`, {
        nm: formData.nm,
        indCd: formData.indCd
      });
      toast.success('회원님의 정보가 정상적으로 수정되었습니다.');
      navigate('/');
    } catch (error) {
      toast.error(error.response?.data?.message || '정보 수정에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const openModal = () => setIsModalOpen(true);
  const closeModal = () => {
    setIsModalOpen(false);
    setSearchKeyword('');
    setSearchResults([]);
    setSearchType('name');
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchKeyword.trim()) return toast.warning('검색어를 입력해주세요.');

    setIsSearching(true);
    try {
      const endpoint = searchType === 'code'
        ? `/business/search/indCd?keyword=${searchKeyword}`
        : `/business/search/indNm?keyword=${searchKeyword}`;

      const response = await apiClient.get(endpoint);
      setSearchResults(response.data);
    } catch (error) {
      toast.error('검색 중 오류가 발생했습니다.');
    } finally {
      setIsSearching(false);
    }
  };

  const handleSelectIndustry = (indCd, indNm) => {
    setFormData({ ...formData, indCd, indNm });
    closeModal();
  };

  return (
    <div>
      <form onSubmit={handleUpdate} className="business-form">
        <div className="form-group">
          <label>사업자등록번호</label>
          <input type="text" value={loginId || ''} disabled className="disabled-input" />
        </div>

        <div className="form-group">
          <label>과세유형</label>
          <input
            type="text"
            value={formData.taxType}
            disabled
            className="disabled-input"
          />
        </div>

        <div className="form-group">
          <label>회사명</label>
          <input
            type="text"
            placeholder="회사명을 입력하세요"
            value={formData.nm}
            onChange={(e) => setFormData({ ...formData, nm: e.target.value })}
            required
          />
        </div>

        <div className="form-group">
          <label>업종</label>
          <div className="group-ind">
            <input
              type="text"
              placeholder="업종을 검색해주세요"
              value={formData.indNm ? `[${formData.indCd}] ${formData.indNm}` : ''}
              readOnly
              required
            />
            <button type="button" onClick={openModal} className="btn secondary">검색</button>
          </div>
        </div>

        <button type="submit" className="btn primary" disabled={isSubmitting}>
          {isSubmitting ? '처리 중...' : '저장하기'}
        </button>
      </form>

      {isModalOpen && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <button className="close-btn" onClick={closeModal}>&times;</button>
            </div>

            <div className="modal-tab">
              <button
                type="button"
                className={searchType === 'name' ? 'active' : ''}
                onClick={() => { setSearchType('name'); setSearchKeyword(''); setSearchResults([]); }}
              >
                업종명
              </button>
              <button
                type="button"
                className={searchType === 'code' ? 'active' : ''}
                onClick={() => { setSearchType('code'); setSearchKeyword(''); setSearchResults([]); }}
              >
                업종코드
              </button>
            </div>

            <form onSubmit={handleSearch} className="modal-search-form">
              <input
                type="text"
                placeholder={searchType === 'name' ? '업종명 (예: 서비스업) 입력' : '업종코드 (예: 552101) 입력'}
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
              />
              <button type="submit" className="btn secondary" disabled={isSearching}>
                {isSearching ? '처리 중...' : '검색'}
              </button>
            </form>

            <div className="modal-results">
              {searchResults.length > 0 ? (
                <ul className="result-list">
                  {searchResults.map((item, index) => (
                    <li
                      key={index}
                      onClick={() => handleSelectIndustry(item.indCd, item.indNm)}
                    >
                      <span className="result-cd">{item.indCd}</span>
                      <span className="result-nm">{item.indNm}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="no-result">검색 결과가 없습니다.</p>
              )}
            </div>
          </div>
        </div>
      )}
      </div>
  );
}

export default BusinessMypage;