# 🛠 Complaint Service

## 📌 Description

**Complaint Service** is a reactive REST API for managing customer complaints. It allows:

- creating new complaints
- updating complaint content
- retrieving individual or filtered complaints

Each complaint includes:

- product ID
- content
- creation and update timestamps
- complainant ID
- country (detected by IP)
- complaint counter

Complaints are **unique by** `productId` and `complainantId`.

---

## ⚙️ Technologies

- Java 21
- Spring Boot 3 (WebFlux)
- MongoDB (Reactive)
- WebClient (geolocation)
- Testcontainers
- Spock Framework
- Swagger / OpenAPI

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven
- Docker (for MongoDB)

### Build the app
```bash
./mvnw clean install
```

### Start MongoDB
```bash
docker-compose up -d
```

### Run the application
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 🔧 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGODB_USERNAME` | admin | MongoDB user |
| `MONGODB_PASSWORD` | admin123 | MongoDB password |
| `MONGODB_HOST` | localhost | MongoDB host |
| `MONGODB_PORT` | 27017 | MongoDB port |
| `MONGODB_DATABASE` | complaints | Database name |
| `MONGODB_AUTH_DATABASE` | admin | Auth database |
| `APPLICATION_GEOLOCATION_BASE_URL` | http://ip-api.com | IP geolocation API |

Export manually or use a `.env` file.

---

## 📚 API Documentation

Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

---

## ✅ Tests

Run unit/integration tests:
```bash
./mvnw test
```

---

## 📁 Project Structure

- `api/` – DTOs and API interfaces
- `service/` – business logic
- `repository/` – MongoDB access
- `client/` – geolocation API client
- `config/` – configuration
- `exception/` – error handling
- `test/` – unit/integration tests

---

## ✍️ Recruitment Task Summary

> The goal was to implement a REST service for managing customer complaints. Each complaint is unique by `productId` and `complainantId`. If a duplicate is submitted, the complaint counter is incremented. Country is auto-detected from the client's IP using a public geolocation API (https://ip-api.com/).

---
