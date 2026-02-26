# Bank Card Management System

REST API для управления банковскими картами с аутентификацией через JWT.

## Технологии

- Java 17, Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA + Hibernate
- PostgreSQL + Liquibase
- Docker Compose
- Swagger / OpenAPI

## Быстрый старт

### 1. Клонировать репозиторий

```bash
git clone <repo-url>
cd bank_rest
```

### 2. Создать `.env` файл

```bash
cp .env.example .env
```

Заполни реальными значениями:

```
DB_HOST=localhost
DB_PORT=5433
DB_NAME=bankcards
DB_USERNAME=postgres
DB_PASSWORD=postgres

JWT_SECRET=myBankAppSuperSecretKey1234567890!!
JWT_EXPIRATION_MS=86400000

ENCRYPTION_KEY=1234567890abcdef1234567890abcdef
```

> `JWT_SECRET` — минимум 32 символа  
> `ENCRYPTION_KEY` — ровно 32 символа (AES-256)

### 3. Запустить PostgreSQL через Docker

```bash
docker-compose up postgres -d
```

### 4. Запустить приложение

```bash
./mvnw spring-boot:run
```

Или через IntelliJ IDEA — запустить `BankCardsApplication`.

### 5. Открыть Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Дефолтный администратор

| Email | Password |
|---|---|
| admin@bank.com | admin123 |

---

## API Эндпоинты

### Аутентификация (публичные)

| Метод | URL | Описание |
|---|---|---|
| POST | `/api/auth/register` | Регистрация |
| POST | `/api/auth/login` | Логин, возвращает JWT |

### Карты (ROLE_USER)

| Метод | URL | Описание |
|---|---|---|
| GET | `/api/cards` | Мои карты (пагинация + фильтр по статусу) |
| POST | `/api/cards/transfer` | Перевод между своими картами |
| PATCH | `/api/cards/{id}/block` | Запрос на блокировку карты |

### Администратор (ROLE_ADMIN)

| Метод | URL | Описание |
|---|---|---|
| GET | `/api/admin/users` | Все пользователи |
| DELETE | `/api/admin/users/{id}` | Удалить пользователя |
| POST | `/api/admin/cards` | Создать карту |
| GET | `/api/admin/cards` | Все карты |
| GET | `/api/admin/cards/{id}` | Карта по ID |
| POST | `/api/admin/cards/{id}/deposit` | Пополнить баланс |
| PATCH | `/api/admin/cards/{id}/block` | Заблокировать карту |
| PATCH | `/api/admin/cards/{id}/activate` | Активировать карту |
| DELETE | `/api/admin/cards/{id}` | Удалить карту |

---

## Безопасность

- Номера карт хранятся в зашифрованном виде (AES-256)
- В ответах API номера карт маскируются: `**** **** **** 1234`
- Пароли хранятся как BCrypt хеш
- Аутентификация через JWT Bearer токен
- Ролевой доступ: ROLE_USER и ROLE_ADMIN

---

## Запуск тестов

```bash
./mvnw test
```
