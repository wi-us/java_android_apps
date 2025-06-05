from peewee import *
#from playhouse.migrate import *
import logging
from fastapi import HTTPException
import hashlib

db = SqliteDatabase('database.sqlite')
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler()])
logger = logging.getLogger(__name__)

class BaseModel(Model):
    class Meta:
        database = db

    @classmethod
    def create_item(cls, **kwargs):
        try:
            try:
                del kwargs["id"]
            except:
                pass
            return cls.create(**kwargs)
        except BaseException as e:
                return HTTPException(status_code=404, detail=f"{e}")
        
    @classmethod
    def get_item(cls, 
                 params: dict, 
                 limit: int = None, 
                 offset: int = None):
        try:
            query = cls.select()
            if params:
                conditions = []
                for field, value in params.items():
                    try:
                        field_obj = getattr(cls, field)
                        conditions.append(field_obj == value)
                    except AttributeError:
                        return HTTPException(status_code=404, detail=f"Неверное поле: {field}")
                
                query = query.where(*conditions)
            
            if offset is not None:
                query = query.offset(offset)
            
            if limit is not None:
                query = query.limit(limit)
        
            return query
        
        except BaseException as e:
                return HTTPException(status_code=404, detail=f"{e}")
        
    @classmethod
    def delete_item(cls, id):
        try:
            if cls.delete_by_id(id):
                return HTTPException(status_code=200, detail=f"Success")
        except BaseException as e:
            return HTTPException(status_code=404, detail=f"{e}")
    
    @classmethod
    def edit_item(cls, **kwargs):
        try:
            update_data = {key: value for key, value in kwargs.items() if hasattr(cls, key)}
            if cls.update(**update_data).where(cls.id == kwargs['id']).execute():
                return HTTPException(status_code=200, detail=f"Success")
            
        except BaseException as e:
                return HTTPException(status_code=404, detail=f"{e}")

class Role(BaseModel):
    name = CharField(null=False,
                     unique=True)

class Category(BaseModel):
    name = CharField(null=False,
                     unique=True)

class Level(BaseModel):
    name = CharField(null=False,
                     unique=True)

class User(BaseModel):
    name = CharField(null=True)
    surname = CharField(null=True)
    nickname = CharField(null=False,
                         unique=True)
    role_id = ForeignKeyField(Role,
                              null=False,
                              default=3)
    password = CharField(null=False)
    balance = DecimalField(null=False,
                           default=0)
    discount = IntegerField(null=False,
                           default=0)

    @classmethod
    def get_item(cls, params: dict, limit: int = None, offset: int = None):
        try:
            query = cls.select()

            if "role_id" in params:
                try:
                    params["role_id"] = int(params["role_id"])
                except:
                    try:
                        params["role_id"] = Role.get(Role.name == params["role_id"])
                        
                    except AttributeError:
                        return HTTPException(status_code=400, detail="Неверное поле: role_id")
                
            if params:
                conditions = []
                for field, value in params.items():
                    try:
                        field_obj = getattr(cls, field)
                        conditions.append(field_obj == value)
                    except AttributeError:
                        return HTTPException(status_code=400, detail=f"{field}")
                
                query = query.where(*conditions)
            
            if offset is not None:
                query = query.offset(offset)
            
            if limit is not None:
                query = query.limit(limit)
                
            return query
        except BaseException as e:
            return HTTPException(status_code=400, detail=f"{e}")
        
    @classmethod
    def edit_item(cls, **kwargs):
        try:
            update_data = {key: value for key, value in kwargs.items() if hasattr(cls, key)}
            if cls.update(**update_data).where(cls.id == kwargs['id']).execute():
                return HTTPException(status_code=200, detail=f"Success")
            
        except BaseException as e:
                return HTTPException(status_code=404, detail=f"{e}")


class Course(BaseModel):
    name = CharField(null=False)
    description = TextField(null=True)
    author_id = ForeignKeyField(User,
                                null=False)
    category_id = ForeignKeyField(Category,
                                  null=False)
    level_id = ForeignKeyField(Level,
                               null=False)
    price = DecimalField(null=False,
                         default=0)
    isVisible = BooleanField(null=False,
                             default=True)

    @classmethod
    def get_item(cls, params: dict, limit: int = None, offset: int = None):
        try:
            query = cls.select()
            if "author_id" in params:
                try:
                    params["author_id"] = int(params["author_id"])
                except:
                    try:
                        params["author_id"] = User.get(User.name == params["author_id"])
                        
                    except AttributeError:
                        return HTTPException(status_code=400, detail=f"Неверное поле: author_id")
            
            if "level_id" in params:
                try:
                    params["level_id"] = int(params["level_id"])
                except:
                    try:
                        params["level_id"] = Level.get(Level.name == params["level_id"])
                        
                    except AttributeError:
                        return HTTPException(status_code=400, detail=f"Неверное поле: level_id")
                    
            if "category_id" in params:
                try:
                    params["category_id"] = int(params["category_id"])
                except:
                    try:
                        params["category_id"] = Category.get(Category.name == params["category_id"])
                        
                    except AttributeError:
                        return HTTPException(status_code=400, detail=f"Неверное поле: category_id")
                    
            if params:
                conditions = []
                for field, value in params.items():
                    try:
                        field_obj = getattr(cls, field)
                        conditions.append(field_obj == value)
                    except AttributeError:
                        return HTTPException(status_code=400, detail=f"Неверное поле: {field}")
                
                query = query.where(*conditions)
            
            if offset is not None:
                query = query.offset(offset)
            
            if limit is not None:
                query = query.limit(limit)
                
            return query
        
        except BaseException as e:
            return HTTPException(status_code=400, detail=f"{e}")
class UserCourse(BaseModel):
    user_id = ForeignKeyField(User,
                              null=False)
    course_id = ForeignKeyField(Course,
                                null=False)
    date = TimestampField(null=False)
    progress = FloatField(null=False,
                          default=0)

    @classmethod
    def get_item(cls, params: dict, limit: int = None, offset: int = None):
        try:
            query = cls.select()

            
            if "user_id" in params:
                try:
                    params["user_id"] = int(params["user_id"])
                except:
                    try:
                        params["user_id"] = User.get(User.name == params["user_id"])
                        
                    except AttributeError:
                        return HTTPException(status_code=400, detail=f"Неверное поле: user_id")
                    
            if "course_id" in params:
                try:
                    params["course_id"] = int(params["course_id"])
                except:
                    try:
                        params["course_id"] = Course.get(Course.name == params["course_id"])
                        
                    except AttributeError:
                        return HTTPException(status_code=400, detail=f"Неверное поле: course_id")
                    
            if params:
                conditions = []
                for field, value in params.items():
                    try:
                        field_obj = getattr(cls, field)
                        conditions.append(field_obj == value)
                    except AttributeError:
                        return HTTPException(status_code=400, detail=f"Неверное поле: {field}")
                
                query = query.where(*conditions)
            
            if offset is not None:
                query = query.offset(offset)
            
            if limit is not None:
                query = query.limit(limit)
                
                
            return query
        except BaseException as e:
            return HTTPException(status_code=400, detail=f"{e}")
class Lesson(BaseModel):
    course_id = ForeignKeyField(Course,
                                null=False,
                                unique=True)
    name = CharField(null=False)
    video_url = CharField(null=True)
    duration = IntegerField(null=True,
                            default=0)
    order = IntegerField(null=False,
                         default=1)

    @classmethod
    def get_item(cls, params: dict, limit: int = None, offset: int = None):
        try:
            query = cls.select()


            if "course_id" in params:
                try:
                    params["course_id"] = int(params["course_id"])
                except:
                    try:
                        params["course_id"] = Course.get(Course.name == params["course_id"])
                        
                    except AttributeError:
                        return HTTPException(status_code=400, detail=f"Неверное поле: course_id")
                    
            if params:
                conditions = []
                for field, value in params.items():
                    try:
                        field_obj = getattr(cls, field)
                        conditions.append(field_obj == value)
                    except AttributeError:
                        return HTTPException(status_code=400, detail=f"Неверное поле: {field}")
                
                query = query.where(*conditions)
            
            if offset is not None:
                query = query.offset(offset)
            
            if limit is not None:
                query = query.limit(limit)
                
                
            return query
        except BaseException as e:
            return HTTPException(status_code=400, detail=f"{e}")

# migrator = SqliteMigrator(db)  # для SQLite
# migrate(
#         migrator.add_column('user', 'balance', User.balance),
#         migrator.add_column('user', 'discount', User.discount)
#     )
Role().create_table()
Category().create_table()
Level().create_table()
User().create_table()
Course().create_table()
UserCourse().create_table()
Lesson().create_table()
