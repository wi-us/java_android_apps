from pydantic import BaseModel, Field, field_validator, Extra
from typing import Optional
from datetime import datetime
import hashlib

class BaseModel(BaseModel):
    class Config:
        from_attributes = True

class Queries(BaseModel):
    limit: Optional[int] = None
    page: Optional[int] = None
    offset: Optional[int] = None


class Roles:
    class GET(BaseModel):
        id: int = Field(...)
        name: str = Field(...)
    
    class POST(BaseModel):
        name: str = Field(...,
                        min_length=1,
                        max_length=200)
    
    class PUT(BaseModel):
        id: int = None
        name: str = Field(...,
                        min_length=1,
                        max_length=200)
    
    class GET_ALL(Queries):
        id: Optional[int] = None
        name: Optional[str] = None
        
class Users:
    class GET(BaseModel):
        id: int = Field(...)
        name: str = Field(...)
        surname: str = Field(...)
        nickname: str = Field(...)
        password: str = Field(...)
        role_id: Roles.GET = Field(...)
        balance: float = Field(...)
        discount: int = Field(...)

        class Config:
            exclude = {"password"}
    
    class POST(BaseModel):
        name: str = Field(...)
        surname: str = Field(...)
        nickname: str = Field(...)
        password: str = Field(...)
        role_id: Roles.GET = Field(...)

        @field_validator('password')
        def hash_password(cls, v: str):
            if v is not None:
                return hashlib.sha256(v.encode("utf-8")).hexdigest()
            
        class Config:
            extra = 'ignore'


    class PUT(BaseModel):
        id: int = Field(...)
        name: Optional[str] = None
        surname: Optional[str] = None
        nickname: Optional[str] = None
        password: Optional[str] = None
        role_id: Optional[int] = None
        balance: Optional[float] = None
        discount: Optional[int] = None

        @field_validator('password')
        def hash_password(cls, v: str):
            if v is not None:
                return hashlib.sha256(v.encode("utf-8")).hexdigest()
            
    class GET_ALL(Queries):
        id: Optional[int] = None
        name: Optional[str] = None
        surname: Optional[str] = None
        nickname: Optional[str] = None
        role_id: Optional[int | str] = None

class Categories:
    class GET(BaseModel):
        id: int = Field(...)
        name: str = Field(...)

    class POST(BaseModel):
        name: str = Field(...)
    
    class PUT(BaseModel):
        id: int = Field(...)
        name: Optional[str] = None
    
    class GET_ALL(Queries):
        id: Optional[int] = None
        name: Optional[str] = None

class Levels:
    class GET(BaseModel):
        id: int = Field(...)
        name: str = Field(...)
        
    class POST(BaseModel):
        name: str = Field(...)
    
    class PUT(BaseModel):
        id: int = Field(...)
        name: Optional[str] = None
    
    class GET_ALL(Queries):
        id: Optional[int] = None
        name: Optional[str] = None

class Courses:
    class GET(BaseModel):
        id: int = Field(...)
        name: str = Field(...)
        description: str = Field(...)
        author_id: Users.GET = Field(...)
        category_id: Categories.GET = Field(...)
        level_id: Levels.GET = Field(...)
        price: float = Field(...)
        isVisible: bool = Field(...)

    class POST(BaseModel):
        name: str = Field(...)
        description: str = Field(...)
        author_id: int = Field(...)
        category_id: int = Field(...)
        level_id: int = Field(...)
        price: float = Field(...)
        isVisible: bool = Field(...)

    class PUT(BaseModel):
        id: int = Field(...)
        name: Optional[str] = None
        description: Optional[str] = None
        author_id: Optional[int] = None
        category_id: Optional[int] = None
        level_id: Optional[int] = None
        price: Optional[float] = None
        isVisible: Optional[bool] = None

    class GET_ALL(Queries):
        id: Optional[int] = None
        name: Optional[str] = None
        description: Optional[str] = None
        author_id: Optional[int | str] = None
        category_id: Optional[int | str] = None
        level_id: Optional[int | str] = None
        price: Optional[float] = None
        isVisible: Optional[bool] = None


class UserCourses:
    class GET(BaseModel):
        id: int = Field(...)
        user_id: Users.GET = Field(...)
        course_id: Courses.GET | int = Field(...)
        date: datetime = Field(
                               default=datetime.now().timestamp())
        progress: float = Field(...)
        
        @field_validator('date')
        def validate_timestamp(cls, v: datetime):
            if v is not None:
                try:
                    return v.timestamp()
                except ValueError:
                    raise ValueError("Timestamp должен быть float")
            return v
    
    class POST(BaseModel):
        user_id: int = Field(...)
        course_id: int = Field(...)
    
    class PUT(BaseModel):
        id: int = Field(...)
        user_id: Optional[int] = None
        course_id: Optional[int] = None
        date: Optional[float] = None
        progress: Optional[float] = None

    class GET_ALL(Queries):
        id: Optional[int | str] = None
        user_id: Optional[int | str] = None
        course_id: Optional[int | str] = None
        date: Optional[float] = None
        progress: Optional[float] = None
        
        
class Lessons:
    class GET(BaseModel):
        id: int = Field(...)
        name: str = Field(...)
        course_id: Courses.GET = Field(...)
        video_url: str = Field(...)
        duration: int = Field(...)
        order: int = Field(...)
        
    class POST(BaseModel):
        name: str = Field(...)
        course_id: int = Field(...)
        video_url: str = Field(...)
        duration: int = Field(...)
        order: int = Field(...)

    class PUT(BaseModel):
        id: int = Field(...)
        name: Optional[str] = None
        course_id: Optional[int] = None
        video_url: Optional[str] = None
        duration: Optional[int] = None
        order: Optional[int] = None

    class GET_ALL(Queries):
        id: Optional[int] = None
        name: Optional[str] = None
        course_id: Optional[int | str] = None
        video_url: Optional[str] = None
        duration: Optional[int] = None
        order: Optional[int] = None

    