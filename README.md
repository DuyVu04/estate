# 🏢 Real Estate Enterprise Platform & AI RAG System

![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)
![Spring Boot 4.1](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg?style=flat-square&logo=springboot)
![Spring AI 2.0](https://img.shields.io/badge/Spring%20AI-2.0.1-blue.svg?style=flat-square&logo=spring)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16%20%2B%20pgvector-blue.svg?style=flat-square&logo=postgresql)
![Redis 7](https://img.shields.io/badge/Redis-7%20%2B%20Redisson-red.svg?style=flat-square&logo=redis)
![RabbitMQ 3](https://img.shields.io/badge/RabbitMQ-3.13-orange.svg?style=flat-square&logo=rabbitmq)
![Prometheus](https://img.shields.io/badge/Prometheus-v2.51.0-red.svg?style=flat-square&logo=prometheus)
![Grafana](https://img.shields.io/badge/Grafana-10.4.1-orange.svg?style=flat-square&logo=grafana)
![Docker](https://img.shields.io/badge/Docker-Containers-blue.svg?style=flat-square&logo=docker)

Hệ thống Backend Nền tảng Bất Động Sản Doanh Nghiệp (Enterprise Real Estate Platform) được xây dựng trên nền tảng **Java 21**, **Spring Boot 4.1.0** và **Spring AI 2.0.1**. Hệ thống tích hợp **PostgreSQL pgvector (HNSW Index)**, trợ lý ảo tư vấn **RAG Chatbot chống ảo giác**, xử lý hàng đợi sự kiện bất đồng bộ **RabbitMQ**, khóa phân tán **Redisson Distributed Lock**, thanh toán **Stripe Gateway API**, và giám sát hiệu năng thời gian thực với **Prometheus & Grafana**.

---

## 🚀 Các Tính Năng Nổi Bật (Core Features)

### 1. 🤖 Trí Tuệ Nhân Tạo & Vector Database (Spring AI & pgvector)
- **HNSW Vector Search**: Tích hợp extension `pgvector` trên PostgreSQL với chỉ mục đồ thị phân tầng **HNSW (Hierarchical Navigable Small World)** và độ đo khoảng cách **Cosine Distance (`<=>`)**, hỗ trợ vector 768 chiều.
- **RAG Architecture (Retrieval-Augmented Generation)**: Trợ lý AI tư vấn bất động sản thông minh sử dụng **Spring AI `ChatClient`** kết hợp **Anti-Hallucination Guardrails Prompting**, đảm bảo câu trả lời trung thực và dẫn chứng 100% từ dữ liệu thực tế.
- **Hybrid Search**: Kết hợp đồng thời bộ lọc điều kiện SQL (Thành phố, Khoảng giá, Trạng thái) và tìm kiếm ngữ nghĩa tự nhiên trong một truy vấn duy nhất.
- **Event-Driven Vector Ingestion**: Tự động sinh và cập nhật vector embedding ngầm qua RabbitMQ Consumer với cơ chế **`TransactionSynchronization afterCommit`**, loại bỏ hoàn toàn hiện tượng Race Condition giữa Database và Message Queue.

### 2. 🔐 Bảo mật & Phân quyền Hạt mịn (Security & RBAC)
- **Stateless Authentication**: Sử dụng JWT (JSON Web Token) mở rộng nhúng trực tiếp **User Roles & Authorities** vào claims payload.
- **Phân quyền Role-Based Access Control (RBAC)**: Kiểm soát truy cập API theo vai trò và danh sách quyền chi tiết (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_REALTOR`) qua `@PreAuthorize`.
- **API Rate Limiting**: Tích hợp bộ giới hạn tần suất truy cập qua Redis & Bucket4j để phòng chống tấn công brute-force và lạm dụng tài nguyên API.

### 3. 🏠 Quản lý Đặt Cọc & Khóa Phân Tán (Concurrency & Redisson Lock)
- **Distributed Lock với Redisson**: Ngăn chặn triệt để hiện tượng Overbooking / Race Condition khi nhiều người dùng cùng bấm đặt cọc một bất động sản tại cùng một thời điểm.
- **State Machine Workflow**: Quản lý vòng đời giữ chỗ bất động sản (`PENDING` ➔ `CONFIRMED` / `CANCELLED` / `EXPIRED`) kết hợp Scheduler tự động giải phóng căn nhà về `AVAILABLE` khi quá hạn thanh toán cọc.

### 4. 💳 Cổng Thanh Toán & Webhook Idempotency (Stripe Gateway)
- **Stripe Checkout Session**: Tạo phiên thanh toán tiền cọc an toàn qua Stripe API.
- **Idempotent Webhook Processing**: Kiểm soát khóa Idempotency Key trong Redis & Database, đảm bảo chỉ xử lý cộng tiền/xác nhận cọc đúng 1 lần duy nhất dù Stripe gửi lặp Webhook.

### 5. ✉️ Hàng Đợi Bất Đồng Bộ & Xử Lý Lỗi (RabbitMQ & DLQ)
- **Asynchronous Messaging**: Gửi email xác nhận giao dịch, hóa đơn và tính toán vector embedding bất đồng bộ mà không block HTTP Request chính.
- **Dead Letter Queue (DLQ)**: Thiết lập cơ chế tự động thử lại (Retry Exchange) và chuyển tiếp tin nhắn lỗi vào Dead Letter Queue để phục vụ giám sát và phục hồi lỗi.

### 6. 📊 Giám Sát Hệ Thống Toàn Diện (Prometheus & Grafana Observability)
- **Metrics Scraping**: Tự động thu thập số liệu vận hành hệ thống qua Spring Boot Actuator (`/api/actuator/prometheus`).
- **Real-time Monitoring**: Dashboard Grafana trực quan hóa thời gian thực về JVM Memory, Connection Pool (HikariCP), Throughput (RPS), Error Rate và API Latency (P95/P99).

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

| Thành phần | Công nghệ |
| :--- | :--- |
| **Ngôn ngữ & Framework** | Java 21, Spring Boot 4.1.0, Spring Framework 7 |
| **AI Framework & Vector Engine** | Spring AI 2.0.1 (`ChatClient`), PostgreSQL `pgvector` (HNSW Index, 768-dim) |
| **Kiến trúc DTO & Mapping** | 100% Java `record`, Lombok `@Builder`, MapStruct 1.6.3 |
| **Cơ sở dữ liệu & Migration** | PostgreSQL 16, Flyway Migration (V1 ➔ V11) |
| **Bộ nhớ đệm & Khóa phân tán** | Redis 7, Redisson 3.51.0, Spring Cache (`@Cacheable`, `@CacheEvict`) |
| **Message Broker** | RabbitMQ 3.13 (Management Console, DLQ, Routing Keys) |
| **Cổng thanh toán & Email** | Stripe Java SDK, SendGrid API |
| **Bảo mật & Rate Limiting** | Spring Security 6, JJWT, Bucket4j |
| **Giám sát (Monitoring)** | Prometheus v2.51.0, Grafana 10.4.1, Micrometer Actuator |
| **Đóng gói & Container** | Docker, Docker Compose, Multi-stage Dockerfile |
| **Code Formatting & Test** | Spotless Plugin (`googleJavaFormat`), JUnit 5, Mockito |

---

## 📁 Cấu Trúc Dự Án (Project Structure)

```text
backend/
├── src/main/java/com/project/estate/
│   ├── common/               # ApiResponse<T>, GlobalExceptionHandler, ErrorCode
│   ├── config/               # SecurityConfig, RedisConfig, RabbitMQConfig, RedissonConfig
│   ├── controller/           # REST API Controllers (Auth, Property, PropertyAi, RealEstateChat, Payment)
│   ├── dto/                  # Requests & Responses (Java records)
│   ├── entity/               # JPA Entities (User, Property, Reservation, Payment, Role, Permission)
│   ├── enums/                # RoleType, PropertyType, PropertyStatus, PaymentStatus Enums
│   ├── exception/            # Custom AccessDeniedHandler & AuthenticationEntryPoint
│   ├── mapper/               # MapStruct Mappers (PropertyMapper, UserMapper, PaymentMapper)
│   ├── messaging/            # RabbitMQ Producers & Consumers (VectorConsumer, EmailConsumer)
│   ├── repository/           # Spring Data JPA & pgvector Native Query Repositories
│   ├── security/             # JwtTokenProvider, JwtAuthenticationFilter
│   └── service/              # Core Business Logic (PropertyAiService, RealEstateRagAdvisorService, ReservationService)
├── src/main/resources/
│   ├── db/migration/         # Flyway SQL Migration Scripts (V1__... ➔ V11__enable_pgvector.sql)
│   ├── application.yml       # Base Configuration
│   ├── application-dev.yml   # Development Profile Configuration
│   └── application-test.yml  # Test Profile Configuration
├── docker-compose.yml        # Multi-container Setup (Postgres+pgvector, Redis, RabbitMQ, Prometheus, Grafana, Qdrant)
├── Dockerfile                # Multi-stage Maven Build Dockerfile
├── prometheus.yml            # Prometheus Scraping Configuration
└── pom.xml                   # Maven Dependencies (Spring Boot 4.1.0 & Spring AI 2.0.1 BOM)
```

---

## ⚡ Hướng Dẫn Khởi Động Nhanh (Quick Start)

### 📋 Yêu cầu hệ thống (Prerequisites)
- **Java Development Kit (JDK)**: Version 21 trở lên.
- **Maven**: Version 3.9+ (hoặc sử dụng wrapper `./mvnw` đi kèm).
- **Docker & Docker Desktop**: Đã cài đặt và đang chạy.

---

### Cách 1: Chạy toàn bộ hệ thống bằng Docker Compose (Khuyên dùng)

1. **Clone repository về máy**:
   ```bash
   git clone https://github.com/DuyVu04/estate.git
   cd estate/backend
   ```

2. **Cấu hình file môi trường**:
   Tạo file `.env` từ file mẫu `.env.example`:
   ```bash
   cp .env.example .env
   ```

3. **Khởi động tất cả dịch vụ trong 1 lệnh duy nhất**:
   ```bash
   docker-compose up --build -d
   ```

4. **Kiểm tra danh sách container đang chạy**:
   ```bash
   docker ps
   ```

---

### Cách 2: Chạy Spring Boot trên Máy Cục Bộ + Hạ Tầng bằng Docker

1. **Khởi động các dịch vụ hạ tầng (Database, Redis, RabbitMQ, Prometheus, Grafana)**:
   ```bash
   docker-compose up -d db redis rabbitmq prometheus grafana
   ```

2. **Biên dịch và chạy ứng dụng Spring Boot**:
   ```bash
   ./mvnw spring-boot:run
   ```

Ứng dụng Backend sẽ chạy tại địa chỉ: `http://localhost:8080/api`

---

## 📊 Hệ Thống Giám Sát & Quản Lý (Dashboard Links)

| Dịch vụ | Đường dẫn (URL) | Thông tin đăng nhập |
| :--- | :--- | :--- |
| **Spring Boot API** | `http://localhost:8080/api` | - |
| **Swagger UI (Docs)** | `http://localhost:8080/api/swagger-ui/index.html` | - |
| **Prometheus Metrics** | `http://localhost:9090/targets` | Không yêu cầu |
| **Grafana Dashboard** | `http://localhost:3000` | **User**: `grafana` \| **Pass**: `password` |
| **RabbitMQ Console** | `http://localhost:15672` | **User**: `guest` \| **Pass**: `guest` |
| **PostgreSQL Database** | `localhost:5432` | **DB**: `my-estate` \| **User**: `duyvu` |
| **Qdrant Dashboard** | `http://localhost:6333/dashboard` | Không yêu cầu |

---

## 🧪 Kiểm Thử & Định Dạng Code (Testing & Quality)

### 1. Chạy toàn bộ bộ kiểm thử tự động
```bash
./mvnw test
```

### 2. Định dạng Code tự động chuẩn Google (Spotless Format)
```bash
./mvnw spotless:apply
```
