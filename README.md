# 🏢 Real Estate System Backend Service

![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)
![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg?style=flat-square&logo=springboot)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=flat-square&logo=postgresql)
![Redis 7](https://img.shields.io/badge/Redis-7-red.svg?style=flat-square&logo=redis)
![RabbitMQ 3](https://img.shields.io/badge/RabbitMQ-3.13-orange.svg?style=flat-square&logo=rabbitmq)
![Prometheus](https://img.shields.io/badge/Prometheus-v2.51.0-red.svg?style=flat-square&logo=prometheus)
![Grafana](https://img.shields.io/badge/Grafana-10.4.1-orange.svg?style=flat-square&logo=grafana)
![Docker](https://img.shields.io/badge/Docker-Containers-blue.svg?style=flat-square&logo=docker)

Hệ thống Backend Dịch vụ Bất động sản cấp Doanh nghiệp (Enterprise Real Estate Platform) được xây dựng trên nền tảng **Java 21** và **Spring Boot 3**, tích hợp kiến trúc **Micro-services ready**, xử lý thanh toán **Stripe API**, hàng đợi thông điệp bất đồng bộ **RabbitMQ**, hệ thống bộ nhớ đệm **Redis Cache**, và hệ thống giám sát toàn diện **Prometheus & Grafana**.

---

## 🚀 Các Tính Năng Nổi Bật (Core Features)

### 1. 🔐 Bảo mật & Phân quyền (Security & JWT Authorization)
- **Stateless Authentication**: Sử dụng JWT (JSON Web Token) mở rộng nhúng trực tiếp **User Roles & Authorities** vào token claims payload.
- **Phân quyền hạt mịn**: Kiểm soát truy cập API theo vai trò (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_REALTOR`) với `@PreAuthorize("hasRole(...)")`.
- **Tự động cấp quyền**: Tự động gán `ROLE_USER` mặc định khi người dùng mới đăng ký tài khoản qua Flyway & DataInitializer.

### 2. 🏠 Quản lý Bất động sản & Đặt cọc (Property & Reservation Workflow)
- **Workflow State Machine**: Xử lý trạng thái đặt cọc theo luồng nghiệp vụ chuẩn (`PENDING` ➔ `CONFIRMED` / `CANCELLED` / `EXPIRED`).
- **Xử lý Concurrency & Expiration**: Tránh ghi đè trùng lặp khi nhiều người dùng cùng đặt cọc một bất động sản; tự động giải phóng bất động sản về `AVAILABLE` khi hết hạn cọc bằng Scheduler.

### 3. 💳 Tích hợp Thanh toán & Webhook Idempotency (Stripe Gateway)
- **Stripe Payment Gateway**: Tạo thanh toán cọc an toàn qua Stripe API.
- **Idempotent Webhook Processing**: Theo dõi khóa Idempotency Key trong Redis & Database, đảm bảo xử lý chính xác tuyệt đối ngay cả khi Stripe gửi trùng lặp Webhook.

### 4. ✉️ Hàng đợi Bất đồng bộ & Xử lý Lỗi (RabbitMQ & DLQ)
- **Asynchronous Messaging**: Gửi thông báo Email xác nhận đặt cọc / hóa đơn thanh toán bất đồng bộ qua RabbitMQ Consumer.
- **Resilience Strategy**: Tích hợp **Dead Letter Queue (DLQ)** và cơ chế tự động thử lại (Automated Retries) khi gửi email thất bại.

### 5. 📊 Giám sát Hệ thống Toàn diện (Prometheus & Grafana Observability)
- **Metrics Scraping**: Tự động xuất các thông số vận hành qua Spring Boot Actuator (`/api/actuator/prometheus`).
- **Visual Dashboards**: Tự động cấu hình Data source và Dashboard chuẩn trên Grafana, giám sát thời gian thực CPU, RAM (JVM Heap/Non-Heap), Connection Pool (HikariCP), Request Rate (RPS), và Latency.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

| Thành phần | Công nghệ |
| :--- | :--- |
| **Ngôn ngữ & Framework** | Java 21, Spring Boot 3.4.2, Spring Data JPA |
| **Kiến trúc DTO** | 100% Java `record` với `@Builder` & MapStruct |
| **Cơ sở dữ liệu** | PostgreSQL 16, Flyway Migration (V1 -> V9) |
| **Bộ nhớ đệm (Cache)** | Redis 7, Spring Cache (`@Cacheable`, `@CacheEvict`) |
| **Message Broker** | RabbitMQ 3 (với Management Console) |
| **Cổng thanh toán** | Stripe Java SDK |
| **Bảo mật** | Spring Security 6, JJWT (io.jsonwebtoken) |
| **Giám sát (Monitoring)** | Prometheus v2.51.0, Grafana 10.4.1 |
| **Đóng gói & Container** | Docker, Docker Compose, Multi-stage Dockerfile |
| **Code Formatting & Test** | Spotless Plugin (`googleJavaFormat`), JUnit 5, Mockito |

---

## 📁 Cấu Trúc Dự Án (Project Structure)

```text
backend/
├── src/main/java/com/project/estate/
│   ├── batch/                # Spring Batch Jobs & Processing
│   ├── common/               # ApiResponse<T>, Global Exceptions
│   ├── config/               # RabbitConfig, SecurityConfig, RedisConfig
│   ├── controller/           # REST API Controllers (User, Property, Payment,...)
│   ├── dto/                  # Requests & Responses (Java records)
│   ├── entity/               # JPA Entities (User, Property, Reservation, Payment)
│   ├── enums/                # Role, Status, ErrorCode Enums
│   ├── exception/            # Custom AccessDenied & Authentication Handlers
│   ├── mapper/               # MapStruct Mappers
│   ├── messaging/            # RabbitMQ Producers & Consumers (EmailConsumer)
│   ├── repository/           # Spring Data JPA Repositories
│   ├── security/             # JwtAuthenticationFilter, CustomUserDetailsService
│   └── service/              # Core Business Logic Services
├── src/main/resources/
│   ├── db/migration/         # Flyway SQL Migration Scripts (V1__... -> V9__...)
│   ├── application.yml       # Configuration File
│   └── application-dev.example.yml # Development Config Template
├── docker-compose.yml        # Multi-container Setup (DB, Redis, RabbitMQ, Prometheus, Grafana, App)
├── Dockerfile                # Multi-stage Maven Build Dockerfile
├── prometheus.yml            # Prometheus Dual Scraping Configuration
├── grafana/provisioning/     # Grafana Auto Datasource Provisioning
└── pom.xml                   # Maven Dependencies & Build Configuration
```

---

## ⚡ Hướng Dẫn Khởi Động Nhanh (Quick Start)

### 📋 Yêu cầu hệ thống (Prerequisites)
- **Java Development Kit (JDK)**: Version 21 trở lên.
- **Maven**: Version 3.9+ (hoặc dùng `mvnw` đi kèm).
- **Docker & Docker Desktop**: Đã cài đặt và đang chạy.

---

### Cách 1: Chạy toàn bộ hệ thống bằng Docker Compose (Khuyên dùng)

1. **Clone repository về máy**:
   ```bash
   git clone https://github.com/DuyVu04/estate.git
   cd estate/backend
   ```

2. **Khởi động tất cả dịch vụ trong 1 lệnh duy nhất**:
   ```bash
   docker-compose up --build -d
   ```

3. **Kiểm tra danh sách container đang chạy**:
   ```bash
   docker ps
   ```

---

### Cách 2: Chạy Spring Boot trên Máy Thật (Local Host) + Khởi động Hạ Tầng bằng Docker

Nếu bạn muốn debug trực tiếp trên IDE (IntelliJ IDEA / VS Code / Eclipse):

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

Sau khi khởi động hệ thống thành công, bạn có thể truy cập các đường dẫn quản trị bên dưới:

| Dịch vụ | Đường dẫn (URL) | Thông tin đăng nhập |
| :--- | :--- | :--- |
| **Spring Boot API** | `http://localhost:8080/api` | - |
| **Swagger UI (Docs)** | `http://localhost:8080/api/swagger-ui/index.html` | - |
| **Prometheus Metrics** | `http://localhost:9090/targets` | Không yêu cầu |
| **Grafana Dashboard** | `http://localhost:3000` | **User**: `grafana` \| **Pass**: `password` |
| **RabbitMQ Console** | `http://localhost:15672` | **User**: `guest` \| **Pass**: `guest` |
| **PostgreSQL DB** | `localhost:5432` | **DB**: `estatedb` \| **User**: `postgres` \| **Pass**: `postgres` |

---

### 🎨 Cấu hình Grafana Dashboard trong 10 giây:
1. Đăng nhập vào **[http://localhost:3000](http://localhost:3000)** với tài khoản `grafana` / `password`.
2. Vào **Dashboards** ➔ Chọn **Import**.
3. Nhập mã ID Dashboard Spring Boot: **`4701`** (hoặc **`11378`**) ➔ Bấm **Load**.
4. Chọn Data Source **Prometheus** ➔ Bấm **Import**.
5. Trên thanh lọc trên cùng, chọn `Instance` ➔ **`host.docker.internal:8080`** (Local) hoặc **`estate-service:8080`** (Docker).

---

## 🧪 Kiểm Thử & Định Dạng Code (Testing & Quality)

### 1. Chạy toàn bộ bộ kiểm thử (Unit & Integration Tests)
```bash
./mvnw test
```

### 2. Định dạng Code tự động chuẩn Google (Spotless Format)
```bash
./mvnw spotless:apply
```

---

## 📄 Giấy Phép (License)

Dự án được phát hành theo giấy phép **MIT License**.

---
*Phát triển với ❤️ bởi **DuyVu04** & Google DeepMind Antigravity Team.*
