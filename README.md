# BizReport - 소상공인 맞춤형 자동 기장 및 세금 예측 시스템

> **"복잡한 세무 지식을 코드로 추상화하여, 소상공인의 세금 불안을 해소합니다."**
<br/>
> 실무 경험을 바탕으로 기획/개발한 백엔드 시스템입니다.

<br/>

## 1. Project Overview
- **기획 배경:** 소상공인들은 매일 발생하는 결제 데이터 속에서 당월 예상 부가세와 종합소득세를 파악하기 어렵습니다.
- **핵심 목표:** 가상의 외부 API(홈택스, 카드사)에서 수만 명의 대용량 거래 데이터를 수집 및 정제하고, 매일 새벽 배치를 통해 세금 리포트를 자동 생성합니다. OCR 기술을 연동하여 영수증 수기 입력의 번거로움을 최소화합니다.

<br/>

## 2. Tech Stack
- **Language:** Java 17
- **Framework:** Spring Boot 3.2.4
- **Data Access:** Spring Data JPA, Querydsl 5.0
- **Batch Processing:** Spring Batch 5
- **Database:** MySQL 8.0, H2 (Test)
- **Monitoring & API:** Jaeger (Distributed Tracing), Swagger (OpenAPI 3.0)
- **Infra:** Docker & Docker Compose

<br/>

## 3. System Architecture
![System Architecture](https://img.shields.io/badge/Architecture-Layered-blue)

```mermaid
graph TD
    subgraph "External Integration"
        H[가상 홈택스 API]
        C[카드사 API]
        OCR[Naver CLOVA OCR]
    end

    subgraph "BizReport Spring Boot (API & Batch)"
        direction TB
        Collector[Data Collector / Open API 연동]
        Processor[Spring Batch / Chunk Processing]
        TaxEngine[Tax Calculation Engine]
        API[REST API Controller]
    end

    subgraph "Storage"
        M[(MySQL - Main DB)]
    end

    H & C --> Collector
    Collector --> M
    M --> Processor
    Processor --> TaxEngine
    TaxEngine --> M
    
    U((소상공인 유저)) --> API
    API --> OCR
    OCR --> M
```

<br/>

## 4. Database Design
- 과세 유형 변경(일반/간이) 이력 추적과 사용자 데이터 무결성을 보장하기 위해 정규화된 설계를 적용했습니다.

```mermaid
erDiagram
    TAX_RATE ||--o{ USER : "applies_to (논리적 참조)"
    USER ||--o{ BIZ_HISTORY : "tracks"
    USER ||--o{ DATA : "owns"
    USER ||--o{ REPORT : "generates"

    TAX_RATE {
        varchar(10) ind_code PK "업종코드"
        year target_year PK "귀속연도"
        decimal vat_rate "업종별 부가가치율"
        decimal exp_rate "단순경비율"
    }

    USER {
        varchar(12) tax_id PK "사업자등록번호"
        varchar(255) password "비밀번호"
        varchar(50) owner_name "대표자명"
        date open_date "개업일자"
        varchar(100) biz_name "상호"
        varchar(10) ind_code FK "주업종코드"
        enum status "상태(CONTINUED/CLOSED)"
        datetime created_at 
        datetime updated_at 
    }

    BIZ_HISTORY {
        bigint history_id PK 
        varchar(12) tax_id FK "사업자등록번호"
        enum vat_type "일반/간이"
        varchar(100) ind_name "업태 및 종목명"
        date start_date "적용 시작일"
        date end_date "적용 종료일"
    }

    DATA {
        bigint data_id PK 
        varchar(12) tax_id FK "사업자등록번호"
        enum data_type "매출/매입 구분"
        enum data_method "증빙종류"
        boolean is_e "전자여부"
        boolean is_mod "유저수정여부"
        varchar(12) vendor_id "거래처 사업자번호"
        date trans_date "거래일자"
        decimal net_price "공급가액"
        decimal vat_value "세액"
        decimal total_price "합계금액"
        varchar(500) ocr_url "영수증 이미지 경로"
        datetime created_at 
    }

    REPORT {
        bigint report_id PK 
        varchar(12) tax_id FK "사업자등록번호"
        enum report_type "리포트 타입"
        varchar(7) target_month "대상 월(YYYY-MM)"
        decimal tax_result "예상세액"
        json tax_calc "계산 근거(JSON)"
        datetime updated_at 
    }
```

<br/>

## 5. Core Features
### 5.1. 주요 도메인 기능
- 사업자 진위 확인 및 이력 관리: 공공데이터 API를 활용해 1,4,7,10,12월 1일마다 사업자 상태를 동기화하고, 과세 유형(일반/간이) 변경 시 BUSINESS_HISTORY 테이블에 이력을 안전하게 적재합니다.
- 영수증 AI 자동 기장: 사용자가 업로드한 영수증 이미지를 비동기로 OCR 분석하여 DATA 테이블에 적재합니다.
- 대용량 세금 리포트 생성 배치: 매일 새벽 3시, 수만 건의 거래 데이터를 Chunk 단위로 읽어 부가세/종소세를 계산합니다.
### 5.2. Process Flow
```mermaid
sequenceDiagram
participant B as Spring Batch
participant DB as MySQL (DATA, HISTORY)
participant E as Tax Engine

    B->>DB: 1. 대상 과세기간 데이터 청크 읽기 (Reader)
    DB-->>B: 결제 데이터 반환
    B->>DB: 2. 거래 발생 시점의 과세 유형(일반/간이) 조회
    DB-->>B: HISTORY 반환
    B->>E: 3. 세액 계산 요청 (Processor)
    E->>E: 업종별 부가가치율 및 공제율 적용 알고리즘
    E-->>B: 예상 납부 세액 산출
    B->>DB: 4. REPORT 테이블에 결과 및 근거(JSON) 저장 (Writer)
```

### 5.3. Out of Scope
- 성실신고대상자 및 복식부기 의무자 대상 로직 제외 (소규모 소상공인 타겟)
- 상세한 세액 공제/감면 특례 적용 제외 (핵심 로직 검증에 집중)
- 실제 공인인증서를 통한 홈택스 스크래핑 (가상 Mock 데이터 API로 대체)

## 6. Tax Calculation Logic
### 6.1. 부가가치세
- 일반과세자: (매출액 * 10%) - (매입액 * 10%) - 기납부세액
- 간이과세자: (매출액 * 업종별부가가치율 * 10%) - (매입액 * 0.5%) - 기납부세액
### 6.2. 종합소득세(단순경비율)
- 소득금액 = 수입금액 - (수입금액 * 단순경비율)
- 산출세액 = (소득금액 - 1,500,000) * 누진세율 - 누진공제

## 7. Troubleshooting
- 추가예정

## 8. API Documentation
- 추가예정

## 9. Testing
- 추가예정
