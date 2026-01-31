# Wallet Service 
Микросервис для управления электронными кошельками с поддержкой транзакций в конкурентной среде.
## Описание
* REST API сервис для операций с кошельками:
* Пополнение кошелька (DEPOSIT)
* Снятие средств (WITHDRAW)
* Получение баланса
* Создание кошелька

## Технологии
* Java 17
* Spring Boot 3
* PostgreSQL 15
* Liquibase (миграции БД)
* Docker & Docker Compose
* Spring Retry (обработка конкурентных операций)
* Mockito & JUnit 5 (тестирование)

## API Endpoints
1. Создание кошелька
    POST /api/v1/wallets/{walletId}/create
2. Операция с кошельком
    POST /api/v1/wallet 
    Content-Type: application/json
3. Получение баланса
   GET /api/v1/wallets/{walletId}
4. Health check
   GET /api/v1/health

## Модель данных запроса
Для создания кошелька можно использовать запрос вида:

{
"walletId": "123e4567-e89b-12d3-a456-426614174000",
"balance": 0.00,
"createdAt": "2026-01-30T10:00:00",
"updatedAt": "2026-01-30T10:00:00"
}

Для операций с кошельком используются запросы вида:

{
"walletId": "123e4567-e89b-12d3-a456-426614174000",
"operationType": "DEPOSIT",
"amount": 1000.00
}
## Запуск
Для запуска приложения склонируйте репозиторий, выполните **docker-compose up -d** из директории репозитория

## Тестирование
Для тестирования нагрузки запустите скрипт load-test.sh после старта приложения.