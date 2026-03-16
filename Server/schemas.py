from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class Token(BaseModel):
    access_token: str
    token_type: str

# Базовые схемы
class RoleBase(BaseModel):
    id: int
    name: str
    class Config: from_attributes = True

class LanguageBase(BaseModel):
    id: int
    name: str
    flag_url: Optional[str] = None
    class Config: from_attributes = True


class GenreBase(BaseModel):
    id: int
    name: str
    class Config: from_attributes = True

class AgeRatingBase(BaseModel):
    id: int
    min_age: int
    class Config: from_attributes = True

class StatusBase(BaseModel):
    id: int
    name: str
    class Config: from_attributes = True

class UserBase(BaseModel):
    id: int
    login: str
    email: str
    class Config: from_attributes = True

class AddressBase(BaseModel):
    id: int
    city: str
    street: str
    class Config: from_attributes = True

class GameBase(BaseModel):
    id: int
    title: str
    slug: str
    min_players: Optional[int] = None
    max_players: Optional[int] = None
    playtime_avg: Optional[int] = None
    age_rating: Optional[AgeRatingBase] = None
    class Config: from_attributes = True

class GameVariantBase(BaseModel):
    id: int
    price: float
    discount_price: Optional[float] = None
    class Config: from_attributes = True

class OrderBase(BaseModel):
    id: int
    total_price: float
    created_at: datetime
    class Config: from_attributes = True

# Role
class RoleSchema(RoleBase): pass
class RoleCreate(BaseModel): name: str
class RoleUpdate(BaseModel): name: Optional[str] = None

# Language
class LanguageSchema(LanguageBase): pass
class LanguageCreate(BaseModel):
    name: str
    flag_url: Optional[str] = None
class LanguageUpdate(BaseModel):
    name: Optional[str] = None
    flag_url: Optional[str] = None

# Genre
class GenreSchema(GenreBase): pass
class GenreCreate(BaseModel): name: str
class GenreUpdate(BaseModel): name: Optional[str] = None

# GameGenre
class GameGenreSchema(BaseModel):
    game_id: int
    genre_id: int
    class Config: from_attributes = True
class GameGenreCreate(BaseModel):
    game_id: int
    genre_id: int
class GameGenreUpdate(BaseModel):
    game_id: Optional[int] = None
    genre_id: Optional[int] = None

# AgeRating
class AgeRatingSchema(AgeRatingBase): pass
class AgeRatingCreate(BaseModel): min_age: int
class AgeRatingUpdate(BaseModel): min_age: Optional[int] = None

# Status
class StatusSchema(StatusBase): pass
class StatusCreate(BaseModel): name: str
class StatusUpdate(BaseModel): name: Optional[str] = None

# User
class UserSchema(UserBase):
    phone_number: Optional[str] = None
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    registration_date: datetime
    role: Optional[RoleBase] = None

class UserCreate(BaseModel):
    login: str
    password: str
    email: str

class UserUpdate(BaseModel):
    login: Optional[str] = None
    password: Optional[str] = None
    email: Optional[str] = None
    phone_number: Optional[str] = None
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    role_id: Optional[int] = None

# Address
class AddressSchema(AddressBase):
    user: UserBase
    country: Optional[str] = None
    house_number: Optional[str] = None
    apartment_number: Optional[str] = None
    postal_code: Optional[str] = None
    full_address: Optional[str] = None
class AddressCreate(BaseModel):
    user_id: int
    country: Optional[str] = None
    city: Optional[str] = None
    street: Optional[str] = None
    house_number: Optional[str] = None
    apartment_number: Optional[str] = None
    postal_code: Optional[str] = None
    full_address: Optional[str] = None
class AddressUpdate(BaseModel):
    user_id: Optional[int] = None
    country: Optional[str] = None
    city: Optional[str] = None
    street: Optional[str] = None
    house_number: Optional[str] = None
    apartment_number: Optional[str] = None
    postal_code: Optional[str] = None
    full_address: Optional[str] = None

# Game
class GameSchema(GameBase):
    age_rating: Optional[AgeRatingBase] = None
    min_players: int
    max_players: int
    playtime_avg: Optional[int] = None
    genres: List[str] = []
    variants: List['GameVariantSchema'] = [] # Добавлено для вложенности

class GameCreate(BaseModel):
    title: str
    slug: str
    min_players: int
    max_players: int
    playtime_avg: Optional[int] = None
    age_rating_id: Optional[int] = None
class GameUpdate(BaseModel):
    title: Optional[str] = None
    slug: Optional[str] = None
    min_players: Optional[int] = None
    max_players: Optional[int] = None
    playtime_avg: Optional[int] = None
    age_rating_id: Optional[int] = None

# GameVariant
class GameVariantSchema(GameVariantBase):
    game: GameBase
    language: Optional[LanguageBase] = None
    status: Optional[StatusBase] = None
    edition_name: Optional[str] = None
    description: Optional[str] = None
    description_html: Optional[str] = None
    rules_html: Optional[str] = None
    components_html: Optional[str] = None
    complexity: Optional[str] = None
    image_link: Optional[str] = None 
    images: List['VariantImageSchema'] = [] # Список всех изображений для слайдера
    is_expansion: bool
    weight_grams: Optional[int] = None
    dimensions_mm: Optional[str] = None
    # Распарсенные размеры коробки
    box_width_mm: Optional[int] = None
    box_height_mm: Optional[int] = None
    box_depth_mm: Optional[int] = None
    genres: List[str] = []  # Жанры игры
    created_at: datetime
    class Config: from_attributes = True
class GameVariantCreate(BaseModel):
    game_id: int
    edition_name: Optional[str] = None
    language_id: Optional[int] = None
    description: Optional[str] = None
    is_expansion: bool = False
    weight_grams: Optional[int] = None
    dimensions_mm: Optional[str] = None
    price: float
    discount_price: Optional[float] = None
    status_id: Optional[int] = None
class GameVariantUpdate(BaseModel):
    game_id: Optional[int] = None
    edition_name: Optional[str] = None
    language_id: Optional[int] = None
    description: Optional[str] = None
    is_expansion: Optional[bool] = None
    weight_grams: Optional[int] = None
    dimensions_mm: Optional[str] = None
    price: Optional[float] = None
    discount_price: Optional[float] = None
    status_id: Optional[int] = None

# VariantImage
class VariantImageSchema(BaseModel):
    id: int
    game_variant_id: int
    url: str
    is_cover: bool
    sort_order: int
    class Config: from_attributes = True
class VariantImageCreate(BaseModel):
    game_variant_id: int
    url: str
    is_cover: bool = False
    sort_order: int = 0
class VariantImageUpdate(BaseModel):
    game_variant_id: Optional[int] = None
    url: Optional[str] = None
    is_cover: Optional[bool] = None
    sort_order: Optional[int] = None

# Cart
class CartSchema(BaseModel):
    id: int
    user: UserBase
    created_at: datetime
    class Config: from_attributes = True
class CartCreate(BaseModel): user_id: int
class CartUpdate(BaseModel): user_id: Optional[int] = None

# CartItem
class CartItemSchema(BaseModel):
    id: int
    cart_id: int
    game_variant_id: int
    quantity: int
    class Config: from_attributes = True
class CartItemCreate(BaseModel):
    cart_id: int
    game_variant_id: int
    quantity: int
class CartItemUpdate(BaseModel):
    quantity: Optional[int] = None

# Order
class OrderSchema(OrderBase):
    user: UserBase
    address: AddressBase
    status: StatusBase
    paid_at: Optional[datetime] = None
    shipped_at: Optional[datetime] = None
    delivered_at: Optional[datetime] = None
    cancelled_at: Optional[datetime] = None
    comment: Optional[str] = None
class OrderCreate(BaseModel):
    user_id: int
    address_id: int
    status_id: int
    total_price: float
    comment: Optional[str] = None
class OrderUpdate(BaseModel):
    status_id: Optional[int] = None
    total_price: Optional[float] = None
    paid_at: Optional[datetime] = None
    shipped_at: Optional[datetime] = None
    delivered_at: Optional[datetime] = None
    cancelled_at: Optional[datetime] = None
    comment: Optional[str] = None

# OrderItem
class OrderItemSchema(BaseModel):
    id: int
    order_id: int
    game_variant_id: int
    quantity: int
    price_per_item: float
    discount_per_item: float
    subtotal: float
    class Config: from_attributes = True
class OrderItemCreate(BaseModel):
    order_id: int
    game_variant_id: int
    quantity: int
    price_per_item: float
    discount_per_item: float = 0.0
    subtotal: float
class OrderItemUpdate(BaseModel):
    quantity: Optional[int] = None
    price_per_item: Optional[float] = None
    discount_per_item: Optional[float] = None
    subtotal: Optional[float] = None
        
        
# --- Checkout (mobile) ---
class CheckoutRequest(BaseModel):
    address: str
    apartment: Optional[str] = None
    name: str
    phone: Optional[str] = None
    comment: Optional[str] = None

class CheckoutResponse(BaseModel):
    order_id: int
    total: float

class AddressSuggestion(BaseModel):
    id: Optional[str] = None
    text: str
    lat: Optional[float] = None
    lon: Optional[float] = None

class AddressDetails(BaseModel):
    id: Optional[str] = None
    text: str
    lat: Optional[float] = None
    lon: Optional[float] = None

        