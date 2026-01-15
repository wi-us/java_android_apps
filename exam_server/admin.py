from fastapi import APIRouter, Request, Depends, HTTPException, Response, Form
from fastapi.encoders import jsonable_encoder
from fastapi.responses import HTMLResponse, RedirectResponse, JSONResponse
from fastapi.templating import Jinja2Templates
from peewee import *
from playhouse.shortcuts import model_to_dict

import database as db
import schemas
import auth

# --- Черный список полей для таблиц админки ---
BLACKLIST_FIELDS = [
    'variants', 'images', 'created_at', 'updated_at', 'stock', 'image_link',
    'password', 'items', 'password_hash', 'role_name'
]

# --- Human-readable labels for FK options / table display ---
def _fk_label(model_cls, row_dict: dict) -> str:
    """
    Возвращает человекочитаемую подпись для строки связанной таблицы.
    row_dict приходит из peewee .dicts() (ключи = колонки).
    """
    if model_cls == db.AgeRating:
        # показываем не id, а 12/14/18 и т.п.
        v = row_dict.get("min_age")
        return f"{v}+" if v is not None else str(row_dict.get("id"))
    if model_cls == db.GameVariant:
        # variant label: "Игра — Edition Name (#id)"
        # game хранится как game_id в dicts(), поэтому аккуратно подтянем title
        gv_id = row_dict.get("id")
        game_id = row_dict.get("game") or row_dict.get("game_id")
        game_title = None
        try:
            if game_id:
                g = db.Game.get_by_id(game_id)
                game_title = g.title
        except Exception:
            game_title = None
        ed = row_dict.get("edition_name") or ""
        base = game_title or f"Game#{game_id}" if game_id else "Game"
        return f"{base} — {ed} (#{gv_id})" if ed else f"{base} (#{gv_id})"
    # common fallbacks
    for k in ("name", "title", "login", "email"):
        if row_dict.get(k):
            return str(row_dict.get(k))
    return str(row_dict.get("id"))

# --- Вспомогательные функции ---
def get_all_fields_from_schema(schema):
    """Извлекает все имена полей из Pydantic схемы, исключая поля из черного списка."""
    if not hasattr(schema, 'model_fields'): return []
    return [field for field in schema.model_fields.keys() if field not in BLACKLIST_FIELDS]

# --- Карта моделей ---
MODEL_MAP = {
    "users": {"model": db.User, "singular": "User", "plural": "Users", "columns": get_all_fields_from_schema(schemas.UserSchema)},
    "roles": {"model": db.Role, "singular": "Role", "plural": "Roles", "columns": get_all_fields_from_schema(schemas.RoleSchema)},
    "addresses": {"model": db.Address, "singular": "Address", "plural": "Addresses", "columns": get_all_fields_from_schema(schemas.AddressSchema)},
    "genres": {"model": db.Genre, "singular": "Genre", "plural": "Genres", "columns": get_all_fields_from_schema(schemas.GenreSchema)},
    "languages": {"model": db.Language, "singular": "Language", "plural": "Languages", "columns": get_all_fields_from_schema(schemas.LanguageSchema)},
    "age_ratings": {"model": db.AgeRating, "singular": "Age Rating", "plural": "Age Ratings", "columns": get_all_fields_from_schema(schemas.AgeRatingSchema)},
    "statuses": {"model": db.Status, "singular": "Status", "plural": "Statuses", "columns": get_all_fields_from_schema(schemas.StatusSchema)},
    "games": {"model": db.Game, "singular": "Game", "plural": "Games", "columns": get_all_fields_from_schema(schemas.GameSchema)},
    "game_genres": {"model": db.GameGenre, "singular": "Game Genre", "plural": "Game Genres", "columns": ["game", "genre"]}, # Ручное управление для простоты
    "game_variants": {"model": db.GameVariant, "singular": "Game Variant", "plural": "Game Variants", "columns": get_all_fields_from_schema(schemas.GameVariantSchema)},
    "variant_images": {"model": db.VariantImage, "singular": "Variant Image", "plural": "Variant Images", "columns": get_all_fields_from_schema(schemas.VariantImageSchema)},
    "carts": {"model": db.Cart, "singular": "Cart", "plural": "Carts", "columns": get_all_fields_from_schema(schemas.CartSchema)},
    "cart_items": {"model": db.CartItem, "singular": "Cart Item", "plural": "Cart Items", "columns": get_all_fields_from_schema(schemas.CartItemSchema)},
    "orders": {"model": db.Order, "singular": "Order", "plural": "Orders", "columns": get_all_fields_from_schema(schemas.OrderSchema)},
    "order_items": {"model": db.OrderItem, "singular": "Order Item", "plural": "Order Items", "columns": get_all_fields_from_schema(schemas.OrderItemSchema)},
}

def get_model_fields(model):
    fields = []
    schema_create_name = f"{model.__name__}Create"
    create_schema = getattr(schemas, schema_create_name, None)
    if not create_schema: return []
    for name, schema_field in create_schema.model_fields.items():
        field_info = {"name": name, "type": "text", "required": schema_field.is_required()}
        db_field = model._meta.fields.get(name.replace('_id', ''))
        if isinstance(db_field, ForeignKeyField):
            field_info["type"] = "select"; field_info["options_model"] = db_field.rel_model
        elif isinstance(db_field, BooleanField):
            field_info["type"] = "checkbox"
        fields.append(field_info)
    return fields

for key, value in MODEL_MAP.items():
    value["form_fields"] = get_model_fields(value["model"])

templates = Jinja2Templates(directory="templates")
admin_auth_router = APIRouter()
admin_router = APIRouter(dependencies=[Depends(auth.get_current_admin_user)])

# --- Публичные роуты (без изменений) ---
@admin_auth_router.get("/login", response_class=HTMLResponse)
async def login_page(request: Request):
    return templates.TemplateResponse("login.html", {"request": request})

@admin_auth_router.post("/login", response_class=RedirectResponse)
async def login_form(response: Response, login: str = Form(...), password: str = Form(...)):
    user = db.User.get_or_none(db.User.login == login)
    if not user or not auth.verify_password(password, user.password_hash) or not user.role or user.role.name != 'admin':
        return RedirectResponse(url="/login?error=true", status_code=302)
    access_token = auth.create_access_token(data={"sub": str(user.id)})
    response = RedirectResponse(url="/", status_code=303); response.set_cookie(key=auth.COOKIE_NAME, value=access_token, httponly=True)
    return response

@admin_auth_router.get("/logout", response_class=RedirectResponse)
async def logout(request: Request):
    response = RedirectResponse(url="/login"); response.delete_cookie(key=auth.COOKIE_NAME)
    return response

# --- Защищенные роуты (без изменений) ---
@admin_router.get("/", response_class=RedirectResponse)
async def admin_root():
    return RedirectResponse(url=f"/{list(MODEL_MAP.keys())[0]}")

@admin_router.get("/{model_name}", response_class=HTMLResponse)
async def list_items(request: Request, model_name: str):
    config = MODEL_MAP.get(model_name)
    if not config: raise HTTPException(status_code=404)
    model = config["model"]
    items_query = model.select()
    items = []
    for item in items_query:
        item_dict = model_to_dict(item, recurse=False, backrefs=False)
        # Добавляем *_id ключи для ForeignKey (например, game_variant_id),
        # чтобы они могли отображаться в таблице и совпадали с названиями полей в схемах.
        for field in model._meta.fields.values():
            if isinstance(field, ForeignKeyField):
                if field.name in item_dict and field.column_name not in item_dict:
                    item_dict[field.column_name] = item_dict[field.name]
                # И для таблицы: вместо id покажем человекочитаемое значение.
                # Например, Game.age_rating -> "12+".
                try:
                    fk_id = item_dict.get(field.name)
                    # В таблицах колонки могут быть как по имени FK поля (field.name),
                    # так и по имени *_id (field.column_name) — в зависимости от схемы.
                    if fk_id and (field.name in config["columns"] or field.column_name in config["columns"]):
                        rel = field.rel_model.get_by_id(fk_id)
                        if field.rel_model == db.AgeRating:
                            label = f"{rel.min_age}+"
                        elif hasattr(rel, "name"):
                            label = rel.name
                        elif hasattr(rel, "title"):
                            label = rel.title
                        elif hasattr(rel, "login"):
                            label = rel.login
                        else:
                            label = fk_id

                        # Заполняем оба ключа (FK поле и *_id), чтобы таблица/модалка были консистентны
                        item_dict[field.name] = label
                        item_dict[field.column_name] = label
                except Exception:
                    pass
        items.append(item_dict)
    for field in config["form_fields"]:
        if "options_model" in field:
            opts = list(field["options_model"].select().dicts())
            # добавим label для красивых select'ов (AgeRating, GameVariant, etc.)
            for o in opts:
                try:
                    o["label"] = _fk_label(field["options_model"], o)
                except Exception:
                    pass
            field["options"] = opts
    return templates.TemplateResponse("list.html", {"request": request, "items": items, "config": config, "model_name": model_name, "models": MODEL_MAP.keys()})

@admin_router.get("/{model_name}/{item_id}", response_class=JSONResponse)
async def get_item_data(model_name: str, item_id: int):
    config = MODEL_MAP.get(model_name)
    if not config: raise HTTPException(status_code=404)
    try:
        item = config["model"].get_by_id(item_id)
        item_dict = model_to_dict(item, recurse=False)
        final_dict = {}
        for key, value in item_dict.items():
            field_obj = config["model"]._meta.fields.get(key)
            if isinstance(field_obj, ForeignKeyField):
                final_dict[field_obj.column_name] = value 
            else: final_dict[key] = value
        if 'id' not in final_dict: final_dict['id'] = item.get_id()
        return JSONResponse(content=jsonable_encoder(final_dict))
    except DoesNotExist: raise HTTPException(status_code=404)

@admin_router.post("/{model_name}/save", response_class=RedirectResponse)
async def save_item(request: Request, model_name: str):
    config = MODEL_MAP.get(model_name)
    if not config: raise HTTPException(status_code=404)
    model = config["model"]
    form_data = await request.form()
    data = {}
    for key, value in form_data.items():
        if value: 
            model_field_name = key[:-3] if key.endswith('_id') else key
            if key.endswith('_id') and model_field_name in model._meta.fields:
                data[model_field_name] = value
            else: data[key] = value
    item_id = data.pop("id", None)
    for field in config["form_fields"]:
        if field['type'] == 'checkbox':
            data[field['name']] = field['name'] in data
    password = data.pop('password', None)
    if model == db.User and not password and item_id: pass 
    if item_id: 
        if model == db.User and password:
            instance = model.get_by_id(item_id); instance.set_password(password); instance.save()
        query = model.update(**data).where(model.id == item_id); query.execute()
    else: 
        if model == db.User and password:
            instance = model.create(**data); instance.set_password(password); instance.save()
        else: model.create(**data)
    return RedirectResponse(url=f"/{model_name}", status_code=303)

@admin_router.get("/{model_name}/delete/{item_id}", response_class=RedirectResponse)
async def delete_item_admin(model_name: str, item_id: int):
    config = MODEL_MAP.get(model_name)
    if not config: raise HTTPException(status_code=404)
    try:
        item = config["model"].get_by_id(item_id); item.delete_instance(recursive=True)
    except DoesNotExist: pass
    return RedirectResponse(url=f"/{model_name}", status_code=303)
