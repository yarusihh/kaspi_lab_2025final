# Проект 1: File Uploader

Сервис загрузки файлов (Spring Boot WebFlux) с сохранением метаданных в PostgreSQL, временной блокировкой в Redis и загрузкой в S3/MinIO.

## Деплой URL

- Веб-интерфейс и API: `https://kaspi.icod.kz/`
- Панель S3 (MinIO Browser): `https://minio-ui.icod.kz/browser/kaspi-lab-public/`

## Доступные Endpoint

- `GET /`  
  HTML-страница проекта.

- `GET /api/v1/`  
  Проверка доступности API.

- `POST /api/v1/upload/`  
  Загрузка файла.  
  Формат: `multipart/form-data`, поле: `file`.

- `GET /api/v1/db/files`  
  Список файлов из БД (до 20 записей).

- `GET /api/v1/storage/files`  
  Список файлов из S3/Storage (до 20 записей).

- `GET /actuator/health`  
- `GET /actuator/info`

## Что используется

- `Spring Boot 3.3 (WebFlux)`
- `PostgreSQL + Flyway`
- `Redis` (lock на время обработки)
- `S3/MinIO` (хранение файлов)
- `Docker / Docker Compose`

Инфраструктура работает в общей сети с внешними зависимостями: Redis, PostgreSQL, Celery, S3/MinIO.

## Запуск через Docker Compose

Файлы:

- `proj2_2/Dockerfile`
- `proj2_2/docker-compose.yml`
- `proj2_2/app/file-uploader/.env`

1. Создать внешнюю сеть (если еще не создана):

```bash
docker network create --driver bridge --subnet 10.50.0.0/24 kaspi_lab_net
```

2. Запустить сервис:

```bash
cd proj2_2
docker compose up -d --build
```

3. Проверка:

```bash
curl http://localhost:8080/api/v1/
```

## Переменные окружения

Основные значения задаются в `app/file-uploader/.env`:

- PostgreSQL: `SPRING_R2DBC_*`, `SPRING_FLYWAY_*`
- Redis: `REDIS_*`
- S3/MinIO: `S3_*`
- Lock TTL: `APP_PROCESSING_LOCK_TTL`

## Endpoint Responses (Detailed)

### `GET /`
- `200 OK`
- Returns HTML page `index`.

### `GET /api/v1/`
- `200 OK`
```json
{
  "success": true,
  "msg": "API v1 работает."
}
```

### `POST /api/v1/upload/`
Request: `multipart/form-data`, field `file`.

- `202 Accepted`
```json
{
  "success": true,
  "msg": "Файл принят в обработку."
}
```

- `409 Conflict`
```json
{
  "success": false,
  "msg": "Файл с таким содержимым уже обрабатывается."
}
```
or
```json
{
  "success": false,
  "msg": "Файл с таким hash уже существует в базе данных."
}
```

- `400 Bad Request`
```json
{
  "success": false,
  "msg": "Файл не передан в запросе."
}
```

- `500 Internal Server Error`
```json
{
  "success": false,
  "msg": "Внутренняя ошибка контроллера: ..."
}
```

### `GET /api/v1/db/files`
- `200 OK`
```json
{
  "success": true,
  "msg": "...",
  "files": [
    {
      "id": 1,
      "uuid": "...",
      "uploadDate": "...",
      "name": "...",
      "size": 123,
      "hash": "...",
      "url": "..."
    }
  ]
}
```

- `500 Internal Server Error`
```json
{
  "success": false,
  "msg": "Не удалось получить список файлов из базы данных: ..."
}
```

### `GET /api/v1/storage/files`
- `200 OK`
```json
{
  "success": true,
  "msg": "...",
  "files": [
    {
      "id": null,
      "uuid": null,
      "uploadDate": "...",
      "name": "...",
      "size": 123,
      "hash": null,
      "url": "..."
    }
  ]
}
```

- `500 Internal Server Error`
```json
{
  "success": false,
  "msg": "Не удалось получить список файлов из S3: ..."
}
```

Note: backend returns both `success` and typo-compatible field `sucess`.
