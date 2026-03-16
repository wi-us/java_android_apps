from peewee import *
import os
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()

# --- ПУТЬ К БАЗЕ ДАННЫХ ---
# По умолчанию: ../shop.db (корень проекта)
# Можно переопределить через DATABASE_PATH в .env
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_DB_PATH = os.path.join(PROJECT_ROOT, 'shop.db')
DATABASE_PATH = os.getenv("DATABASE_PATH", DEFAULT_DB_PATH)

db = SqliteDatabase(DATABASE_PATH)

# --- БАЗОВЫЕ МОДЕЛИ ---
class BaseModel(Model):
    class Meta:
        database = db

class Role(BaseModel):
    id = AutoField()
    name = CharField(unique=True)

class User(BaseModel):
    id = AutoField()
    login = CharField(unique=True)
    email = CharField(unique=True)
    password_hash = CharField()
    first_name = CharField(null=True)
    last_name = CharField(null=True)
    phone_number = CharField(null=True)
    role = ForeignKeyField(Role, backref='users', null=True)
    registration_date = DateTimeField(default=datetime.now)

    def set_password(self, password):
        import hashlib
        self.password_hash = hashlib.sha256(password.encode('utf-8')).hexdigest()

    @property
    def role_name(self):
        return self.role.name if self.role else None

class PendingRegistration(BaseModel):
    """Временная запись до подтверждения email. После верификации удаляется, создаётся User."""
    id = AutoField()
    email = CharField(unique=True)
    login = CharField()
    password_hash = CharField()
    code = CharField(max_length=6)
    expires_at = DateTimeField()
    created_at = DateTimeField(default=datetime.now)

class Address(BaseModel):
    id = AutoField()
    user = ForeignKeyField(User, backref='addresses')
    country = CharField(null=True)
    city = CharField()
    street = CharField()
    house_number = CharField()
    apartment_number = CharField(null=True)
    postal_code = CharField()
    # Удобно для мобильного: храним адрес одной строкой (автоввод).
    full_address = TextField(null=True)

# --- МОДЕЛИ ТОВАРОВ ---
class Status(BaseModel):
    id = AutoField()
    name = CharField(unique=True)

class AgeRating(BaseModel):
    id = AutoField()
    min_age = IntegerField(unique=True)

class Language(BaseModel):
    id = AutoField()
    name = CharField(unique=True)
    flag_url = CharField(null=True)

class Genre(BaseModel):
    id = AutoField()
    name = CharField(unique=True)
    description = TextField(null=True)

class Game(BaseModel):
    id = AutoField()
    title = CharField(unique=True)
    slug = CharField(unique=True)
    age_rating = ForeignKeyField(AgeRating, backref='games', null=True)
    min_players = IntegerField(null=True)
    max_players = IntegerField(null=True)
    playtime_avg = IntegerField(null=True) # in minutes

class GameGenre(BaseModel):
    game = ForeignKeyField(Game, backref='genres')
    genre = ForeignKeyField(Genre, backref='games')
    class Meta:
        primary_key = CompositeKey('game', 'genre')

class GameVariant(BaseModel):
    id = AutoField()
    game = ForeignKeyField(Game, backref='variants')
    edition_name = CharField()
    description = TextField(null=True)
    description_html = TextField(null=True)
    rules_html = TextField(null=True)
    components_html = TextField(null=True)
    complexity = CharField(null=True)  # Лёгкая / Средняя / Сложная / Хардкор
    language = ForeignKeyField(Language, backref='variants', null=True)
    is_expansion = BooleanField(default=False)
    weight_grams = IntegerField(null=True)
    dimensions_mm = CharField(null=True) # e.g. "295x295x70"
    price = DecimalField(max_digits=10, decimal_places=2)
    discount_price = DecimalField(max_digits=10, decimal_places=2, null=True)
    status = ForeignKeyField(Status, backref='variants', null=True)
    created_at = DateTimeField(default=datetime.now)

class VariantImage(BaseModel):
    id = AutoField()
    game_variant = ForeignKeyField(GameVariant, backref='images')
    url = CharField()
    is_cover = BooleanField(default=False)
    sort_order = IntegerField(default=0)

# --- МОДЕЛИ ЗАКАЗОВ ---
class Cart(BaseModel):
    id = AutoField()
    user = ForeignKeyField(User, backref='cart', unique=True)
    created_at = DateTimeField(default=datetime.now)

class CartItem(BaseModel):
    id = AutoField()
    cart = ForeignKeyField(Cart, backref='items')
    game_variant = ForeignKeyField(GameVariant)
    quantity = IntegerField(default=1)

class Order(BaseModel):
    id = AutoField()
    user = ForeignKeyField(User, backref='orders')
    address = ForeignKeyField(Address)
    status = ForeignKeyField(Status)
    total_price = DecimalField(max_digits=10, decimal_places=2)
    created_at = DateTimeField(default=datetime.now)
    paid_at = DateTimeField(null=True)
    shipped_at = DateTimeField(null=True)
    delivered_at = DateTimeField(null=True)
    cancelled_at = DateTimeField(null=True)
    comment = TextField(null=True)

class OrderItem(BaseModel):
    id = AutoField()
    order = ForeignKeyField(Order, backref='items')
    game_variant = ForeignKeyField(GameVariant)
    quantity = IntegerField()
    price_per_item = DecimalField(max_digits=10, decimal_places=2)
    discount_per_item = DecimalField(max_digits=10, decimal_places=2, default=0)
    subtotal = DecimalField(max_digits=10, decimal_places=2)

# --- ФУНКЦИЯ ДЛЯ СОЗДАНИЯ ТАБЛИЦ ---
def create_tables():
    with db:
        db.create_tables([
            Role, User, PendingRegistration, Address, Status, AgeRating, Language, Genre, Game, GameGenre,
            GameVariant, VariantImage, Cart, CartItem, Order, OrderItem
        ], safe=True)


def _try_drop_column(table_name: str, column_name: str) -> None:
    """
    Пытается удалить колонку из SQLite (если версия поддерживает ALTER TABLE DROP COLUMN).
    Если не поддерживает/ошибка — тихо игнорируем.
    """
    try:
        db.execute_sql(f'ALTER TABLE "{table_name}" DROP COLUMN "{column_name}"')
    except Exception:
        # SQLite < 3.35 не умеет DROP COLUMN. Это не критично: модель просто перестанет использовать колонку.
        pass

def _try_add_column(table_name: str, column_def_sql: str) -> None:
    """
    Добавляет колонку в SQLite через ALTER TABLE ADD COLUMN (поддерживается давно).
    column_def_sql: например 'warehouse_location_id INTEGER'.
    """
    try:
        db.execute_sql(f'ALTER TABLE "{table_name}" ADD COLUMN {column_def_sql}')
    except Exception:
        pass


def migrate_schema() -> None:
    """
    Мягкая миграция схемы:
    - Удаляем год выпуска из Game
    - Удаляем contents_note из GameVariant
    - Удаляем is_base_game из GameVariant

    Важно: если SQLite не умеет DROP COLUMN, колонки физически останутся,
    но код/модели их больше не используют.
    """
    try:
        # Проверим, что таблицы уже есть
        existing_tables = set(db.get_tables())
        game_table = Game._meta.table_name
        variant_table = GameVariant._meta.table_name
        user_table = User._meta.table_name

        if game_table in existing_tables:
            cols = {c.name for c in db.get_columns(game_table)}
            if "year_published" in cols:
                _try_drop_column(game_table, "year_published")
        if variant_table in existing_tables:
            cols = {c.name for c in db.get_columns(variant_table)}
            if "contents_note" in cols:
                _try_drop_column(variant_table, "contents_note")
            if "is_base_game" in cols:
                _try_drop_column(variant_table, "is_base_game")
            if "description_html" not in cols:
                _try_add_column(variant_table, "description_html TEXT")
            if "rules_html" not in cols:
                _try_add_column(variant_table, "rules_html TEXT")
            if "components_html" not in cols:
                _try_add_column(variant_table, "components_html TEXT")
            if "complexity" not in cols:
                _try_add_column(variant_table, "complexity VARCHAR(50)")

        # User.registration_date (нужен для схемы UserSchema / регистрации)
        if user_table in existing_tables:
            cols = {c.name for c in db.get_columns(user_table)}
            if "registration_date" not in cols:
                _try_add_column(user_table, "registration_date DATETIME")
            # Заполняем для старых строк, чтобы API не падал на ResponseValidationError
            try:
                db.execute_sql(f'UPDATE "{user_table}" SET registration_date = CURRENT_TIMESTAMP WHERE registration_date IS NULL')
            except Exception:
                pass

        # PendingRegistration table
        pending_table = PendingRegistration._meta.table_name
        if pending_table not in existing_tables:
            db.create_tables([PendingRegistration], safe=True)

        # Address.full_address (автоввод адреса)
        address_table = Address._meta.table_name
        if address_table in existing_tables:
            cols = {c.name for c in db.get_columns(address_table)}
            if "full_address" not in cols:
                _try_add_column(address_table, "full_address TEXT")

        # Order: доп. поля под схемы и комментарий
        order_table = Order._meta.table_name
        if order_table in existing_tables:
            cols = {c.name for c in db.get_columns(order_table)}
            if "paid_at" not in cols:
                _try_add_column(order_table, "paid_at DATETIME")
            if "shipped_at" not in cols:
                _try_add_column(order_table, "shipped_at DATETIME")
            if "delivered_at" not in cols:
                _try_add_column(order_table, "delivered_at DATETIME")
            if "cancelled_at" not in cols:
                _try_add_column(order_table, "cancelled_at DATETIME")
            if "comment" not in cols:
                _try_add_column(order_table, "comment TEXT")

        # OrderItem: discount_per_item/subtotal
        order_item_table = OrderItem._meta.table_name
        if order_item_table in existing_tables:
            cols = {c.name for c in db.get_columns(order_item_table)}
            if "discount_per_item" not in cols:
                _try_add_column(order_item_table, "discount_per_item DECIMAL(10,2) DEFAULT 0")
            if "subtotal" not in cols:
                _try_add_column(order_item_table, "subtotal DECIMAL(10,2)")
    except Exception:
        pass
