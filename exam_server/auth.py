import jwt
from fastapi import Depends, HTTPException, Request, Response
from fastapi.responses import RedirectResponse
from fastapi.security import OAuth2PasswordBearer
from datetime import datetime, timedelta, timezone
from database import User
import hashlib
from typing import Optional

# --- Настройки ---
SECRET_KEY = "a_very_secret_key_that_should_be_in_env_var"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30
COOKIE_NAME = "access_token"

# Схема для получения токена из заголовка Authorization.
# auto_error=False означает, что зависимость не будет вызывать ошибку,
# если заголовок отсутствует, а просто вернет None.
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/login", auto_error=False)

# --- Хелперы для паролей ---
def verify_password(plain_password, hashed_password):
    """Проверяет, соответствует ли пароль хэшу."""
    return hashlib.sha256(plain_password.encode('utf-8')).hexdigest() == hashed_password

# --- Создание и проверка токенов ---
def create_access_token(data: dict):
    """Создает JWT токен."""
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

# --- Зависимости для FastAPI ---
def get_current_user(request: Request, response: Response, token: Optional[str] = Depends(oauth2_scheme)) -> User:
    """
    Зависимость для получения текущего пользователя.
    Пытается извлечь токен сначала из cookie (для веб-админки),
    а затем из заголовка Authorization (для API).
    """
    token_from_cookie = request.cookies.get(COOKIE_NAME)
    
    final_token = token_from_cookie or token

    credentials_exception = HTTPException(
        status_code=401,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    if final_token is None:
        raise credentials_exception

    try:
        payload = jwt.decode(final_token, SECRET_KEY, algorithms=[ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            raise credentials_exception
    except jwt.ExpiredSignatureError:
        # Если токен истек и запрос пришел из браузера (есть cookie),
        # перенаправляем на страницу входа и удаляем старый cookie.
        if request.cookies.get(COOKIE_NAME):
            # Важно: НЕ передавать сюда headers от RedirectResponse целиком —
            # это иногда приводит к RuntimeError "Response content longer than Content-Length".
            # Делаем редирект через заголовок Location и одновременно удаляем cookie.
            delete_cookie = f"{COOKIE_NAME}=; Max-Age=0; Path=/; HttpOnly"
            raise HTTPException(
                status_code=303,
                detail="Redirecting to login",
                headers={"Location": "/login", "Set-Cookie": delete_cookie},
            )
        # Для API-запросов (без cookie) просто сообщаем об истечении токена.
        raise HTTPException(status_code=401, detail="Token has expired")
    except jwt.PyJWTError:
        raise credentials_exception
    
    user = User.get_or_none(User.id == int(user_id))
    if user is None:
        raise credentials_exception
    return user

def get_current_admin_user(response: Response, current_user: User = Depends(get_current_user)) -> User:
    """
    Зависимость, которая проверяет, является ли текущий пользователь администратором.
    """
    if not current_user.role or current_user.role.name != 'admin':
        raise HTTPException(status_code=403, detail="The user doesn't have enough privileges")
    return current_user
