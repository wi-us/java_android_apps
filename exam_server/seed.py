from database import db, Status, AgeRating, Language, Genre, Game, GameVariant, create_tables

def seed_database():
    """
    Заполняет таблицы-справочники и создает тестовые товары.
    Использует get_or_create, чтобы избежать дубликатов при повторном запуске.
    """
    db.connect()

    print("Creating tables if they don't exist...")
    create_tables()

    print("Seeding lookup tables...")

    # --- Справочники ---
    # --- Statuses ---
    statuses_list = [
        "В наличии",
        "Нет в наличии",
        "Предзаказ",
        "Скоро в продаже",
        "Снят с продажи",
        "Ожидается поставка",
    ]
    for name in statuses_list: Status.get_or_create(name=name)
    print(f"  - Seeded {len(statuses_list)} statuses.")

    age_ratings_list = [0, 3, 6, 8, 10, 12, 14, 16, 18]
    for age in age_ratings_list: AgeRating.get_or_create(min_age=age)
    print(f"  - Seeded {len(age_ratings_list)} age ratings.")

    # --- Languages ---
    languages_list = [
        "Русский",
        "Английский",
        "Немецкий",
        "Французский",
        "Испанский",
        "Итальянский",
        "Польский",
        "Независимый от языка",
    ]
    for name in languages_list: Language.get_or_create(name=name)
    print(f"  - Seeded {len(languages_list)} languages.")

    # --- Genres ---
    genres_list = [
        "Стратегия",
        "Тактика",
        "Семейная",
        "Для вечеринок",
        "Кооперативная",
        "Экономическая",
        "Детективная",
        "Приключения",
        "Карточная",
        "Дуэльная",
        "Абстрактная",
        "Варгейм",
        "Ролевая",
        "Филлер",
        "Хардкор",
        "Евро",
        "Америтреш",
    ]
    for name in genres_list: Genre.get_or_create(name=name)
    print(f"  - Seeded {len(genres_list)} genres.")

    # --- Тестовые товары (создаются только если таблица пуста) ---
    if not Game.select().exists():
        print("Seeding test products...")

        # Получаем объекты из справочников для связей
        age_10 = AgeRating.get(AgeRating.min_age == 10)
        age_6 = AgeRating.get(AgeRating.min_age == 6)
        lang_ru = Language.get(Language.name == "Русский")
        status_in_stock = Status.get(Status.name == "В наличии")
        status_preorder = Status.get(Status.name == "Предзаказ")

        # --- Игра 1: Колонизаторы ---
        catan, _ = Game.get_or_create(
            title="Колонизаторы",
            slug="catan",
            defaults={
                'min_players': 3,
                'max_players': 4,
                'age_rating': age_10
            }
        )
        # Вариант 1.1: Базовая игра
        GameVariant.create(
            game=catan,
            edition_name="Базовая игра (Рус.)",
            language=lang_ru,
            description="Классическое издание знаменитой игры о колонизации острова.",
            price=2500,
            status=status_in_stock
        )
        # Вариант 1.2: Подарочное издание
        GameVariant.create(
            game=catan,
            edition_name="Подарочное издание",
            language=lang_ru,
            description="Эксклюзивное издание с улучшенными компонентами.",
            price=4990,
            # По требованиям проекта: discount_price по умолчанию равна price (в будущем меняете вручную)
            discount_price=4990,
            status=status_preorder
        )

        # --- Игра 2: Каркассон ---
        carcassonne, _ = Game.get_or_create(
            title="Каркассон",
            slug="carcassonne",
            defaults={
                'min_players': 2,
                'max_players': 5,
                'age_rating': age_6
            }
        )
        # Вариант 2.1: Средневековье
        GameVariant.create(
            game=carcassonne,
            edition_name="Средневековье",
            language=lang_ru,
            description="Выкладывайте квадраты земель, стройте города и монастыри.",
            price=1500,
            status=status_in_stock
        )
        print("  - Seeded 2 games with 3 variants.")
    else:
        print("Products table is not empty, skipping product seeding.")


    print("Seeding completed successfully!")
    db.close()

if __name__ == "__main__":
    seed_database()
