# 💳 OmniPoint (LoyaltyEngine)

**Event-driven микросервисная система лояльности и кэшбэка**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Kafka-3.x-231F20?logo=apache-kafka)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)](https://www.docker.com/)

---

## 📋 Описание

**OmniPoint** — это высоконагруженная **event-driven система лояльности**, которая обрабатывает транзакции, рассчитывает кэшбэк по динамическим правилам и управляет балансами пользователей с гарантией консистентности данных в распределённой среде.

### Основные возможности

- 💰 **Автоматический расчёт кэшбэка** по настраиваемым правилам (базовый + категориальный)
- 🔄 **SAGA Choreography pattern** для распределённых транзакций
- 🛡️ **Optimistic Locking** для предотвращения Race Condition
- 🚀 **Apache Kafka** для асинхронной обработки событий
- 🔥 **Rate Limiter** для защиты публичных API от перегрузки
- 💾 **Redis кэширование** с TTL для снижения нагрузки на БД
- 🔁 **Kafka resilience**: Retry mechanism + Dead Letter Queue
- 📊 **Идемпотентность** операций через deduplication
- 🔐 **Audit Trail** — полная история изменений баланса

---

### 🔄 SAGA Flow

```
1. TransactionService
   ↓ (валидация, deduplication)
   ↓ публикует: transaction.created
   
2. RuleEngineService
   ↓ (расчёт кэшбэка по правилам)
   ↓ публикует: points.calculated
   
3. WalletService
   ↓ (атомарное обновление баланса)
   ├─ SUCCESS → баланс обновлён
   └─ FAILURE → публикует: points.failed (компенсация)
```

---

## 🛠️ Технологический стек

### Backend
- **Java 21** (Records, Pattern Matching, Virtual Threads ready)
- **Spring Boot 3.x**
- **Spring Data JPA** (Hibernate)
- **Spring Kafka** (Consumer/Producer API)
- **Spring Cache** (Redis integration)
- **Spring Validation** (Bean Validation)

### Microservices Architecture
- **SAGA Choreography pattern** (распределённые транзакции)
- **Onion Architecture** (Entity → Domain → DTO layers)
- **Domain-Driven Design** principles

### Data & Messaging
- **Apache Kafka 3.x**
  - Consumer Groups для параллельной обработки
  - Retry mechanism с exponential backoff (1s, 2s, 4s)
  - Dead Letter Queue (DLQ) для failed messages
  - Идемпотентные producers
- **PostgreSQL 16**
  - JPA/Hibernate ORM
  - Optimistic Locking через `@Version`
  - B-tree индексы для быстрого поиска
- **Redis 7.x**
  - Кэширование правил (TTL 5 минут)
  - Eviction policy: `allkeys-lru`

### Resilience & Performance
- **Bucket4j** (Rate Limiting с Token Bucket algorithm)
- **Optimistic Locking** (предотвращение lost updates)
- **BigDecimal** для финансовых расчётов (RoundingMode.HALF_UP)
- **Retry mechanism** для Kafka consumers
- **Connection pooling** (HikariCP)

### Tools & DevOps
- **Docker** + **Docker Compose**
- **MapStruct** (маппинг между слоями)
- **Lombok** (boilerplate reduction)
- **Maven** (multi-module project)

### Testing
- **JUnit 5**
- **Mockito**
- **Testcontainers** (integration tests с реальными Kafka, PostgreSQL, Redis)
- **AssertJ** (fluent assertions)

---

## 🚀 Быстрый старт

### Требования
- Java 21+
- Docker & Docker Compose
- Maven 3.8+

### Запуск

1. **Клонируйте репозиторий**
```bash
git clone https://github.com/maydeixxx/loyaltyEngine.git
cd loyaltyEngine
```

2. **Запустите инфраструктуру (Kafka, PostgreSQL, Redis)**
```bash
docker-compose up -d
```

Это запустит:
- **Kafka** + Zookeeper (порты 9092, 2181)
- **PostgreSQL** (порт 5432)
- **Redis** (порт 6379)

3. **Соберите все модули**
```bash
mvn clean install
```

4. **Запустите сервисы**

```bash
# TransactionService (порт 8080)
cd transaction-service
mvn spring-boot:run

# RuleEngineService (порт 8081)
cd ../rule-engine-service
mvn spring-boot:run

# WalletService (порт 8082)
cd ../wallet-service
mvn spring-boot:run
```

5. **Проверьте работу**
```bash
# Health check
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```
---

## 👤 Автор

**Александр Карев**

- Telegram: [@amaevalx](https://t.me/amaevalx)
- Email: amaevalx@gmail.com
- GitHub: [@maydeixxx](https://github.com/maydeixxx)

---
