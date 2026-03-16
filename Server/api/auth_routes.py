from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import OAuth2PasswordRequestForm
from pydantic import BaseModel
from typing import Optional
from datetime import datetime, timedelta
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from auth import create_access_token, verify_password, get_password_hash
from database import User, Role, PendingRegistration
import schemas
from email_utils import generate_code, send_verification_email

router = APIRouter()


# ─── Авторизация ─────────────────────────────────────────────────────────────

@router.post("/login", response_model=schemas.Token, tags=["Authentication"])
async def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends()):
    user = User.get_or_none(User.login == form_data.username)
    if not user or not verify_password(form_data.password, user.password_hash):
        raise HTTPException(
            status_code=401,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    access_token = create_access_token(data={"sub": str(user.id)})
    return {"access_token": access_token, "token_type": "bearer"}


# ─── Авторизация через Google ─────────────────────────────────────────────────

class GoogleAuthRequest(BaseModel):
    email: str
    display_name: Optional[str] = None


@router.post("/auth/google", response_model=schemas.Token, tags=["Authentication"])
async def google_auth(data: GoogleAuthRequest):
    if not data.email:
        raise HTTPException(status_code=400, detail="email is required")

    user = User.get_or_none(User.email == data.email)

    if user is None:
        base_login = data.email.split("@")[0]
        base_login = "".join(c if c.isalnum() or c == "_" else "_" for c in base_login)
        login = base_login
        counter = 1
        while User.get_or_none(User.login == login) is not None:
            login = f"{base_login}{counter}"
            counter += 1

        first_name = None
        last_name = None
        if data.display_name:
            parts = data.display_name.strip().split(" ", 1)
            first_name = parts[0] if parts else None
            last_name = parts[1] if len(parts) > 1 else None

        customer_role = Role.get_or_none(Role.name == "customer")

        user = User.create(
            login=login,
            email=data.email,
            password_hash="google_oauth_no_password",
            first_name=first_name,
            last_name=last_name,
            role=customer_role,
        )

    access_token = create_access_token(data={"sub": str(user.id)})
    return {"access_token": access_token, "token_type": "bearer"}


# ─── Регистрация: шаг 1 — отправить код ──────────────────────────────────────

class RegisterRequest(BaseModel):
    email: str
    login: str
    password: str


class RegisterStartResponse(BaseModel):
    email: str
    message: str


@router.post("/auth/register", response_model=RegisterStartResponse, tags=["Authentication"])
async def register_send_code(data: RegisterRequest):
    """
    Шаг 1 регистрации: проверяем, что email и логин свободны,
    сохраняем данные во временную таблицу, отправляем 6-значный код.
    Если для этого email уже был запрос — старый код удаляется, отправляется новый.
    Пользователь создаётся только после подтверждения кода.
    """
    if User.get_or_none(User.email == data.email):
        raise HTTPException(status_code=400, detail="email_taken")

    if User.get_or_none(User.login == data.login):
        raise HTTPException(status_code=400, detail="login_taken")

    # Удаляем предыдущую заявку на этот email (если была)
    PendingRegistration.delete().where(PendingRegistration.email == data.email).execute()

    code = generate_code()
    expires_at = datetime.now() + timedelta(minutes=10)

    PendingRegistration.create(
        email=data.email,
        login=data.login,
        password_hash=get_password_hash(data.password),
        code=code,
        expires_at=expires_at,
    )

    send_verification_email(data.email, code)

    return RegisterStartResponse(
        email=data.email,
        message="Код подтверждения отправлен на почту",
    )


# ─── Регистрация: шаг 2 — подтвердить код ────────────────────────────────────

class VerifyEmailRequest(BaseModel):
    email: str
    code: str


@router.post("/auth/verify-email", response_model=schemas.Token, tags=["Authentication"])
async def verify_email(data: VerifyEmailRequest):
    """
    Шаг 2 регистрации: проверяем код, создаём пользователя, возвращаем JWT.
    """
    pending = PendingRegistration.get_or_none(PendingRegistration.email == data.email)

    if not pending:
        raise HTTPException(status_code=400, detail="Заявка не найдена. Пройдите регистрацию заново")

    if pending.code != data.code:
        raise HTTPException(status_code=400, detail="Неверный код подтверждения")

    if pending.expires_at < datetime.now():
        pending.delete_instance()
        raise HTTPException(status_code=400, detail="code_expired")

    # Повторная проверка: за время ожидания кто-то мог занять логин/email
    if User.get_or_none(User.email == pending.email):
        pending.delete_instance()
        raise HTTPException(status_code=400, detail="email_taken")

    if User.get_or_none(User.login == pending.login):
        pending.delete_instance()
        raise HTTPException(status_code=400, detail="login_taken")

    customer_role = Role.get_or_none(Role.name == "customer")

    user = User.create(
        login=pending.login,
        email=pending.email,
        password_hash=pending.password_hash,
        role=customer_role,
    )

    pending.delete_instance()

    access_token = create_access_token(data={"sub": str(user.id)})
    return {"access_token": access_token, "token_type": "bearer"}


# ─── Повторная отправка кода ──────────────────────────────────────────────────

class ResendCodeRequest(BaseModel):
    email: str


@router.post("/auth/resend-code", tags=["Authentication"])
async def resend_code(data: ResendCodeRequest):
    """
    Отправляет новый код для существующей заявки. Старый код становится недействительным.
    Ограничение: не чаще 1 раза в 60 секунд.
    """
    pending = PendingRegistration.get_or_none(PendingRegistration.email == data.email)

    if not pending:
        raise HTTPException(status_code=404, detail="Заявка не найдена. Пройдите регистрацию заново")

    elapsed = (datetime.now() - pending.created_at).total_seconds()
    if elapsed < 60:
        wait = int(60 - elapsed)
        raise HTTPException(
            status_code=429,
            detail=f"wait:{wait}",
        )

    code = generate_code()
    pending.code = code
    pending.expires_at = datetime.now() + timedelta(minutes=10)
    pending.created_at = datetime.now()
    pending.save()

    send_verification_email(data.email, code)

    return {"message": "Новый код отправлен", "email": data.email}
