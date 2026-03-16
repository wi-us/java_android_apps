from fastapi import APIRouter, HTTPException, Request, Depends, Response, Query
from peewee import DoesNotExist
from playhouse.shortcuts import model_to_dict
from typing import List, Optional
import schemas
import database as db
import auth
import requests
from datetime import datetime
import os

router = APIRouter()


# --- Мобильная корзина (в БД) ---

@router.get("/cart", tags=["Cart"])
def get_my_cart(request: Request, response: Response, current_user: db.User = Depends(auth.get_current_user)):
    """
    Возвращает корзину текущего пользователя (создаёт при отсутствии).
    """
    cart, _ = db.Cart.get_or_create(user=current_user)
    base_url = str(request.base_url).rstrip("/")

    items_out = []
    total = 0.0
    for ci in db.CartItem.select().where(db.CartItem.cart == cart):
        variant_dict = build_variant_dict(ci.game_variant, base_url=base_url)
        price = float(ci.game_variant.discount_price or ci.game_variant.price or 0)
        line_total = price * int(ci.quantity or 0)
        total += line_total
        items_out.append(
            {
                "id": ci.id,
                "quantity": ci.quantity,
                "game_variant": variant_dict,
                "line_total": line_total,
            }
        )
    return {"id": cart.id, "items": items_out, "total": total}


# --- Профиль текущего пользователя (mobile) ---
@router.get("/me", response_model=schemas.UserSchema, tags=["User"])
def get_me(request: Request, response: Response, current_user: db.User = Depends(auth.get_current_user)):
    return current_user


# --- Обновление профиля (mobile) ---
@router.put("/me", response_model=schemas.UserSchema, tags=["User"])
def update_me(
    data: dict,
    request: Request,
    response: Response,
    current_user: db.User = Depends(auth.get_current_user)
):
    """
    Обновление профиля текущего пользователя.
    Можно изменить: email, first_name, last_name, phone_number.
    Нельзя изменить: login, password, role.
    """
    allowed_fields = ["email", "first_name", "last_name", "phone_number"]
    
    for field in allowed_fields:
        if field in data and data[field] is not None:
            setattr(current_user, field, data[field])
    
    current_user.save()
    return current_user


# --- Заказы текущего пользователя (mobile) ---
@router.get("/my_orders", tags=["Order"])
def get_my_orders(request: Request, response: Response, current_user: db.User = Depends(auth.get_current_user)):
    """
    Возвращает список заказов текущего пользователя.
    """
    base_url = str(request.base_url).rstrip("/")
    orders = list(db.Order.select().where(db.Order.user == current_user).order_by(db.Order.created_at.desc()))
    
    result = []
    for order in orders:
        order_data = {
            "id": order.id,
            "total_price": float(order.total_price),
            "created_at": order.created_at.isoformat() if order.created_at else None,
            "status": {"id": order.status.id, "name": order.status.name} if order.status else None,
            "address": order.address.full_address if order.address and order.address.full_address else None,
            "comment": order.comment,
        }
        result.append(order_data)
    
    return result


# --- Детали заказа (mobile) ---
@router.get("/my_orders/{order_id}", tags=["Order"])
def get_my_order_detail(order_id: int, request: Request, response: Response, current_user: db.User = Depends(auth.get_current_user)):
    """
    Возвращает детали заказа с позициями.
    """
    base_url = str(request.base_url).rstrip("/")
    
    order = db.Order.get_or_none((db.Order.id == order_id) & (db.Order.user == current_user))
    if not order:
        raise HTTPException(status_code=404, detail="Order not found")
    
    # Получаем позиции заказа
    items = []
    for item in db.OrderItem.select().where(db.OrderItem.order == order):
        variant = item.game_variant
        variant_data = build_variant_dict(variant, base_url=base_url) if variant else None
        items.append({
            "id": item.id,
            "quantity": item.quantity,
            "price_per_item": float(item.price_per_item),
            "subtotal": float(item.subtotal),
            "game_variant": variant_data,
        })
    
    return {
        "id": order.id,
        "total_price": float(order.total_price),
        "created_at": order.created_at.isoformat() if order.created_at else None,
        "status": {"id": order.status.id, "name": order.status.name} if order.status else None,
        "address": order.address.full_address if order.address and order.address.full_address else None,
        "comment": order.comment,
        "items": items,
    }


def _dgis_key(request: Request | None = None) -> str:
    """
    Сначала берём ключ из env (правильно для сервера),
    но для учебного проекта разрешаем fallback из заголовка X-DGIS-KEY (удобно для мобильного).
    """
    key = os.getenv("DGIS_API_KEY") or os.getenv("2GIS_API_KEY")
    if not key and request is not None:
        key = request.headers.get("X-DGIS-KEY") or request.headers.get("X-2GIS-KEY")
    if not key:
        raise HTTPException(
            status_code=500,
            detail="DGIS_API_KEY is not set (and X-DGIS-KEY header missing). Set env var on server or pass X-DGIS-KEY from app.",
        )
    return key


# --- Подсказки адреса (2ГИС Suggest API) ---
@router.get("/address_suggest", response_model=List[schemas.AddressSuggestion], tags=["Address"])
def address_suggest(
    q: str,
    request: Request,
    response: Response,
    current_user: db.User = Depends(auth.get_current_user),
):
    query = (q or "").strip()
    if len(query) < 2:
        return []

    key = _dgis_key(request)
    try:
        r = requests.get(
            "https://catalog.api.2gis.com/3.0/suggests",
            params={
                "q": query,
                "suggest_type": "address",
                "locale": "ru_RU",
                "key": key,
            },
            timeout=6,
        )
        if r.status_code != 200:
            return []
        data = r.json() or {}
        items = (((data.get("result") or {}) if isinstance(data.get("result"), dict) else {}).get("items")) or []
        out = []
        for it in items[:8]:
            it = it or {}
            text = it.get("full_name") or it.get("name")
            point = it.get("point") if isinstance(it.get("point"), dict) else {}
            lat = point.get("lat")
            lon = point.get("lon") or point.get("lng")
            if not text:
                continue
            # чаще всего full_name начинается с "Россия, ..." — для UI это шум
            if isinstance(text, str) and text.lower().startswith("россия,"):
                text = text.split(",", 1)[1].strip()
            out.append({"id": it.get("id"), "text": text, "lat": lat, "lon": lon})
        return out
    except Exception:
        return []


@router.get("/address_details", response_model=schemas.AddressDetails, tags=["Address"])
def address_details(
    q: str,
    request: Request,
    response: Response,
    current_user: db.User = Depends(auth.get_current_user),
):
    query = (q or "").strip()
    if len(query) < 2:
        raise HTTPException(status_code=400, detail="q is required")

    key = _dgis_key(request)
    try:
        r = requests.get(
            "https://catalog.api.2gis.com/3.0/items/geocode",
            params={
                "key": key,
                "q": query,
                "locale": "ru_RU",
            },
            timeout=6,
        )
        if r.status_code != 200:
            raise HTTPException(status_code=502, detail="2GIS geocoder error")
        data = r.json() or {}
        items = (((data.get("result") or {}) if isinstance(data.get("result"), dict) else {}).get("items")) or []
        if not items:
            return {"id": None, "text": query, "lat": None, "lon": None}
        it = items[0] or {}
        point = it.get("point") if isinstance(it.get("point"), dict) else {}
        lat = point.get("lat")
        lon = point.get("lon") or point.get("lng")
        text = it.get("full_name") or it.get("name") or query
        if isinstance(text, str) and text.lower().startswith("россия,"):
            text = text.split(",", 1)[1].strip()
        return {"id": it.get("id"), "text": text, "lat": lat, "lon": lon}
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(status_code=500, detail="Failed to resolve address")


@router.get("/address_reverse", response_model=schemas.AddressDetails, tags=["Address"])
def address_reverse(
    lat: float,
    lon: float,
    request: Request,
    response: Response,
    current_user: db.User = Depends(auth.get_current_user),
):
    """
    Reverse geocode по координатам (для выбора точки на карте).
    Параметры lat/lon приходят из приложения.
    """
    key = _dgis_key(request)
    try:
        r = requests.get(
            "https://catalog.api.2gis.com/3.0/items/geocode",
            params={
                "key": key,
                # во многих примерах 2ГИС принимает point=lon,lat
                "point": f"{lon},{lat}",
                "locale": "ru_RU",
            },
            timeout=6,
        )
        if r.status_code != 200:
            raise HTTPException(status_code=502, detail="2GIS reverse geocoder error")
        data = r.json() or {}
        items = (((data.get("result") or {}) if isinstance(data.get("result"), dict) else {}).get("items")) or []
        if not items:
            return {"id": None, "text": "", "lat": lat, "lon": lon}
        it = items[0] or {}
        text = it.get("full_name") or it.get("name") or ""
        if isinstance(text, str) and text.lower().startswith("россия,"):
            text = text.split(",", 1)[1].strip()
        point = it.get("point") if isinstance(it.get("point"), dict) else {}
        out_lat = point.get("lat") or lat
        out_lon = point.get("lon") or point.get("lng") or lon
        return {"id": it.get("id"), "text": text, "lat": out_lat, "lon": out_lon}
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(status_code=500, detail="Failed to reverse geocode")


# --- Оформление заказа (из текущей корзины) ---
@router.post("/orders/checkout", response_model=schemas.CheckoutResponse, tags=["Order"])
def checkout(
    data: schemas.CheckoutRequest,
    request: Request,
    response: Response,
    current_user: db.User = Depends(auth.get_current_user),
):
    address_text = (data.address or "").strip()
    apartment = (data.apartment or "").strip() if getattr(data, "apartment", None) else ""
    if apartment:
        address_text = f"{address_text}, кв. {apartment}"
    name = (data.name or "").strip()
    phone = (data.phone or "").strip() if data.phone else None
    comment = (data.comment or "").strip() if data.comment else None

    if not address_text:
        raise HTTPException(status_code=400, detail="address is required")
    if not name:
        raise HTTPException(status_code=400, detail="name is required")

    cart, _ = db.Cart.get_or_create(user=current_user)
    cart_items = list(db.CartItem.select().where(db.CartItem.cart == cart))
    if not cart_items:
        raise HTTPException(status_code=400, detail="cart is empty")

    # обновляем телефон пользователя (по ТЗ)
    if phone is not None and phone != (current_user.phone_number or None):
        current_user.phone_number = phone
        current_user.save()

    # Парсим адрес для сохранения в отдельных полях
    address_parts = [p.strip() for p in address_text.split(",") if p.strip()]
    city = address_parts[0] if len(address_parts) > 0 else ""
    street = address_parts[1] if len(address_parts) > 1 else ""
    house = address_parts[2] if len(address_parts) > 2 else ""
    
    addr = db.Address.create(
        user=current_user,
        country="Россия",
        city=city,
        street=street,
        house_number=house,
        apartment_number=apartment if apartment else None,
        postal_code="",
        full_address=address_text,
    )

    # статус заказа (используем ту же таблицу Status, чтобы не менять схему БД радикально)
    status, _ = db.Status.get_or_create(name="Новый заказ")

    total = 0.0
    with db.db.atomic():
        # считаем total и создаём позиции
        for ci in cart_items:
            price = float(ci.game_variant.discount_price or ci.game_variant.price or 0)
            qty = int(ci.quantity or 0)
            total += price * qty

        order = db.Order.create(
            user=current_user,
            address=addr,
            status=status,
            total_price=total,
            created_at=datetime.now(),
            comment=comment,
        )

        for ci in cart_items:
            price = float(ci.game_variant.discount_price or ci.game_variant.price or 0)
            qty = int(ci.quantity or 0)
            subtotal = price * qty
            db.OrderItem.create(
                order=order,
                game_variant=ci.game_variant,
                quantity=qty,
                price_per_item=price,
                discount_per_item=0,
                subtotal=subtotal,
            )

        # очищаем корзину после оформления
        db.CartItem.delete().where(db.CartItem.cart == cart).execute()

    return {"order_id": int(order.id), "total": float(total)}


@router.post("/cart/items", tags=["Cart"])
def add_to_cart(
    data: dict,
    request: Request,
    response: Response,
    current_user: db.User = Depends(auth.get_current_user),
):
    """
    Тело: { "game_variant_id": int, "quantity": int? }.
    Если позиция уже есть — увеличиваем quantity.
    """
    gv_id = int(data.get("game_variant_id"))
    qty = int(data.get("quantity") or 1)
    if qty <= 0:
        raise HTTPException(status_code=400, detail="quantity must be > 0")

    cart, _ = db.Cart.get_or_create(user=current_user)
    variant = db.GameVariant.get_or_none(db.GameVariant.id == gv_id)
    if not variant:
        raise HTTPException(status_code=404, detail="GameVariant not found")

    ci = db.CartItem.get_or_none((db.CartItem.cart == cart) & (db.CartItem.game_variant == variant))
    if ci:
        ci.quantity = int(ci.quantity or 0) + qty
        ci.save()
    else:
        ci = db.CartItem.create(cart=cart, game_variant=variant, quantity=qty)

    # возвращаем обновлённую корзину (проще для клиента)
    return get_my_cart(request, response, current_user)


@router.put("/cart/items/{game_variant_id}", tags=["Cart"])
def set_cart_item_quantity(
    game_variant_id: int,
    data: dict,
    request: Request,
    response: Response,
    current_user: db.User = Depends(auth.get_current_user),
):
    """
    Тело: { "quantity": int }.
    quantity <= 0 => удалить позицию.
    """
    qty = int(data.get("quantity") or 0)
    cart, _ = db.Cart.get_or_create(user=current_user)
    ci = db.CartItem.get_or_none((db.CartItem.cart == cart) & (db.CartItem.game_variant_id == game_variant_id))
    if not ci:
        # если нет позиции — просто вернём корзину
        return get_my_cart(request, response, current_user)

    if qty <= 0:
        ci.delete_instance()
    else:
        ci.quantity = qty
        ci.save()

    return get_my_cart(request, response, current_user)

# Карта моделей для админки и универсальных эндпоинтов
MODEL_PATHS = {
    db.Role: "roles",
    db.Genre: "genres",
    db.Language: "languages",
    db.AgeRating: "age_ratings",
    db.Status: "statuses",
    db.User: "users",
    db.Address: "addresses",
    db.Game: "games",
    db.GameGenre: "game_genres",
    db.GameVariant: "game_variants",
    db.VariantImage: "variant_images",
    db.Cart: "carts",
    db.CartItem: "cart_items",
    db.Order: "orders",
    db.OrderItem: "order_items",
}

# --- Универсальные CRUD операции (без изменений) ---
def create_item(model, schema_create):
    if model == db.User:
        user_data = schema_create.dict(); plain_password = user_data.pop("password")
        user = model(**user_data)
        user.set_password(plain_password)
        # safety: если по каким-то причинам registration_date не проставился
        if hasattr(user, "registration_date") and getattr(user, "registration_date", None) is None:
            from datetime import datetime
            user.registration_date = datetime.now()
        user.save()
        return user
    return model.create(**schema_create.dict())

def get_item(model, item_id: int):
    item = model.get_or_none(model.id == item_id)
    if not item: raise HTTPException(status_code=404, detail=f"{model.__name__} not found")
    return item

def get_all_items(model): return list(model.select())

def update_item(model, item_id: int, schema_update):
    try:
        query = model.update(**schema_update.dict(exclude_unset=True)).where(model.id == item_id)
        query.execute(); return get_item(model, item_id)
    except DoesNotExist: raise HTTPException(status_code=404, detail=f"{model.__name__} not found")

def delete_item(model, item_id: int):
    try:
        item = model.get_by_id(item_id); item.delete_instance()
        return {"detail": f"{model.__name__} deleted successfully"}
    except DoesNotExist: raise HTTPException(status_code=404, detail=f"{model.__name__} not found")


# --- НОВАЯ НАДЕЖНАЯ ЛОГИКА СБОРКИ ДАННЫХ ---

def _to_absolute_url(url: str | None, base_url: str | None) -> str | None:
    if not url:
        return url
    if not base_url:
        return url
    if url.startswith("http://") or url.startswith("https://"):
        return url
    b = base_url.rstrip("/")
    if url.startswith("/"):
        return f"{b}{url}"
    return f"{b}/{url}"


def build_variant_dict(variant: db.GameVariant, base_url: str | None = None) -> dict:
    """
    Вручную собирает словарь для варианта, принудительно загружая все связанные данные.
    """
    # 1. Преобразуем основной объект варианта в словарь
    variant_data = model_to_dict(variant, backrefs=False, recurse=False)
    
    # 2. Вручную добавляем связанные объекты, превращая их в словари
    # Важно: mobile ожидает game.min_players/max_players и game.age_rating как объект, а не id
    game_data = model_to_dict(variant.game, backrefs=False, recurse=False)
    if getattr(variant.game, "age_rating_id", None):
        game_data["age_rating"] = model_to_dict(variant.game.age_rating, backrefs=False, recurse=False)
    else:
        game_data["age_rating"] = None
    variant_data["game"] = game_data
    if variant.language: variant_data['language'] = model_to_dict(variant.language)
    if variant.status: variant_data['status'] = model_to_dict(variant.status)
    
    # 3. Загружаем главное изображение (cover image)
    # Сравниваем по *_id, чтобы избежать нюансов сравнения FK по объектам
    cover_image = (
        db.VariantImage.select()
        .where((db.VariantImage.game_variant_id == variant.id) & (db.VariantImage.is_cover == True))
        .order_by(db.VariantImage.sort_order.asc())
        .first()
    )
    if not cover_image:
        cover_image = (
            db.VariantImage.select()
            .where(db.VariantImage.game_variant_id == variant.id)
            .order_by(db.VariantImage.sort_order.asc())
            .first()
        )
    variant_data["image_link"] = _to_absolute_url(cover_image.url, base_url) if cover_image else None
    
    # 4. Загружаем все изображения для слайдера
    # Важно: VariantImageSchema ожидает game_variant_id (а model_to_dict отдаёт game_variant)
    variant_images = (
        db.VariantImage.select()
        .where(db.VariantImage.game_variant_id == variant.id)
        .order_by(db.VariantImage.sort_order.asc())
    )
    variant_data["images"] = [
        {
            "id": img.id,
            "game_variant_id": img.game_variant_id,
            "url": _to_absolute_url(img.url, base_url),
            "is_cover": img.is_cover,
            "sort_order": img.sort_order,
        }
        for img in variant_images
    ]

    # 5. Парсим dimensions_mm → box_width_mm, box_height_mm, box_depth_mm
    dims = getattr(variant, "dimensions_mm", None) or ""
    parts = [p.strip() for p in dims.replace("x", "x").split("x") if p.strip().isdigit()]
    variant_data["box_width_mm"] = int(parts[0]) if len(parts) > 0 else None
    variant_data["box_height_mm"] = int(parts[1]) if len(parts) > 1 else None
    variant_data["box_depth_mm"] = int(parts[2]) if len(parts) > 2 else None

    # 6. Жанры игры
    try:
        genre_links = db.GameGenre.select().where(db.GameGenre.game == variant.game)
        variant_data["genres"] = [gl.genre.name for gl in genre_links]
    except Exception:
        variant_data["genres"] = []

    return variant_data

# --- Кастомные эндпоинты, использующие новую логику ---

def _variant_matches_search(variant: db.GameVariant, q_lower: str) -> bool:
    """Поиск только по названию товара (название игры) без учёта регистра."""
    title = (variant.game.title or "") if variant.game else ""
    return title.lower().find(q_lower) >= 0


def _variant_matches_filters(
    variant: db.GameVariant,
    min_players: Optional[int],
    max_players: Optional[int],
    min_age: Optional[int],
    price_min: Optional[float],
    price_max: Optional[float],
    in_stock: Optional[bool],
    genres: Optional[List[str]],
    complexity: Optional[str],
    playtime_min: Optional[int],
    playtime_max: Optional[int],
) -> bool:
    """Проверка варианта по фильтрам: игроки, возраст, цена, наличие, жанры, сложность, длительность."""
    game = variant.game
    # Фильтр по игрокам: строгое соответствие диапазону [min_players, max_players]
    if min_players is not None and game and game.min_players is not None:
        if game.min_players < min_players:
            return False
    if max_players is not None and game and game.max_players is not None:
        if game.max_players > max_players:
            return False
    if min_age is not None and game and getattr(game, "age_rating_id", None):
        ar = game.age_rating
        if ar is not None and getattr(ar, "min_age", None) is not None and ar.min_age < min_age:
            return False
    actual_price = float(variant.discount_price if variant.discount_price is not None else variant.price)
    if price_min is not None and actual_price < price_min:
        return False
    if price_max is not None and actual_price > price_max:
        return False
    if in_stock:
        if not variant.status_id:
            return False
        st = variant.status
        if st is None or (st.name or "").strip().lower() != "в наличии":
            return False
    # Фильтр по жанрам: игра должна иметь хотя бы один из выбранных жанров
    if genres:
        genre_links = db.GameGenre.select().where(db.GameGenre.game == variant.game)
        game_genres = {gl.genre.name for gl in genre_links}
        if not game_genres.intersection(set(genres)):
            return False
    # Фильтр по сложности
    if complexity:
        v_complexity = (variant.complexity or "").strip()
        if v_complexity.lower() != complexity.strip().lower():
            return False
    # Фильтр по длительности
    if playtime_min is not None and game and game.playtime_avg is not None:
        if game.playtime_avg < playtime_min:
            return False
    if playtime_max is not None and game and game.playtime_avg is not None:
        if game.playtime_avg > playtime_max:
            return False
    return True


@router.get("/genres", response_model=List[schemas.GenreSchema], tags=["Genre"])
def get_genres_for_filter():
    """Возвращает список жанров, у которых есть хотя бы одна игра (для фильтров)."""
    used_genre_ids = {gg.genre_id for gg in db.GameGenre.select()}
    genres = list(db.Genre.select().where(db.Genre.id.in_(used_genre_ids)).order_by(db.Genre.name))
    return genres


@router.get("/game_variants", response_model=List[schemas.GameVariantSchema], tags=["GameVariant"])
def get_all_game_variants_custom(
    request: Request,
    q: Optional[str] = Query(None),
    min_players: Optional[int] = Query(None),
    max_players: Optional[int] = Query(None),
    min_age: Optional[int] = Query(None),
    price_min: Optional[float] = Query(None),
    price_max: Optional[float] = Query(None),
    in_stock: Optional[bool] = Query(None),
    genres: Optional[str] = Query(None),       # жанры через запятую: "Стратегия,Евро"
    complexity: Optional[str] = Query(None),   # "Лёгкая" / "Средняя" / "Сложная" / "Хардкор"
    playtime_min: Optional[int] = Query(None),
    playtime_max: Optional[int] = Query(None),
):
    """Список вариантов: поиск по названию + фильтры по игрокам, возрасту, цене, наличию, жанрам, сложности, длительности."""
    variants = list(db.GameVariant.select().join(db.Game))
    if q and q.strip():
        q_clean = q.strip().lower()
        variants = [v for v in variants if _variant_matches_search(v, q_clean)]

    genres_list: Optional[List[str]] = None
    if genres and genres.strip():
        genres_list = [g.strip() for g in genres.split(",") if g.strip()]

    if any(x is not None for x in (min_players, max_players, min_age, price_min, price_max, in_stock, genres_list, complexity, playtime_min, playtime_max)):
        variants = [
            v for v in variants
            if _variant_matches_filters(v, min_players, max_players, min_age, price_min, price_max, in_stock, genres_list, complexity, playtime_min, playtime_max)
        ]
    base_url = str(request.base_url).rstrip("/")
    response_data = [build_variant_dict(v, base_url=base_url) for v in variants]
    return response_data

@router.get("/game_variants/{item_id}", response_model=schemas.GameVariantSchema, tags=["GameVariant"])
def get_single_game_variant_custom(item_id: int, request: Request):
    """Отдает один вариант по ID, вручную подгружая связанные данные (для детального экрана)."""
    variant = get_item(db.GameVariant, item_id)
    base_url = str(request.base_url).rstrip("/")
    return build_variant_dict(variant, base_url=base_url)

@router.get("/games/{item_id}", response_model=schemas.GameSchema, tags=["Game"])
def get_single_game_custom(item_id: int, request: Request):
    """Отдает одну игру по ID, включая все ее варианты с их связанными данными."""
    game = get_item(db.Game, item_id)
    game_data = model_to_dict(game, exclude=[db.Game.variants]) # Конвертируем игру без вариантов
    if getattr(game, "age_rating_id", None):
        game_data["age_rating"] = model_to_dict(game.age_rating, backrefs=False, recurse=False)
    else:
        game_data["age_rating"] = None
    # Вручную собираем варианты через новую функцию
    base_url = str(request.base_url).rstrip("/")
    game_data['variants'] = [build_variant_dict(v, base_url=base_url) for v in game.variants]
    return game_data

# --- Генератор остальных CRUD эндпоинтов ---
def create_crud_endpoints(router, model, schema, schema_create, schema_update=None):
    path_name = MODEL_PATHS.get(model);
    if not path_name: raise ValueError(f"No path defined for model {model.__name__}")
    model_name_cap = model.__name__
    if schema_update is None: schema_update = schema_create

    @router.post(f"/{path_name}", response_model=schema, tags=[model_name_cap])
    def post_endpoint(data: schema_create): return create_item(model, data)

    # Исключаем эндпоинты, которые мы определили кастомно
    if model not in [db.GameVariant, db.Game]:
        @router.get(f"/{path_name}/{{item_id}}", response_model=schema, tags=[model_name_cap])
        def get_endpoint(item_id: int): return get_item(model, item_id)

    if model != db.GameVariant:
        @router.get(f"/{path_name}", response_model=List[schema], tags=[model_name_cap])
        def get_all_endpoint(): return get_all_items(model)

    @router.put(f"/{path_name}/{{item_id}}", response_model=schema, tags=[model_name_cap])
    def put_endpoint(item_id: int, data: schema_update): return update_item(model, item_id, data)

    @router.delete(f"/{path_name}/{{item_id}}", tags=[model_name_cap])
    def delete_endpoint(item_id: int): return delete_item(model, item_id)

for model in MODEL_PATHS.keys():
    create_crud_endpoints(router, model, getattr(schemas, f"{(s:=model.__name__)}Schema"), getattr(schemas, f"{s}Create"), getattr(schemas, f"{s}Update"))
