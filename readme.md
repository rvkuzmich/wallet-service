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

## Запуск
Для запуска приложения склонируйте репозиторий, выполните **docker-compose up -d** из директории репозитория

## Тестирование
Для тестирования нагрузки запустите скрипт load-test.sh после старта приложения.