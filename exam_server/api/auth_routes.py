from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import OAuth2PasswordRequestForm
from auth import create_access_token, verify_password
from database import User
import schemas

router = APIRouter()

@router.post("/login", response_model=schemas.Token, tags=["Authentication"])
async def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends()):
    """
    Принимает данные формы (username, password), проверяет их и возвращает токен.
    """
    # Форма ожидает "username", поэтому мы используем поле "login" для этого.
    user = User.get_or_none(User.login == form_data.username)
    if not user or not verify_password(form_data.password, user.password_hash):
        raise HTTPException(
            status_code=401,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    access_token = create_access_token(data={"sub": str(user.id)})
    
    # Возвращаем токен в теле ответа
    return {"access_token": access_token, "token_type": "bearer"}
