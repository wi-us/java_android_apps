from fastapi import FastAPI, Request, HTTPException, Depends
from fastapi.params import Query, Body
from fastapi.responses import JSONResponse
import schemas
import database
import logging
import uvicorn
from typing import List, Optional
from datetime import datetime
from auth import router as auth_router, get_user_by_token
from json import loads

app = FastAPI(root_path="/api")
app.include_router(router=auth_router)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler()])
logger = logging.getLogger(__name__)

class NewModel():
    db = database
    schema = schemas
    
    def get_by_id(cls, 
                  request: Request, 
                  id: int):
        """
        Получение по ID
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                return user
            else:
                if id is not None:
                    if user.role_id == 1:
                        response = cls.schema.GET.from_orm(cls.db.get(cls.db.id == id)).dict() #TODO: получать через database
                        return JSONResponse(response)
                    raise HTTPException(status_code=403, detail="Forbidden")
                raise HTTPException(status_code=400, detail="id is empty")
        except BaseException as e:
            return e
        
    def get(cls,
            request,
            params: Optional[schemas.Queries] = None
            ):
        """
        Получение всех разрешений или ограниченных page и limit
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                 return user
            else:
                if user.role_id == 1:
                    params = params.dict(exclude_none=True)
                    
                    limit = params.pop('limit', None)
                    page = params.pop('page', None)
                    offset = params.pop('offset', None)
                    
                    if offset is None:
                        if page is not None and limit is not None:
                            offset = limit * (page - 1)
                        else:
                            offset = 0
                    return cls.db.get_item(params, limit=limit, offset=offset)
                raise HTTPException(status_code=403, detail="Forbidden")
        except BaseException as e:
            return e
        
    def post(cls, 
             request: Request,
             data: schema ):
        """
        Добавление Разрешения
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                 return user
            else:
                if user.role_id == 1:
                    return cls.db.create_item(**dict(data))
                raise HTTPException(status_code=403, detail="Forbidden")
        except BaseException as e:
            return e
        
    def delete(cls, 
               request: Request, 
               id: int):
        """
        Удаление Разрешения по ID
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                 return user
            else:
                if user.role_id == 1:
                    return cls.db.delete_item(id)
                raise HTTPException(status_code=403, detail="Forbidden")
        except BaseException as e:
            return e
    
    def put(cls, 
            request: Request,
            data: schema):
        """
        Добавление Разрешения
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                 return user
            else:
                if user.role_id == 1:
                    data = data.dict(exclude_none=True)
                    return cls.db.edit_item(**dict(data))
                raise HTTPException(status_code=403, detail="Forbidden")
            
        except BaseException as e:
            return e
    
class API_Role(NewModel):
    db = database.Role
    schema = schemas.Roles

class API_User(NewModel): #Main entity

    db = database.User
    schema = schemas.Users

    # Студент (роль 3):
    ## может просматривать информацию о пользователях(GET)
    # Преподаватель (роль 2):
    ## может просматривать информацию о пользователях(GET)

    def get_by_id(cls, 
                  request: Request, 
                  id: int):
        """
        Получение по ID
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                 return user
            else:
                if id is not None:
                    if user.role_id < 4:
                        response = cls.schema.GET.from_orm(cls.db.get(cls.db.id == id)).dict() #TODO: через database
                        return JSONResponse(response)
                    raise HTTPException(status_code=403, detail="Forbidden")
                raise HTTPException(status_code=400, detail="ID is empty")
        except HTTPException:
            return HTTPException(status_code=400, detail="Bad Request")
        
    def get(cls,
            request: Request,
            params: Optional[schema.GET_ALL] = None
            ):
        """
        Получение всех разрешений или ограниченных page и limit
        """
        
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                 return user
            else:
                if user.role_id < 4:
                    params = params.dict(exclude_none=True)
                    
                    limit = params.pop('limit', None)
                    page = params.pop('page', None)
                    offset = params.pop('offset', None)
                    
                    if offset is None:
                        if page is not None and limit is not None:
                            offset = limit * (page - 1)
                        else:
                            offset = 0
                    return cls.db.get_item(params, limit=limit, offset=offset)
                raise HTTPException(status_code=403, detail="Forbidden")
        except HTTPException:
            return HTTPException(status_code=400, detail="Bad Request")

class API_Course(NewModel): #Main entity
    db = database.Course
    schema = schemas.Courses

    # Студент (роль 3):
    ## может просматривать курсы (GET)
    
    # Преподаватель (роль 2):
    ## может просматривать курсы (GET)
    ## может создавать их (POST)
    ## может редактировать их (PUT)
    ## может удалять их (PUT)
    ### только свои курсы

    # проматривать курсы (GET)
    ## может каждый

    def get_by_id(cls, 
                  request: Request, 
                  id: int):
        """
        Получение по ID
        """
        try:
            if id is not None:
                response = cls.schema.GET.from_orm(cls.db.get(cls.db.id == id)).dict() #TODO: через database
                return JSONResponse(response)
            raise HTTPException(status_code=400, detail="ID is empty")
        except HTTPException:
            return HTTPException(status_code=400, detail="Bad Request")
        
    def get(cls,
            request,
            page: Optional[int] = Query(None), 
            limit: Optional[int] = Query(None),
            params: Optional[schema.GET_ALL] = None
            ):
        """
        Получение всех разрешений или ограниченных page и limit
        """

        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                return user
            else:
                params = params.dict(exclude_none=True)
                
                limit = params.pop('limit', None)
                page = params.pop('page', None)
                offset = params.pop('offset', None)
                
                if offset is None:
                    if page is not None and limit is not None:
                        offset = limit * (page - 1)
                    else:
                        offset = 0
                return cls.db.get_item(params, limit=limit, offset=offset)
        except HTTPException:
            return HTTPException(status_code=400, detail="Bad Request")

    def post(cls, 
            request: Request,
            data):
        """
        Добавление Разрешения
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                 return user
            else:
                if user.role_id == 1 or user.role_id == 2:
                    return cls.db.create_item(**dict(data))
                raise HTTPException(status_code=403, detail="Forbidden")
        except HTTPException:
            return HTTPException(status_code=400, detail="Bad Request")
        
    def delete(cls, 
               request: Request, 
               id: int):
        """
        Удаление Разрешения по ID
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                 return user
            else:
                if user.role_id == 1:
                    return cls.db.delete_item(id)
                elif user.role_id == 2 and user.id == loads(cls.get_by_id(request=request, id=id).body)["author_id"]["id"]:
                    return cls.db.delete_item(id)
                raise HTTPException(status_code=403, detail="Forbidden")
        except HTTPException:
            return HTTPException(status_code=400, detail="Bad Request")
    
    def put(cls, 
            request: Request,
            data):
        """
        Изменение курса
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                 return user
            else:
                if user.role_id == 1:
                    return cls.db.edit_item(**dict(data))
                elif user.role_id == 2 and user.id == loads(cls.get_by_id(request=request, id=id).body)["author_id"]["id"]:
                    return cls.db.delete_item(id)
                raise HTTPException(status_code=403, detail="Forbidden")
        except HTTPException:
            return HTTPException(status_code=400, detail="Bad Request")

class API_UserCourse(NewModel): #Main entity
    db = database.UserCourse
    schema = schemas.UserCourses

    # Студент (роль 3):
    ## может просматривать курсы на которые записан (GET)
    ## может записываться на курсы (POST)
    ### система скидок (+5% за купленный курс до 50%. Система не сбрасывается)
    
    # Преподаватель (роль 2):
    ## может просматривать курсы на которые записан (GET)
    ## может просматривать студентов которые записаны на его курс (GET)
    ### система скидок (+5% за купленный курс до 50%. Система не сбрасывается)

    def get_by_user(cls, 
                    request: Request, 
                    id: int
                    ) -> List[JSONResponse]:
        """
        Получение по ID
        """
        try:
            if id is not None:
                courses = cls.db.select().where(cls.db.user_id == id) #TODO: сделать через database
                arr = []
                for item in courses:
                    arr.append(cls.schema.GET
                                    .from_orm(item)
                                    .dict())
                #test[0] = datetime.timestamp(test.date)
                return JSONResponse(arr)
            raise HTTPException(status_code=400, detail="ID is empty")
        except HTTPException:
            return HTTPException(status_code=400, detail="Bad Request")
        
    def get_by_course(cls, 
                      request: Request, 
                      id: int
                      ) -> List[JSONResponse]:
        """
        Получение всех студентов курса по ID курса
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                return user
            else:
                if id is not None:
                    courses = cls.db.select().where(cls.db.course_id == id) #TODO: сделать через database
                    arr = []
                    for item in courses:
                        _validate = cls.schema.GET.from_orm(item).dict()
                        if _validate["course_id"]["author_id"]["id"] == user.id:
                            arr.append(cls.schema.GET
                                        .from_orm(item)
                                        .dict())
                            
                    return JSONResponse(arr)
                raise HTTPException(status_code=400, detail="ID is empty")
        except HTTPException:
            return HTTPException(status_code=400, detail="Bad Request")

    def post_sign_up_for_course(cls, 
                                request: Request, 
                                data: schemas):
        """
        Запись пользователя на курс ID
        """
        try:
            user = get_user_by_token(request=request)
            if type(user) == HTTPException:
                 return user
            
            if not data.user_id:
                 data.user_id = user.id
            if not data.course_id:
                return HTTPException(status_code=400, detail="The course id didnt sent")
        
            else:
                course = None
                try:
                    course = database.Course.get(database.Course.id == data.course_id)
                except:
                    return HTTPException(status_code=404, detail="Course not found")
                
                course = API_Course.schema.GET.from_orm(course).dict()
                
                if cls.db.select(cls.db.user_id).where(cls.db.user_id == data.user_id 
                                                                       and cls.db.course_id == data.user_id).count() > 0:
                    return HTTPException(status_code=404, detail="User already registered on the course")

                if user.balance >= course["price"]:
                    sch = cls.schema.POST(user_id = user.id, 
                                            course_id = data.course_id).dict()
                    try:
                        cls.db.create_item(**sch)
                        user.balance = course["price"] - course["price"]*user.discount/100
                        if user.discount <= 50 and user.discount + 5 <= 50:
                            user.discount += 5
                        elif user.discount <= 50 and user.discount + 5 > 50:
                             user.discount = 50
                        user = user.dict()
                        sch = schemas.Users.PUT(**user)
                        return API_User().put(request=request, data=sch)
                    except:
                        return HTTPException(status_code=400, detail="Error with creating entity")
                raise HTTPException(status_code=400, detail="Not enough money")
        except HTTPException:
            return HTTPException(status_code=400, detail="Bad Request")
        
class API_Lesson(NewModel):
    db = database.Lesson
    schema = schemas.Lessons

class API_Category(NewModel):
    db = database.Category
    schema = schemas.Categories

class API_Level(NewModel):
    db = database.Level
    schema = schemas.Levels 

#
#Roles
#

@app.post("/role", tags=["Роль"], name="Создать роль", include_in_schema=False)
def post(request: Request, 
         data: API_Role.schema.POST = Depends(API_Role.schema.POST)):
        return API_Role().post(request=request, data=data)

@app.get("/role/{id}", tags=["Роль"], name="Получить объект роли по ID", response_model=API_Role.schema.GET, include_in_schema=False)
def get(request: Request, id: int):
        return API_Role().get_by_id(request=request, id=id)

@app.get("/role", tags=["Роль"], name="Получить все объекты таблицы роли по QUERY парамтрам", response_model=List[API_Role.schema.GET], include_in_schema=False)
def get(request: Request, 
        params: API_Role.schema.GET_ALL = Depends()):
     
        return API_Role().get(request=request, params=params)

@app.put("/role", tags=["Роль"], name="Изменить роль", include_in_schema=False)
def put(request: Request, 
        data: API_Role.schema.PUT = Depends(API_Role.schema.PUT)):
     
        return API_Role().put(request=request, data=data)

@app.delete("/role/{id}", tags=["Роль"], name="Удалить роль", include_in_schema=False)
def delete(request: Request, id: int):
        return API_Role().delete(request=request, id=id)


#
#Users
#

@app.post("/user", tags=["Пользователь"])
def post(request: Request, 
         data: API_User.schema.POST = Depends(API_User.schema.POST)):
     
        return API_User().post(request=request, data=data)

@app.get("/user/{id}", tags=["Пользователь"], response_model=API_User.schema.GET)
def get(request: Request,
        id: int):
     
        return API_User().get_by_id(request=request, id=id)


@app.get("/user", tags=["Пользователь"], response_model=List[API_User.schema.GET])
def get(request: Request,
        params: API_User.schema.GET_ALL = Depends()):

        #return API_User().get(request=request, limit=limit, page=page, params=params)
        return API_User().get(request=request, params=params)
    

@app.put("/user", tags=["Пользователь"])
def put(request: Request, 
        data: API_User.schema.PUT = Depends(API_User.schema.PUT)):
        
        return API_User().put(request=request, data=data)


@app.delete("/user/{id}", tags=["Пользователь"])
def delete(request: Request, 
           id: int):
     
        return API_User().delete(request=request, id=id)

#
#Course
#

@app.post("/course", tags=["Курс"])
def post(request: Request, 
         data: API_Course.schema.POST = Depends(API_Course.schema.POST)):
        
        return API_Course().post(data=data, request=request)

@app.get("/course/{id}", tags=["Курс"], response_model=API_Course.schema.GET)
def get(request: Request, 
        id: int):
        
        return API_Course().get_by_id(id=id, request=request)

@app.get("/course", tags=["Курс"], response_model=List[API_Course.schema.GET])
def get(request: Request, 
        params: API_Course.schema.GET_ALL = Depends()):

        return API_Course().get(params=params, 
                                request=request)

@app.put("/course", tags=["Курс"])
def put(request: Request, 
        data: API_Course.schema.PUT = Depends(API_Course.schema.PUT)):
        
        return API_Course().put(data=data, request=request)

@app.delete("/course/{id}", tags=["Курс"])
def delete(request: Request, 
           id: int):
        
        return API_Course().delete(id=id, request=request)

#
#UserCourse
#

@app.post("/user_course", tags=["Запись на курс"])
def post(request: Request, 
         data: API_UserCourse.schema.POST = Depends(API_UserCourse.schema.POST)):
        
        return API_UserCourse().post_sign_up_for_course(request=request, data=data)

@app.get("/user_course/user/{id}", tags=["Запись на курс"], response_model=List[API_UserCourse.schema.GET])
def get(request: Request, 
        id: int):

        return API_UserCourse().get_by_user(request=request, id=id)

@app.get("/user_course/course/{id}", tags=["Запись на курс"], response_model=List[API_UserCourse.schema.GET])
def get(request: Request, 
        id: int):
        
        return API_UserCourse().get_by_course(request=request, id=id)

@app.get("/user_course", tags=["Запись на курс"], response_model=List[API_UserCourse.schema.GET])
def get(request: Request, 
        params: API_Course.schema.GET_ALL = Depends()):

        return API_UserCourse().get(request=request, 
                                    params=params)

@app.put("/user_course", tags=["Запись на курс"])
def put(request: Request, 
        data: API_UserCourse.schema.PUT = Depends(API_UserCourse.schema.PUT)):
     
        return API_UserCourse().put(request=request, data=data)

@app.delete("/user_course/{id}", tags=["Запись на курс"])
def delete(request: Request, 
           id: int):
     
        return API_UserCourse().delete(request=request, id=id)

#
#Lesson
#

@app.post("/lesson", tags=["Урок"], include_in_schema=False)
def post(request: Request, 
         data: API_Lesson.schema.POST = Depends(API_Lesson.schema.POST)):
     
        return API_Lesson().post(request=request, data=data)

@app.get("/lesson/{id}", tags=["Урок"], response_model=API_Lesson.schema.GET, include_in_schema=False)
def get(request: Request, 
        id: int):
     
        return API_Lesson().get_by_id(request=request, id=id)

@app.get("/lesson", tags=["Урок"], response_model=List[API_Lesson.schema.GET], include_in_schema=False)
def get(request: Request, 
        params: API_Lesson.schema.GET_ALL = Depends()):
     
        return API_Lesson().get(request=request, 
                                params=params)

@app.put("/lesson", tags=["Урок"], include_in_schema=False)
def put(request: Request, 
        data: API_Lesson.schema.PUT = Depends(API_Lesson.schema.PUT)):
     
        return API_Lesson().put(request=request, data=data)

@app.delete("/lesson/{id}", tags=["Урок"], include_in_schema=False)
def delete(request: Request, 
           id: int):
     
        return API_Lesson().delete(request=request, id=id)

#
#Category
#

@app.post("/category", tags=["Категория"], include_in_schema=False)
def post(request: Request, 
         data: API_Category.schema.POST = Depends(API_Category.schema.POST)):
     
        return API_Category().post(request=request, data=data)

@app.get("/category/{id}", tags=["Категория"], response_model=API_Category.schema.GET, include_in_schema=False)
def get(request: Request, 
        id: int):
     
        return API_Category().get_by_id(request=request, id=id)

@app.get("/category", tags=["Категория"], response_model=List[API_Category.schema.GET], include_in_schema=False)
def get(request: Request, 
        params: API_Category.schema.GET_ALL = Depends()):
     
        return API_Category().get(request=request,
                                  params=params)

@app.put("/category", tags=["Категория"], include_in_schema=False)
def put(request: Request, 
        data: API_Category.schema.PUT = Depends(API_Category.schema.PUT)):
     
        return API_Category().put(request=request, data=data)

@app.delete("/category/{id}", tags=["Категория"], include_in_schema=False)
def delete(request: Request, 
           id: int):
     
        return API_Category().delete(request=request, id=id)

#
#Level
#

@app.post("/level", tags=["Уровень сложности"], include_in_schema=False)
def post(request: Request, 
         data: API_Level.schema.POST = Depends(API_Level.schema.POST)):
     
        return API_Level().post(request=request, data=data)

@app.get("/level/{id}", tags=["Уровень сложности"], response_model=API_Category.schema.GET, include_in_schema=False)
def get(request: Request, 
        id: int):
     
        return API_Level().get_by_id(request=request, id=id)

@app.get("/level", tags=["Уровень сложности"], response_model=List[API_Category.schema.GET], include_in_schema=False)
def get(request: Request, 
        params: API_Level.schema.GET_ALL = Depends()):
     
        return API_Level().get(request=request,
                               params=params)

@app.put("/level", tags=["Уровень сложности"], include_in_schema=False)
def put(request: Request, 
        data: API_Level.schema.PUT = Depends(API_Level.schema.PUT)):
     
        return API_Level().put(request=request, data=data)

@app.delete("/level/{id}", tags=["Уровень сложности"], include_in_schema=False)
def delete(request: Request, 
           id: int):  
     
        return API_Level().delete(request=request, id=id)

if __name__ == "__main__":
    uvicorn.run("routes:app", reload=True)
    