package com.example.lab66;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles simple in-app SQLite storage for categories and products.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "products.db";
    private static final int DB_VERSION = 2;

    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_PRODUCTS = "products";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT UNIQUE NOT NULL"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_PRODUCTS + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "description TEXT,"
                + "price REAL NOT NULL,"
                + "category_id INTEGER NOT NULL,"
                + "FOREIGN KEY(category_id) REFERENCES " + TABLE_CATEGORIES + "(id)"
                + ")");

        seedData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    private void seedData(SQLiteDatabase db) {
        long foodId = insertCategory(db, "Еда");
        long clothesId = insertCategory(db, "Одежда");
        long toysId = insertCategory(db, "Игрушки");

        // Еда — 10 позиций
        insertProduct(db, "Сэндвич", "Хрустящий бутерброд с овощами", 5.49, foodId);
        insertProduct(db, "Смузи", "Фруктовый напиток без сахара", 3.99, foodId);
        insertProduct(db, "Пицца мини", "Пепперони на тонком тесте", 7.99, foodId);
        insertProduct(db, "Суп день", "Куриный бульон с лапшой", 4.79, foodId);
        insertProduct(db, "Салат греческий", "Овощи, сыр фета, оливки", 6.10, foodId);
        insertProduct(db, "Бургер", "Говядина, сыр, соус", 8.25, foodId);
        insertProduct(db, "Паста сливочная", "Фетучини с грибами", 9.40, foodId);
        insertProduct(db, "Сырники", "Подаются с вареньем", 4.20, foodId);
        insertProduct(db, "Чизкейк", "Нью-Йорк с ягодным соусом", 5.95, foodId);
        insertProduct(db, "Лимонад", "Домашний напиток с мятой", 2.80, foodId);

        // Одежда — 6 позиций
        insertProduct(db, "Куртка", "Лёгкая ветровка", 49.99, clothesId);
        insertProduct(db, "Джинсы", "Классический крой", 39.50, clothesId);
        insertProduct(db, "Кроссовки", "Для повседневной носки", 59.00, clothesId);
        insertProduct(db, "Футболка", "Хлопок, белая", 14.90, clothesId);
        insertProduct(db, "Платье", "Повседневное, свободный крой", 44.30, clothesId);
        insertProduct(db, "Шарф", "Шерсть, клетка", 12.60, clothesId);

        // Игрушки — 8 позиций
        insertProduct(db, "Конструктор", "Набор из 150 деталей", 24.99, toysId);
        insertProduct(db, "Мяч", "Резиновый, яркий", 9.99, toysId);
        insertProduct(db, "Мягкий медведь", "Плюшевый друг 30 см", 14.49, toysId);
        insertProduct(db, "Пазл", "500 деталей, морской пейзаж", 11.20, toysId);
        insertProduct(db, "Настольная игра", "Кооперативная, 2-4 игрока", 29.90, toysId);
        insertProduct(db, "Робот", "Интерактивный, со звуком", 34.80, toysId);
        insertProduct(db, "Кукла", "В комплекте платье и аксессуары", 18.70, toysId);
        insertProduct(db, "Машинка", "Металлический корпус", 7.60, toysId);
    }

    private long insertCategory(SQLiteDatabase db, String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        return db.insert(TABLE_CATEGORIES, null, values);
    }

    private void insertProduct(SQLiteDatabase db, String name, String description, double price, long categoryId) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("description", description);
        values.put("price", price);
        values.put("category_id", categoryId);
        db.insert(TABLE_PRODUCTS, null, values);
    }

    public List<Product> getProductsByCategory(String categoryName) {
        List<Product> products = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT p.name, p.description, p.price " +
                "FROM " + TABLE_PRODUCTS + " p " +
                "JOIN " + TABLE_CATEGORIES + " c ON p.category_id = c.id " +
                "WHERE c.name = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{categoryName})) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String description = cursor.getString(1);
                double price = cursor.getDouble(2);
                products.add(new Product(name, description, price));
            }
        }
        return products;
    }
}

