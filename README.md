# Shop Exam

Мобильное приложение интернет-магазина настольных игр.

## Требования

- Android 7.0+ (API 24)
- Java 8

## Сервер

Локальный запуск:

```bash
cd Server
pip install -r requirements.txt
uvicorn main:app --reload
```

Сервер будет доступен по адресу http://127.0.0.1:8000

## Notion Telegram BotОтдельный сервис для интеграции Notion с Telegram — см. папку `Notion/`.