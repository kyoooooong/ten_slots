# Ten Slots

고동시성 환경에서 재고 정합성과 결제 원자성을 보장하는 예약·결제 백엔드 시스템입니다.

---

## Tech Stack

| 분류 | 기술 |
|------|------|
| Language / Framework | Java 21, Spring Boot 3.4.5 |
| Database | MySQL 8.0, Redis 7 |
| Resilience | Resilience4j Circuit Breaker |
| Load Balancer | Nginx (rate limiting 포함) |
| Monitoring | Prometheus, Grafana |
| Infrastructure | Docker Compose (app 2 instances) |
| ID Generation | TSID (분산 환경 time-ordered ID) |

---

## System Architecture

### Infrastructure

```
                        ┌──────────────────────────────┐
       Client           │           Nginx :80           │
      Requests  ───────▶│  rate limit / round-robin LB  │
                        └───────────┬──────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
             ┌────────────┐                 ┌────────────┐
             │   app1     │                 │   app2     │
             │ :8080      │                 │ :8080      │
             │ tsid.node=1│                 │ tsid.node=2│
             └─────┬──────┘                 └─────┬──────┘
                   │                               │
          ┌────────┴───────────────────────────────┘
          │                     │
          ▼                     ▼
   ┌─────────────┐      ┌──────────────┐
   │  Redis :6380│      │ MySQL  :3307 │
   │  stock      │      │ booking      │
   │  idempotency│      │ payment      │
   └─────────────┘      │ product      │
                        │ stock        │
                        │ users        │
                        └──────────────┘

   ┌───────────────┐    ┌───────────────┐
   │ Prometheus    │    │ Grafana       │
   │ :9090         │───▶│ :3000         │
   └───────────────┘    └───────────────┘
```

### Hexagonal Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Adapters IN                          │
│  BookingController  (POST /api/v1/booking)                  │
│  CheckoutController (GET  /api/v1/checkout/{productId})     │
│  StockWarmupScheduler (cron — 오픈 10분 전 Redis 사전 적재) │
└────────────────────────────┬────────────────────────────────┘
                             │  Port IN (UseCase interface)
┌────────────────────────────▼────────────────────────────────┐
│                       Application                           │
│  BookingService              CheckoutService                │
│  CompositePaymentProcessor                                  │
└────────────────────────────┬────────────────────────────────┘
                             │  Port OUT (interface)
┌────────────────────────────▼────────────────────────────────┐
│                       Adapters OUT                          │
│                                                             │
│  Redis    CircuitBreakerStockAdapter  (@Primary)            │
│           RedisStockAdapter  (Lua: EXISTS+DECR, atomic)     │
│           RedisIdempotencyAdapter  (SET NX, 24h TTL)        │
│                                                             │
│  MySQL    BookingPersistenceAdapter                         │
│           ConfirmBookingPersistenceAdapter                  │
│           PaymentPersistenceAdapter                         │
│           DbStockAdapter  (optimistic lock, retry ×3)       │
│           ProductPersistenceAdapter                         │
│                                                             │
│  PG       CreditCardGatewayAdapter                          │
│           YPayGatewayAdapter                                │
│           PointGatewayAdapter                               │
└─────────────────────────────────────────────────────────────┘
```

### Booking Flow

```
POST /api/v1/booking
  │
  ├─ [1] 중복 요청 차단          Redis SET NX (idempotencyKey)
  ├─ [2] 상품 조회 + 검증        오픈 시각, 결제 금액 합계
  ├─ [3] 재고 차감               Redis Lua → CB OPEN 시 DbStockAdapter (optimistic lock, retry ×3)
  ├─ [4] PENDING 예약 저장       REQUIRES_NEW → 즉시 커밋 (장애 복구 기준점)
  ├─ [5] PG 결제 (TX 밖)         커넥션 미점유, timeout → inquiry(pgIdempotencyKey)
  │       partial failure      → 앞서 승인된 결제 동기 취소
  └─ [6] 예약 + 결제 확정        단일 TX REQUIRED → 원자적 커밋
```

### Sequence Diagram

```
Client          Nginx           BookingService      Redis           MySQL(DB)       PG
  │               │                   │               │                │             │
  │─POST /booking▶│                   │               │                │             │
  │               │──rate limit───────│               │                │             │
  │               │  (10r/s,burst30)  │               │                │             │
  │               │                   │               │                │             │
  │               │                   │─SET NX───────▶│                │             │
  │               │                   │◀─acquired─────│                │             │
  │               │                   │               │                │             │
  │               │                   │─findById──────────────────────▶│             │
  │               │                   │◀─product──────────────────────│             │
  │               │                   │  (검증: openAt, totalAmount)   │             │
  │               │                   │               │                │             │
  │               │                   │─Lua DECR─────▶│                │             │
  │               │                   │  (CB OPEN시)──────────────────▶│ @Version    │
  │               │                   │◀─result───────│                │             │
  │               │                   │               │                │             │
  │               │                   │─save(PENDING)─────────────────▶│ REQUIRES_NEW│
  │               │                   │◀─booking──────────────────────│             │
  │               │                   │               │                │             │
  │               │                   │─save(REQUESTED)───────────────▶│ REQUIRES_NEW│
  │               │                   │─process()──────────────────────────────────▶│
  │               │                   │  (timeout→inquiry)             │            │
  │               │                   │─save(APPROVED)────────────────▶│ REQUIRES_NEW│
  │               │                   │               │                │             │
  │               │                   │─confirm(booking+payment)──────▶│ REQUIRED TX │
  │               │                   │◀─confirmed────────────────────│             │
  │               │                   │               │                │             │
  │◀─201 BOOKING_SUCCESS──────────────│               │                │             │
```

---

## How to Run

```bash
# 1. 환경 변수 설정
cp .sample.env .env
# 아래 항목을 확인·수정한다:
#   SPRING_PROFILES_ACTIVE     → local
#   SPRING_DATASOURCE_USERNAME → DB 사용자명 (Docker: root)
#   MYSQL_ROOT_PASSWORD        → MySQL root 비밀번호 (Docker: password)
#   MYSQL_DATABASE             → 데이터베이스명 (Docker: ten_slots)
#   SPRING_JPA_DDL_AUTO        → update (최초 실행 시 테이블 자동 생성)
#
# ※ DOCKER_DATASOURCE_URL 은 .sample.env 기본값 그대로 사용 가능

# 2. 전체 스택 실행
docker compose up --build
```

**포트 요약**

| 서비스 | 호스트 포트 |
|--------|-------------|
| Nginx (API gateway) | 80 |
| MySQL | 3307 |
| Redis | 6380 |
| Prometheus | 9090 |
| Grafana | 3000 (admin / admin) |

Swagger UI: `http://localhost/swagger-ui/index.html`

---

## API Spec

모든 응답은 `{ status, code, message[, data] }` 구조입니다. `status`는 HTTP 상태 코드와 동일하게 설정됩니다.

### GET /api/v1/checkout/{productId}

주문서 조회. 상품 정보와 사용자 보유 포인트를 반환합니다.

| 항목 | 내용 |
|------|------|
| Method | GET |
| Path | `/api/v1/checkout/{productId}` |
| Query | `userId` (Long, required) |

**Response 200**
```json
{
  "status": 200,
  "code": "CHECKOUT_SUCCESS",
  "message": "주문서 조회 성공",
  "data": {
    "productId": "01JXXXXXXXXXXXXXXXXXX",
    "productName": "오션뷰 스위트",
    "price": 150000,
    "checkInTime": "2026-05-10T15:00:00+09:00",
    "checkOutTime": "2026-05-11T11:00:00+09:00",
    "openAt": "2026-05-10T00:00:00+09:00",
    "userPoint": 30000
  }
}
```

---

### POST /api/v1/booking

예약 및 결제. 재고 차감 → PG 승인 → 예약 확정 순서로 처리됩니다.

| 항목 | 내용 |
|------|------|
| Method | POST |
| Path | `/api/v1/booking` |
| Content-Type | `application/json` |

**Request Body**
```json
{
  "productId": "01JXXXXXXXXXXXXXXXXXX",
  "userId": 1001,
  "idempotencyKey": "user-1001-product-01JXX-20260510",
  "payments": [
    { "method": "CREDIT_CARD", "amount": 120000 },
    { "method": "POINT",       "amount": 30000  }
  ]
}
```

`payments[].method` 허용값: `CREDIT_CARD` | `Y_PAY` | `POINT`  
복합 결제: `CREDIT_CARD + POINT` 또는 `Y_PAY + POINT` 가능. `CREDIT_CARD + Y_PAY` 혼용 불가.

**Response 201**
```json
{
  "status": 201,
  "code": "BOOKING_SUCCESS",
  "message": "예약 및 결제 완료",
  "data": {
    "bookingId": "01JXXXXXXXXXXXXXXXXXX",
    "productId": "01JXXXXXXXXXXXXXXXXXX",
    "status": "CONFIRMED",
    "totalAmount": 150000,
    "confirmedAt": "2026-05-10T00:00:01.234Z"
  }
}
```

**Error Response**
```json
{ "status": 409, "code": "STOCK_SOLD_OUT",               "message": "재고가 모두 소진되었습니다." }
{ "status": 409, "code": "DUPLICATE_REQUEST",            "message": "이미 처리 중인 요청입니다." }
{ "status": 422, "code": "PAYMENT_FAILED",               "message": "결제에 실패했습니다." }
{ "status": 400, "code": "BOOKING_NOT_OPEN_YET",         "message": "아직 판매 시작 전입니다." }
{ "status": 400, "code": "INVALID_PAYMENT_COMBINATION",  "message": "올바르지 않은 결제 수단 조합입니다." }
{ "status": 503, "code": "LOCK_CONFLICT",                "message": "일시적으로 처리할 수 없습니다. 잠시 후 다시 시도해 주세요." }
```

---

## ERD

주문/결제 도메인 중심 테이블 정의입니다. DDL은 `spring.jpa.hibernate.ddl-auto` 설정으로 자동 생성됩니다.

```sql
-- 상품
CREATE TABLE product (
    id           VARCHAR(13)    NOT NULL COMMENT 'TSID',
    name         VARCHAR(255)   NOT NULL,
    price        DECIMAL(12, 2) NOT NULL,
    check_in_time  DATETIME     NOT NULL,
    check_out_time DATETIME     NOT NULL,
    open_at        DATETIME     NOT NULL COMMENT '판매 오픈 시각',
    created_at   DATETIME,
    updated_at   DATETIME,
    deleted_at   DATETIME,
    PRIMARY KEY (id),
    INDEX idx_product_open_at (open_at)
);

-- 재고
CREATE TABLE stock (
    product_id   VARCHAR(13) NOT NULL COMMENT 'product.id 참조',
    quantity     INT         NOT NULL COMMENT '잔여 수량',
    max_quantity INT         NOT NULL COMMENT '최대 수량 (복구 상한)',
    version      BIGINT               COMMENT '낙관적 락',
    created_at   DATETIME,
    updated_at   DATETIME,
    PRIMARY KEY (product_id)
);

-- 예약
CREATE TABLE booking (
    id           VARCHAR(13)  NOT NULL COMMENT 'TSID',
    user_id      BIGINT       NOT NULL,
    product_id   VARCHAR(13)  NOT NULL,
    status       VARCHAR(20)  NOT NULL COMMENT 'PENDING | CONFIRMED | FAILED',
    confirmed_at DATETIME              COMMENT '확정일시 (UTC)',
    created_at   DATETIME,
    updated_at   DATETIME,
    deleted_at   DATETIME,
    PRIMARY KEY (id),
    INDEX idx_booking_user_product_status (user_id, product_id, status)
);

-- 결제
CREATE TABLE payment (
    id                  VARCHAR(13)    NOT NULL COMMENT 'TSID',
    booking_id          VARCHAR(13)    NOT NULL,
    method              VARCHAR(20)    NOT NULL COMMENT 'CREDIT_CARD | Y_PAY | POINT',
    amount              DECIMAL(12, 2) NOT NULL,
    status              VARCHAR(20)    NOT NULL COMMENT 'REQUESTED | APPROVED | CONFIRMED | FAILED',
    pg_transaction_id   VARCHAR(255)            COMMENT 'PG사 거래번호',
    pg_idempotency_key  VARCHAR(255)   NOT NULL COMMENT 'PG 중복 청구 방지 키',
    created_at          DATETIME,
    updated_at          DATETIME,
    deleted_at          DATETIME,
    PRIMARY KEY (id),
    INDEX idx_payment_booking_id (booking_id),
    INDEX idx_payment_pg_idempotency_key (pg_idempotency_key)
);

-- 사용자 (포인트 조회 전용)
CREATE TABLE users (
    id    BIGINT NOT NULL AUTO_INCREMENT,
    point BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
```

관계 요약: `booking.product_id → product.id`, `payment.booking_id → booking.id`, `stock.product_id → product.id` (1:1)

---

## Package Structure

```
com.tenslots
├── adapter
│   ├── in
│   │   ├── scheduler
│   │   │   ├── StockWarmupScheduler.java    # cron: 오픈 10분 전 Redis warm-up
│   │   │   └── WarmupProductLoader.java
│   │   └── web
│   │       ├── BookingController.java
│   │       ├── CheckoutController.java
│   │       └── dto/BookingRequest.java
│   └── out
│       ├── payment
│       │   ├── CreditCardGatewayAdapter.java
│       │   ├── YPayGatewayAdapter.java
│       │   ├── PointGatewayAdapter.java
│       │   └── PaymentGatewayRouter.java
│       ├── persistence
│       │   ├── BookingPersistenceAdapter.java
│       │   ├── ConfirmBookingPersistenceAdapter.java
│       │   ├── PaymentPersistenceAdapter.java
│       │   ├── DbStockAdapter.java          # CB fallback, optimistic lock retry ×3
│       │   ├── StockDecreaseHelper.java     # @Transactional REQUIRES_NEW per attempt
│       │   ├── ProductPersistenceAdapter.java
│       │   └── UserPersistenceAdapter.java
│       └── redis
│           ├── CircuitBreakerStockAdapter.java  # @Primary — Redis→CB→DB 라우팅
│           ├── RedisStockAdapter.java           # Lua script (EXISTS+DECR, atomic)
│           ├── RedisIdempotencyAdapter.java     # SET NX 24h TTL
│           └── StockKeyNotFoundException.java
├── application
│   ├── port
│   │   ├── in
│   │   │   ├── BookingUseCase.java
│   │   │   ├── CheckoutUseCase.java
│   │   │   ├── BookingCommand.java
│   │   │   ├── BookingResponse.java
│   │   │   └── CheckoutResponse.java
│   │   └── out
│   │       ├── StockPort.java
│   │       ├── IdempotencyPort.java
│   │       ├── LoadProductPort.java
│   │       ├── SaveBookingPort.java
│   │       ├── ConfirmBookingPort.java
│   │       ├── SavePaymentPort.java
│   │       ├── PaymentGatewayPort.java
│   │       └── PaymentProcessorPort.java
│   └── service
│       ├── BookingService.java
│       ├── CheckoutService.java
│       └── CompositePaymentProcessor.java
├── domain
│   ├── booking   (Booking, BookingStatus)
│   ├── common    (BaseEntity, DomainConstants, InvalidStateTransitionException)
│   ├── payment   (Payment, PaymentMethod, PaymentStatus, InvalidPaymentCombinationException)
│   ├── product   (Product)
│   ├── stock     (Stock, StockOverflowException)
│   └── user      (User)
└── global
    ├── api       (ApiResponse, SuccessResponse, FailureResponse, ErrorCode, SuccessCode,
    │              GlobalExceptionHandler)
    ├── config    (ClockConfig, RedisConfig, JpaAuditingConfig, SchedulerConfig, SwaggerConfig)
    └── exception (BusinessException, PaymentTimeoutException)
```
