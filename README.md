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

### 3. Запустить приложение

```bash
docker-compose up --build
```

Команда запускает PostgreSQL и приложение вместе. При первом запуске скачиваются Docker-образы и зависимости — это займёт несколько минут.

### 4. Открыть Swagger UI

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
| GET | `/api/admin/users/{id}` | Пользователь по ID |
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

- Номера карт хранятся в зашифрованном виде (AES-256-GCM)
- В ответах API номера карт маскируются: `**** **** **** 1234`
- Пароли хранятся как BCrypt хеш
- Аутентификация через JWT Bearer токен
- Ролевой доступ: ROLE_USER и ROLE_ADMIN
- Защита от IDOR атак

---

## Запуск тестов

```bash
JAVA_HOME=~/Library/Java/JavaVirtualMachines/ms-17.0.15/Contents/Home mvn clean test
```

Или если `JAVA_HOME` уже настроен:

```bash
mvn clean test
```
