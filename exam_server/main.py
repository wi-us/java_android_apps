from fastapi import FastAPI, Depends
from fastapi.staticfiles import StaticFiles
from database import create_tables, migrate_schema, db, Role, User
from routes import router as api_router
from admin import admin_router, admin_auth_router # Импортируем оба роутера
from api.auth_routes import router as auth_router
import uvicorn
from pathlib import Path

app = FastAPI(
    title="Shop Exam API",
    description="API for the online board game store.",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# Сначала подключаем статику, чтобы она имела приоритет
app.mount("/static", StaticFiles(directory="static"), name="static")

# Раздача скачанных изображений из /cache (используется мобильным приложением)
BASE_DIR = Path(__file__).resolve().parent.parent
CACHE_DIR = BASE_DIR / "cache"
CACHE_DIR.mkdir(parents=True, exist_ok=True)
app.mount("/cache", StaticFiles(directory=str(CACHE_DIR)), name="cache")

# Подключаем API роутеры
app.include_router(api_router, prefix="/api")
app.include_router(auth_router, prefix="/api")

# Подключаем роутеры админки: сначала публичный, потом защищенный
app.include_router(admin_auth_router)
app.include_router(admin_router)

@app.on_event("startup")
def startup_event():
    """При старте приложения создает таблицы и начальные данные."""
    if db.is_closed():
        db.connect()
    create_tables()
    migrate_schema()

    admin_role, _ = Role.get_or_create(name='admin')
    Role.get_or_create(name='customer')

    admin, created = User.get_or_create(
        login='admin',
        defaults={'email': 'admin@example.com', 'role': admin_role, 'password_hash': 'temp'}
    )
    if created:
        admin.set_password('admin')
        admin.save()

@app.on_event("shutdown")
def shutdown_event():
    """При остановке приложения закрывает соединение с БД."""
    if not db.is_closed():
        db.close()

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8003, reload=True)
