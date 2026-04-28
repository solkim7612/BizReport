-- 1. 사용자(사업자) 테이블
CREATE TABLE IF NOT EXISTS `USER` (
                                      `b_id` VARCHAR(12) PRIMARY KEY COMMENT '사업자등록번호',
    `nm` VARCHAR(255) COMMENT '상호명',
    `tax_type` VARCHAR(50) NOT NULL COMMENT '과세유형 (GENERAL, SIMPLIFIED 등)',
    `tax_type_change_dt` DATE COMMENT '과세유형 전환일자',
    `ind_cd` VARCHAR(10) COMMENT '업종코드',
    `ind_nm` VARCHAR(255) COMMENT '업종명',
    `b_stt` VARCHAR(50) COMMENT '영업상태',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 세율/경비율 마스터 테이블
CREATE TABLE IF NOT EXISTS `TAX_RATE` (
                                          `ind_cd` VARCHAR(10) NOT NULL COMMENT '업종코드',
    `year` VARCHAR(4) NOT NULL COMMENT '귀속연도',
    `vat_rate` DECIMAL(5, 4) NOT NULL COMMENT '업종별 부가가치율',
    `exp_rate` DECIMAL(5, 4) NOT NULL COMMENT '단순경비율',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`ind_cd`, `year`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 가상 세무 데이터(영수증) 테이블
CREATE TABLE IF NOT EXISTS `DATA` (
                                      `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      `b_id` VARCHAR(12) NOT NULL COMMENT '사업자등록번호 (FK)',
    `type` VARCHAR(50) NOT NULL COMMENT 'SALES(매출) or PURCHASE(매입)',
    `method` VARCHAR(50) NOT NULL COMMENT '결제수단',
    `is_e` BOOLEAN NOT NULL COMMENT '전자발행 여부',
    `is_mod` BOOLEAN NOT NULL COMMENT '수정 여부',
    `vendor_id` VARCHAR(12) COMMENT '거래처 사업자번호',
    `trans_date` DATE NOT NULL COMMENT '거래일자',
    `net_value` DECIMAL(15, 0) NOT NULL COMMENT '공급가액',
    `vat_value` DECIMAL(15, 0) NOT NULL COMMENT '부가세액',
    `total_price` DECIMAL(15, 0) NOT NULL COMMENT '공급대가 (총액)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`b_id`) REFERENCES `USER`(`b_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 세금 리포트 테이블
CREATE TABLE IF NOT EXISTS `REPORT` (
                                        `report_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        `b_id` VARCHAR(12) NOT NULL,
    `report_type` ENUM('VAT', 'CIT') NOT NULL COMMENT '부가세 또는 종소세',
    `period_type` ENUM('MONTHLY', 'ACCUMULATED') NOT NULL COMMENT '월간 또는 누적',
    `period_target` VARCHAR(50) NOT NULL COMMENT 'YYYY-MM-dd 또는 YYYY-MM',
    `tax_result` DECIMAL(15, 0) DEFAULT 0 COMMENT '예상세액',
    `tax_calc` JSON COMMENT '계산 근거 스냅샷 (세율, 경비율 등)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_report_target` (`b_id`, `report_type`, `period_type`, `period_target`),
    FOREIGN KEY (`b_id`) REFERENCES `USER`(`b_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;