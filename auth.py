from fastapi import HTTPException, Response, Depends, APIRouter, Request 
from authx import AuthX, AuthXConfig
from pydantic import BaseModel
from database import User, Role
from fastapi.responses import JSONResponse
import schemas
from datetime import datetime
import hashlib

router = APIRouter(tags=["Demo Auth"])
#router = APIRouter(tags=["Demo Auth"])

config = AuthXConfig()
config.JWT_SECRET_KEY = "SECRET_KEY"
config.JWT_ACCESS_COOKIE_NAME = "ACCESS_TOKEN"
config.JWT_TOKEN_LOCATION = ["cookies", "headers"]

security = AuthX(config=config)



class UserLoginSchema(BaseModel):   
    username: str
    password: str

class UserAccessSchema(BaseModel):
    id: int
    nickname: str
    role_id: int
    token: str
    isTokenExp: bool
    balance: float
    discount: int
    
@router.post("/login")
def login(response: Response,
          credentials: UserLoginSchema = Depends(UserLoginSchema)):
    try:
        user = User.select(User.id, User.nickname, User.password).where(User.nickname == credentials.username)[0]
        credentials.password = hashlib.sha256(credentials.password.encode("utf-8")).hexdigest()
        if credentials.username == user.nickname:
            if credentials.password == user.password:
                token = security.create_access_token(uid=str(user.id))
                response.set_cookie(config.JWT_ACCESS_COOKIE_NAME, token)
                return {"access_token": token}
            raise HTTPException(status_code=400, detail="Wrong password") 
        raise HTTPException(status_code=400, detail="User with this login not found") 
    except BaseException as e:
        return HTTPException(status_code=400, detail=f"{e}")

def token_expired(token: str) -> bool:
    try:
        if security._decode_token(token).iat < datetime.now().timestamp():
            return False
        else:
            return True
    except: 
        return HTTPException(status_code=403, detail="Invalid token")

def get_user_by_token(request: Request) -> JSONResponse:
    try:
        token = ""
        if request != None:
            try:
                token = request.cookies[config.JWT_ACCESS_COOKIE_NAME]
            except:
                try:
                    token = request.headers[config.JWT_ACCESS_COOKIE_NAME]
                except:
                    return HTTPException(status_code=401, detail="No token in the Request")
        else:
            return HTTPException(status_code=400, detail="Request is None")
        
        if token_expired(token) == False:
            id = int(security._decode_token(token).sub)
            user = User.get(User.id == id)
            schema = schemas.Users.GET.from_orm(user).dict()
            return UserAccessSchema(
                id = schema["id"],
                nickname = schema["nickname"],
                role_id = schema["role_id"]["id"],
                balance = schema["balance"],
                discount = schema["discount"],
                token = token,
                isTokenExp = False,
            )
        raise HTTPException(status_code=401, detail="Unauthorized")
    except BaseException as e:
        return HTTPException(status_code=400, detail=f"{e}")


# @router.get(path=" /protected", dependencies=[Depends(security.access_token_required)])
# def protected(req: Request):
#     return get_user_by_token(token=req.cookies[config.JWT_ACCESS_COOKIE_NAME])
