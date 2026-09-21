# 💳 OmniPoint (LoyaltyEngine)

**Высоконагруженная Event-Driven микросервисная система лояльности и начисления кэшбэка с адаптивным веб-интерфейсом**

[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.x-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025.x-6DB33F?logo=spring)](https://spring.io/projects/spring-cloud)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-6-646CFF?logo=vite)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-v4-38B2AC?logo=tailwind-css)](https://tailwindcss.com/)
[![Kafka](https://img.shields.io/badge/Kafka-3.x-231F20?logo=apache-kafka)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-316192?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)](https://www.docker.com/)

---

## 📋 Описание проекта

**OmniPoint (LoyaltyEngine)** — распределённая event-driven платформа для программ лояльности, процессинга покупок и динамического начисления кэшбэка. Архитектура гарантирует консистентность данных при высоких нагрузках, изоляцию доменов и масштабируемость каждого сервиса.

Система включает в себя **6 микросервисов**, инфраструктуру обмена сообщениями (Kafka + Zookeeper), распределенный кэш (Redis), 4 изолированные базы данных PostgreSQL, распределенный трейсинг (Zipkin) и современный **SPA-фронтенд** на React 19 с полной поддержкой мобильных устройств.

---

### 🌟 Ключевые возможности

- 💰 **Динамический расчёт кэшбэка**: базовый процент + настраиваемые бонусные правила по категориям товаров (`cafe`, `groceries`, `electronics`, `apparel`, `auto`, `pharmacy`) с периодами действия.
- 🔄 **SAGA Choreography & Transactional Outbox**: распределенные транзакции через Kafka с защитой от потери событий и гарантией Eventual Consistency.
- 🛡️ **Идемпотентность и Deduplication**: сквозной заголовок `X-IDEMPOTENCY-KEY` (UUID v4) предотвращает дублирование списаний и чеков.
- ⚡ **Redis Caching**: кэширование правил начисления кэшбэка для минимизации latency при расчетах.
- 🔁 **Kafka Resilience**: автоматический Retry механизм с экспоненциальным backoff и отправкой проблемных сообщений в Dead Letter Queue (DLQ).
- 🔐 **Безопасность и RBAC**: аутентификация через JWT (HMAC-SHA256), роли `ROLE_USER` и `ROLE_ADMIN`, проверка владения ресурсами через Spring Security SpEL.
- 📱 **Современный Web-интерфейс**: адаптивный личный кабинет клиента (баланс, чеки, конструктор покупки со списанием баллов) и панель администратора для управления правилами и блокировкой кошельков.

---

## 🏛️ Архитектура системы

```
                             ┌──────────────────────────────┐
                             │       Web Frontend           │
                             │  (React 19 + TypeScript)     │
                             └──────────────┬───────────────┘
                                            │ HTTP / REST
                                            ▼
                             ┌──────────────────────────────┐
                             │    Spring Cloud Gateway      │  :8080
                             │ (CORS, JWT AuthFilter, Rate) │
                             └──────┬───────────────┬───────┘
                                    │               │
            ┌───────────────────────┼───────────────┼───────────────────────┐
            │ Service Discovery     │ lb://         │ lb://                 │ lb://
            ▼                       ▼               ▼                       ▼
   ┌─────────────────┐    ┌────────────────┐ ┌─────────────────┐ ┌─────────────────────┐
   │  Eureka Server  │    │  UserService   │ │ TransactionSvc  │ │  RuleEngineService  │
   │      :8761      │    │     :8084      │ │      :8081      │ │        :8083        │
   └─────────────────┘    └───────┬────────┘ └────────┬────────┘ └──────────┬──────────┘
                                  │                   │                     │
                                  │ Outbox            │ Outbox              │ Redis Cache
                                  ▼                   ▼                     ▼
                             ┌──────────────────────────────────────────────────────────┐
                             │                   Apache Kafka (Broker)                  │
                             │  Topics: user_created | transaction_created              │
                             │          points_calculated | points_failed               │
                             └────────────────────────┬─────────────────────────────────┘
                                                      │
                                                      ▼
                                           ┌─────────────────────┐
                                           │    WalletService    │
                                           │        :8082        │
                                           └─────────────────────┘
```

---

### 🔄 Event Flows

#### 1. Регистрация и инициализация кошелька:
```
UserService (регистрация) 
   ↓ Transactional Outbox
Kafka: topic [user_created] 
   ↓ Consumer
WalletService (автоматическое создание бонусного кошелька с нулевым балансом)
```

#### 2. Процессинг покупки и начисление кэшбэка (SAGA):
```
1. Client → POST /api/v1/transactions (X-IDEMPOTENCY-KEY, items, useCashback)
2. TransactionService
   ↓ Валидация суммы и товаров, проверка идемпотентности
   ↓ Сохранение транзакции в статусе NEW
   ↓ Outbox Event → Kafka: topic [transaction_created]

3. RuleEngineService
   ↓ Consumer [transaction_created]
   ↓ Поиск активных категорий кэшбэка в Redis / БД
   ↓ Расчет баллов кэшбэка
   ↓ Kafka: topic [points_calculated]

4. WalletService
   ↓ Consumer [points_calculated]
   ├─ SUCCESS: Начисление баллов (CREDIT) или списание (DEBIT) 
   │           → Баланс обновлен → topic [transaction_handled]
   └─ FAILURE: Недостаточно средств для списания
               → topic [points_failed] (SAGA компенсация)
```

---

## 🛠️ Состав микросервисов и порты

| Сервис | Порт | База данных / Хранилище | Описание |
| :--- | :--- | :--- | :--- |
| **Frontend** | `3000` (dev) / `80` (prod) | — | SPA на React 19, TypeScript, Vite, Tailwind CSS v4 |
| **GatewayService** | `8080` | — | Spring Cloud Gateway, единая точка входа, JWT-фильтрация |
| **EurekaServer** | `8761` | — | Netflix Eureka Service Discovery |
| **UserService** | `8084` | PostgreSQL `users_db` (`:5435`) | Регистрация, логин (выдача JWT), профиль, BCrypt |
| **TransactionService** | `8081` | PostgreSQL `loyalty_db` (`:5432`) | Чеки, корзина покупок, идемпотентность |
| **WalletService** | `8082` | PostgreSQL `wallet_db` (`:5434`) | Бонусный баланс, аудит операций, блокировка |
| **RuleEngineService** | `8083` | PostgreSQL `ruleEngine_db` (`:5433`), Redis (`:6379`) | Категории и процентные ставки кэшбэка, кэш правил |
| **Kafka & Zookeeper** | `9092`, `2181` | — | Асинхронная шина событий |
| **Zipkin** | `9411` | — | Распределенный трейсинг запросов |

---

## 🚀 Инструкция по запуску

### Системные требования
- **Java 21** или выше
- **Node.js 20+** и **npm**
- **Docker** и **Docker Compose**

---

### Вариант 1. Запуск всего стека в Docker

Все базы данных, брокер Kafka, Redis, все 6 Java-микросервисов и Nginx с собранным фронтендом поднимаются **двумя командами**:

1. **Клонируйте проект:**
   ```bash
   git clone https://github.com/maydeixxx/loyaltyEngine.git
   cd loyaltyEngine
   git checkout frontend
   ```

2. **Соберите JAR-файлы всех микросервисов:**
   ```bash
   ./mvnw clean package -DskipTests
   ```

3. **Запустите контейнеры:**
   ```bash
   docker compose -f docker-compose.prod.yml up -d --build
   ```

4. **Готово! Приложение доступно:**
   - 🌐 **Веб-интерфейс**: [http://localhost](http://localhost) (или `http://IP_СЕРВЕРА`)
   - 🚪 **API Gateway**: [http://localhost:8080](http://localhost:8080)
   - 🔍 **Eureka Dashboard**: [http://localhost:8761](http://localhost:8761)

---

### Вариант 2. Локальный запуск для разработки (Local Dev)

Если вы разрабатываете и отлаживаете код на своем компьютере:

#### Шаг 1. Запустите инфраструктуру (БД, Kafka, Redis, Zipkin):
```bash
docker compose up -d
```
> Запустит 4 базы данных PostgreSQL на портах `5432-5435`, Redis на `6379`, Kafka на `9092` и Zipkin на `9411`.

#### Шаг 2. Соберите проект:
```bash
./mvnw clean install -DskipTests
```

#### Шаг 3. Запустите бэкенд-сервисы (по порядку):
Рекомендуемый порядок запуска:
1. **EurekaServer**:
   ```bash
   cd EurekaServer && ../mvnw spring-boot:run
   ```
2. **GatewayService**:
   ```bash
   cd GatewayService && ../mvnw spring-boot:run
   ```
3. **UserService**, **WalletService**, **TransactionService**, **RuleEngineService** (каждый в своем терминале или через IntelliJ IDEA Run Configurations):
   ```bash
   cd UserService && ../mvnw spring-boot:run
   cd WalletService && ../mvnw spring-boot:run
   cd TransactionService && ../mvnw spring-boot:run
   cd RuleEngineService && ../mvnw spring-boot:run
   ```

#### Шаг 4. Запустите фронтенд:
Прямо из корневой папки проекта:
```bash
npm run dev
```
*(либо перейдите в папку `cd frontend && npm run dev`)*.

Откройте в браузере: **[http://localhost:3000](http://localhost:3000)**.
> Все запросы вида `/api/*` автоматически проксируются Vite на Gateway `http://localhost:8080`.

---

## 📡 Основные REST API эндпоинты (через Gateway :8080)

### Аутентификация и пользователи (`UserService`)
- `POST /api/v1/users/register` — Регистрация (`email`, `firstName`, `lastName`, `password`)
- `POST /api/v1/users/auth` — Вход в систему (возвращает JWT токен)
- `GET /api/v1/users` — Получение профиля текущего пользователя (по Bearer JWT)
- `PUT /api/v1/users/{email}` — Обновление имени, фамилии или пароля
- `GET /api/v1/users/get_all` — [ADMIN] Список всех пользователей

### Кошелек и баллы (`WalletService`)
- `GET /api/v1/wallets/{userId}/balance` — Текущий баланс бонусов
- `GET /api/v1/wallets/{userId}/history` — История начислений и списаний баллов
- `PUT /api/v1/wallets/{userId}/block` — [ADMIN] Блокировка кошелька
- `PUT /api/v1/wallets/{userId}/unblock` — [ADMIN] Разблокировка кошелька

### Покупки и транзакции (`TransactionService`)
- `POST /api/v1/transactions` — Создание покупки (требует заголовок `X-IDEMPOTENCY-KEY: <UUID>`, тело: `amount`, `items`, `useCashbackBalance`)
- `GET /api/v1/transactions/user/{userId}` — История чеков пользователя
- `GET /api/v1/transactions/{id}` — [ADMIN] Детальная информация по чеку

### Правила кэшбэка (`RuleEngineService`)
- `GET /api/v1/rules` — [ADMIN] Список всех активных категориальных правил кэшбэка
- `POST /api/v1/rules` — [ADMIN] Создание правила (`category`, `percentage`, `validFrom`, `validTo`)
- `PUT /api/v1/rules/{id}` — [ADMIN] Обновление параметров правила
- `DELETE /api/v1/rules/{id}` — [ADMIN] Удаление правила

---

## 🧪 Тестирование

Проект покрыт unit- и интеграционными тестами с использованием **Testcontainers**:
```bash
./mvnw test
```
Тесты поднимают изолированные экземпляры PostgreSQL, Kafka и Redis в Docker-контейнерах для проверки сквозных бизнес-сценариев.

---

## 👤 Автор

**Александр Карев**

- Telegram: [@amaevalx](https://t.me/amaevalx)
- Email: amaevalx@gmail.com
- GitHub: [@maydeixxx](https://github.com/maydeixxx)
