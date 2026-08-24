# Bước 1: Dùng Maven và Java 21 để build mã nguồn thành file .jar
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Bước 2: Dùng môi trường Java 21 gọn nhẹ để chạy file .jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file .jar vừa build được ở Bước 1 sang
COPY --from=builder /app/target/*.jar app.jar

# Mở cổng 8080
EXPOSE 8080

# Lệnh khởi chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]