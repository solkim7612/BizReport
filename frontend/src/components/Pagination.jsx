import React from 'react';
import './Pagination.css';

function Pagination({ totalItems, itemsPerPage, currentPage, onPageChange, pageWindowSize = 5 }) {
  const totalPages = Math.ceil(totalItems / itemsPerPage);

  if (totalPages === 0) return null;

  const startPage = Math.floor((currentPage - 1) / pageWindowSize) * pageWindowSize + 1;
  const endPage = Math.min(startPage + pageWindowSize - 1, totalPages);
  const pageNumbers = Array.from({ length: endPage - startPage + 1 }, (_, i) => startPage + i);

  return (
    <div className="pagination">
      <button onClick={() => onPageChange(1)} disabled={currentPage === 1}>&lt;&lt;</button>
      <button onClick={() => onPageChange(Math.max(1, currentPage - 1))} disabled={currentPage === 1}>&lt;</button>

      {pageNumbers.map(p => (
        <button
          key={p}
          className={currentPage === p ? 'active' : ''}
          onClick={() => onPageChange(p)}
        >
          {p}
        </button>
      ))}

      <button onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))} disabled={currentPage === totalPages}>&gt;</button>
      <button onClick={() => onPageChange(totalPages)} disabled={currentPage === totalPages}>&gt;&gt;</button>
    </div>
  );
}

export default Pagination;