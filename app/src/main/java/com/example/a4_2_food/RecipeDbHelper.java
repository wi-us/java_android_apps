package com.example.a4_2_food;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

// База: книги и рецепты. При первом запуске создаёт таблицы и заполняет данными.
public class RecipeDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "recipes.db";
    private static final int DB_VERSION = 13;

    private static final String TABLE_BOOKS = "cookbooks";
    private static final String TABLE_RECIPES = "recipes";

    public RecipeDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_BOOKS + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name_ru TEXT NOT NULL," +
                        "name_en TEXT NOT NULL," +
                        "name_es TEXT NOT NULL);"
        );

        db.execSQL(
                "CREATE TABLE " + TABLE_RECIPES + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "cookbook_id INTEGER NOT NULL," +
                        "name_ru TEXT NOT NULL," +
                        "name_en TEXT NOT NULL," +
                        "name_es TEXT NOT NULL," +
                        "ingredients_ru TEXT," +
                        "ingredients_en TEXT," +
                        "ingredients_es TEXT," +
                        "steps_ru TEXT," +
                        "steps_en TEXT," +
                        "steps_es TEXT," +
                        "image_dish TEXT," +
                        "ingredient_images TEXT," +
                        "meal_type TEXT," +
                        "FOREIGN KEY (cookbook_id) REFERENCES " + TABLE_BOOKS + "(id));"
        );

        fillInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKS);
        onCreate(db);
    }

    private void fillInitialData(SQLiteDatabase db) {
        insertBook(db, "Кухня World of Warcraft", "World of Warcraft Cookbook", "Libro de cocina de World of Warcraft");
        insertBook(db, "Славные рецепты", "Glorious Recipes", "Recetas gloriosas");
        insertBook(db, "Кухня Minecraft", "Minecraft Cookbook", "Libro de cocina de Minecraft");
        insertBook(db, "Оригинальные рецепты", "Original Recipes", "Recetas originales");

        insertRecipe(db, 1,
                "Пирог из тыквы", "Pumpkin Pie", "Tarta de calabaza",
                "1/2 порции слоёного теста||2 яйца||2 ч.л. праздничных специй||2 ст.л. миндальной муки||1/2 стакана мёда||1 банка тыквы 425 г (Элвин или Тирисал)||1 банка сгущённого молока 340 мл||1 порция взбитых сливок (по желанию)",
                "1/2 batch Flaky Pie Dough||2 eggs||2 teaspoons Holiday Spices||2 tablespoons finely ground almond meal||1/2 cup honey||One 15-ounce can pumpkin (Elwynn or Tirisfal)||One 12-ounce can evaporated milk||1 batch Whipped Cream (optional)",
                "1/2 masa de tarta||2 huevos||2 cucharaditas especias||2 cucharadas almendra molida||1/2 taza miel||1 lata calabaza 425 g||1 lata leche evaporada 340 ml||nata montada (opcional)",
                "Разогреть духовку до 190 °C. Раскатать тесто, выложить в форму, обрезать края.||Взбить яйца. Смазать корж яйцом при желании. Проколоть вилкой, выпекать корж 15 мин.||Смешать специи, миндальную муку, мёд с яйцами. Добавить тыкву и молоко. Вылить в корж, выпекать 40 мин.||Подавать со взбитыми сливками или мороженым.",
                "Preheat oven to 375°F. Roll dough, lay in pan, trim. Fork bottom, pre-bake 15 min.||Beat eggs. Brush crust with egg if decorative.||Combine spices, almond meal, honey with eggs. Add pumpkin and evaporated milk. Pour into crust, bake 40 min.||Top with whipped cream or serve with ice cream.",
                "Precalentar horno 190 °C. Estirar masa, prehornear 15 min.||Batir huevos. Mezclar especias, almendra, miel, calabaza y leche. Hornear 40 min.||Servir con nata o helado.",
                "", "", "breakfast");
        insertRecipe(db, 1,
                "Завитки заварного теста в сахарной пудре", "Sugar-Dusted Choux Twists", "Trenzas de profiteroles con azúcar",
                "1 стакан воды||100 г сливочного масла||2 ст.л. сахара||1 стакан муки||4 яйца||1 порция взбитых сливок||1 порция глазури||сахарная пудра (по желанию)",
                "1 cup water||1 stick salted butter||2 tablespoons sugar||1 cup flour||4 eggs||1 batch Whipped Cream||1 batch Drizzled Icing and Glaze||Confectioners' sugar (optional)",
                "1 taza agua||100 g mantequilla||2 cucharadas azúcar||1 taza harina||4 huevos||nata montada||glaseado||azúcar glas (opcional)",
                "Разогреть духовку до 220 °C. Застелить 2 противня бумагой.||В кастрюле довести воду, масло и сахар до кипения. Всыпать муку, мешать до отставания от стенок. Снять с огня.||Переложить в миску, остудить 5 мин. Миксером вбивать яйца по одному.||Переложить в кондитерский мешок с зубчатой насадкой. Отсадить зигзаги 7–8 см. Выпекать по одному противню 15–20 мин.||Проколоть 2 дырки снизу, наполнить кремом. Окунуть в глазурь, присыпать сахарной пудрой. Подавать сразу.",
                "Preheat oven to 425°F. Line two baking sheets.||Combine water, butter, sugar in pot, simmer. Add flour, stir until dough pulls away from pan. Remove from heat.||Transfer to bowl, cool 5 min. Add eggs one at a time with mixer.||Pipe with star tip in zigzag 3 in long. Bake one pan 15–20 min.||Poke 2 holes, fill with whipped cream. Dip in glaze, dust with confectioners' sugar. Serve immediately.",
                "Precalentar 220 °C. Mezclar agua, mantequilla, azúcar y harina. Añadir huevos.||En manga, formar trenzas. Hornear 15–20 min. Rellenar con nata, bañar en glaseado.",
                "", "", "breakfast");
        insertRecipe(db, 1,
                "Кактусовое яблоко с сюрпризом", "Cactus Apple Surprise", "Sorpresa de manzana de cactus",
                "60 мл текилы||30 мл трипл сека||15 мл яблочного бренди||30–60 мл сиропа опунции||60 мл лимонада||мята для украшения||ОБОДОК: щепотка чили, 2 ст.л. крупного сахара",
                "2 ounces tequila||1 ounce triple sec||1/2 ounce apple brandy||1 to 2 ounces prickly pear syrup||2 ounces lemonade||Mint for garnish||RIM: pinch chili powder, 2 tbsp coarse sugar",
                "60 ml tequila||30 ml triple sec||15 ml brandy de manzana||30–60 ml jarabe de tuna||60 ml limonada||menta||BORDE: chile, 2 cucharadas azúcar",
                "Смешать чили и сахар для ободка. Провести долькой лайма по краям бокалов, обмакнуть в смесь. Наполнить бокалы льдом.||Смешать в шейкере все ингредиенты кроме лимонада. Встряхнуть. Разлить по бокалам, добавить лимонад по вкусу. Украсить мятой. Примечание: если нет сиропа опунции — заменить малиновым или гранатовым.",
                "Combine chili powder and sugar for rim. Run lime around rims, dip in sugar. Fill glasses with ice.||Shake all ingredients except lemonade in shaker. Pour over ice, add lemonade to taste. Garnish with mint. Cook's note: substitute raspberry or pomegranate syrup if no prickly pear.",
                "Mezclar chile y azúcar para el borde. Mojar bordes con lima.||Agitar en coctelera todo menos limonada. Servir con hielo y limonada.",
                "", "", "dinner");
        insertRecipe(db, 1,
                "Курица с арахисом на шпажках", "Skewered Peanut Chicken", "Pollo con maní en brochetas",
                "4 больших куриных грудки||МАРИНАД: 1/2 стакана соевого соуса, 1 ч.л. тёртого имбиря, щепотка соли||СОУС: 1/4 стакана арахисовой пасты, 1 банка кокосового молока 380 мл, 1/4 стакана коричневого сахара, 1 ст.л. соевого соуса, 1,5 ст.л. пасты красного карри||помидорки черри и снежный горошек для украшения (по желанию)",
                "4 large chicken breasts||MARINADE: 1/2 cup soy sauce, 1 tsp freshly grated ginger, pinch salt||SAUCE: 1/4 cup creamy peanut butter, 13-oz can coconut milk, 1/4 cup brown sugar, 1 tbsp soy sauce, 1 1/2 tbsp red curry paste||Cherry tomatoes and snap peas for garnish (optional)",
                "4 pechugas||MARINADA: 1/2 taza salsa de soja, 1 cucharadita jengibre, sal||SALSA: 1/4 taza mantequilla de maní, 380 ml leche de coco, 1/4 taza azúcar moreno, 1 cucharada soja, 1,5 cucharadas pasta curry||tomates y guisantes para decorar (opcional)",
                "Смешать маринад с курицей в пакете или миске. Мариновать в холодильнике не менее 4 часов. Замочить 4 деревянные шпажки в воде.||Приготовить соус: смешать ингредиенты в сотейнике на среднем огне, помешивать до однородности. Снять с огня, остудить.||Нанизать курицу на шпажки. Жарить на гриле 10 мин, перевернуть в середине. Подавать с соусом и рисом. Украшать овощами после гриля. Примечание: овощи не жарить — переварятся.",
                "Combine marinade and chicken, refrigerate at least 4 hours. Soak 4 wooden skewers in water.||Make sauce: combine sauce ingredients in saucepan over medium heat, stir until creamy. Remove, cool.||Thread chicken onto skewers. Grill over medium-high 10 min, flipping halfway. Plate, drizzle sauce. Cook's note: garnish with tomatoes and snap peas after grilling.",
                "Mezclar marinada y pollo, refrigerar 4 h. Remojar brochetas.||Hacer salsa en cazo a fuego medio.||Enbrochetar pollo, asar 10 min. Servir con salsa y arroz.",
                "", "", "dinner");

        insertRecipe(db, 2,
                "Славные чашушули", "Glorious Chashushuli", "Chashushuli glorioso",
                "Говядина 3000 г||Сладкий перец 6 шт.||Репчатый лук 5 шт.||Помидор 6 шт.||Петрушка 100 г||Кинза 100 г||Чеснок 10 зубчиков||Аджика 1 ст. л.||Томатная паста 3 ст. л.||Хмели-сунели 2 ст. л.||Сливочное масло 100 г||Лавровый лист 2 шт.||Душистый перец горошком 4 шт.||Соль 1 ст. л.||Сахар 1 ст. л.",
                "3 kg beef||6 bell peppers||5 onions||6 tomatoes||100 g parsley||100 g cilantro||10 cloves garlic||1 tbsp adjika||3 tbsp tomato paste||2 tbsp khmeli-suneli||100 g butter||2 bay leaves||4 allspice berries||1 tbsp salt||1 tbsp sugar",
                "3 kg ternera||6 pimientos||5 cebollas||6 tomates||100 g perejil||100 g cilantro||10 dientes ajo||1 cucharada adjika||3 cucharadas pasta de tomate||2 cucharadas khmeli-suneli||100 g mantequilla||2 hojas laurel||4 bayas pimienta||1 cucharada sal||1 cucharada azúcar",
                "Мясо отделить от кости, нарезать крупными кубиками.||Кости варить для бульона с лавром и душистым перцем.||Лук, перец, зелень и чеснок нарезать. С помидоров снять шкуру, нарезать.||Смешать томатную пасту, аджику и хмели-сунели с водой.||Мясо обжарить до золотистой корочки.||В кастрюле смешать лук и мясо, обжарить лук. Добавить масло, соль, сахар, смесь из томатов, перец и томаты. Залить бульоном, томить 2–2,5 ч.||Выключить огонь, добавить чеснок и зелень. Подавать с гарниром.",
                "Cut meat off bone, dice.||Simmer bones for stock with bay and allspice.||Chop onion, pepper, herbs, garlic. Skin and chop tomatoes.||Mix tomato paste, adjika and khmeli-suneli with water.||Fry meat until golden.||In pot combine onion and meat, fry onion. Add butter, salt, sugar, tomato mix, pepper and tomatoes. Cover with stock, simmer 2–2.5 h.||Turn off heat, add garlic and herbs. Serve with side.",
                "Cortar carne, cubos.||Hervir huesos para caldo.||Picar cebolla, pimiento, hierbas, ajo. Pelar y cortar tomates.||Mezclar pasta de tomate, adjika y khmeli-suneli.||Freír carne.||En cazuela, cebolla y carne, añadir mantequilla, sal, azúcar, tomates. Cubrir con caldo, cocer 2–2,5 h.||Añadir ajo y hierbas. Servir con guarnición.",
                "", "", "lunch");
        insertRecipe(db, 2,
                "Славный борщ", "Glorious Borscht", "Borscht glorioso",
                "Говядина 1600 г||Говяжьи кости 3000 г||Репчатый лук 2 шт.||Морковь 3 шт.||Свекла 2 шт.||Капуста 1 шт.||Корень сельдерея 1 шт.||Томатная паста 3 ст. л.||Укроп 1 пучок||Петрушка 1 пучок||Чеснок 40 г||Душистый перец горошком 10 шт.||Гвоздика 7 шт.||Лавровый лист 3 шт.||Сахар 2 ст. л.||Соль 2 ст. л.||Лимонный сок 40 г",
                "1.6 kg beef||3 kg beef bones||2 onions||3 carrots||2 beets||1 cabbage||1 celery root||3 tbsp tomato paste||1 bunch dill||1 bunch parsley||40 g garlic||10 allspice berries||7 cloves||3 bay leaves||2 tbsp sugar||2 tbsp salt||40 g lemon juice",
                "1,6 kg ternera||3 kg huesos||2 cebollas||3 zanahorias||2 remolachas||1 repollo||1 apio||3 cucharadas pasta tomate||1 manojo eneldo||1 manojo perejil||40 g ajo||10 bayas||7 clavos||3 hojas laurel||2 cucharadas azúcar||2 cucharadas sal||40 g zumo limón",
                "Кости запечь на противне до золотистости.||Мясо нарезать кубиками, обжарить со всех сторон.||Капусту мелко нашинковать, слегка обжарить.||Кости и мясо в скороварку со специями для бульона. Залить водой, варить 1,5 ч (или 3 ч в кастрюле).||Лук, морковь, сельдерей и свеклу нарезать или пропустить через комбайн.||Обжарить лук, морковь, сельдерей. Добавить томатную пасту, лимонный сок и свеклу.||Бульон процедить. Мясо нарезать.||В бульон добавить зажарку, капусту, мясо, зелень, соль, сахар, чеснок. Варить 10–15 мин. Подавать со сметаной.",
                "Roast bones on tray until golden.||Dice meat, fry on all sides.||Shred cabbage, fry briefly.||Put bones and meat in pressure cooker with spices. Cover with water, cook 1.5 h (or 3 h in pot).||Chop onion, carrot, celery, beet.||Fry onion, carrot, celery. Add tomato paste, lemon juice, beet.||Strain stock. Dice meat.||Add fry, cabbage, meat, herbs, salt, sugar, garlic to stock. Cook 10–15 min. Serve with sour cream.",
                "Asar huesos.||Cortar y freír carne.||Cortar repollo, freír.||Cocinar huesos y carne con especias 1,5 h.||Picar cebolla, zanahoria, apio, remolacha.||Sofreír, añadir pasta tomate, limón, remolacha.||Colar caldo.||Añadir sofrito, repollo, carne, hierbas. Cocer 10–15 min. Servir con nata.",
                "", "", "lunch");
        insertRecipe(db, 2,
                "Славный соус для шавермы", "Glorious Shawarma Sauce", "Salsa gloriosa para shawarma",
                "Простокваша 300 г||Желток куриного яйца 60 г||Чеснок 25 г||Горчица 25 г||Соль 8 г||Хмели-сунели 5 г||Подсолнечное масло 450 г||Лимонный сок 15 г",
                "300 g buttermilk/yogurt||60 g egg yolk||25 g garlic||25 g mustard||8 g salt||5 g khmeli-suneli||450 g sunflower oil||15 g lemon juice",
                "300 g suero||60 g yema||25 g ajo||25 g mostaza||8 g sal||5 g khmeli-suneli||450 g aceite||15 g zumo limón",
                "В блендер поместите желтки, чеснок, хмели-сунели, соль и горчицу.||Включите блендер, перебейте чеснок и постепенно вливайте масло до загущения.||В конце добавьте сок лимона и перемешайте.||Влейте простоквашу, перемешайте и уберите в холодильник минимум на полчаса.",
                "Put yolks, garlic, khmeli-suneli, salt and mustard in blender.||Blend, then gradually add oil until thick.||Add lemon juice and mix.||Stir in buttermilk, refrigerate at least 30 min.",
                "Poner yemas, ajo, khmeli-suneli, sal y mostaza en batidora.||Batir y añadir aceite poco a poco.||Añadir zumo de limón.||Añadir suero, refrigerar 30 min.",
                "", "", "lunch");
        insertRecipe(db, 2,
                "Славная картошка с грибным соусом", "Glorious Potato with Mushroom Sauce", "Patatas gloriosas con salsa de setas",
                "Картошка 2 кг||Шампиньоны 600 г||Грибной порошок 2 ст. л.||Репчатый лук 500 г||Сливочное масло 180 г||Чеснок 10 зубчиков||Тимьян 1 пучок||Сметана 600 г||Подсолнечное масло 3 ст. л.",
                "2 kg potatoes||600 g champignons||2 tbsp mushroom powder||500 g onions||180 g butter||10 cloves garlic||1 bunch thyme||600 g sour cream||3 tbsp sunflower oil",
                "2 kg patatas||600 g champiñones||2 cucharadas polvo setas||500 g cebollas||180 g mantequilla||10 dientes ajo||1 ramo tomillo||600 g nata agria||3 cucharadas aceite",
                "Картофель отварить до полуготовности (10–15 мин).||Нашинковать лук, грибы и чеснок. Нарезать картофель кубиками.||Обжарить шампиньоны с тимьяном, затем достать тимьян.||Добавить грибной порошок, прогреть. Добавить сметану, соль и перец. Готовить 2–3 мин.||Обжаривать картофель на сливочном масле 30–35 мин, переворачивая. За 10 мин до конца добавить лук, в конце — чеснок.||Подавать картофель с грибным соусом.",
                "Parboil potatoes 10–15 min.||Slice onion, mushrooms, garlic. Dice potatoes.||Fry champignons with thyme, remove thyme.||Add mushroom powder, heat. Add sour cream, salt, pepper. Cook 2–3 min.||Fry potatoes in butter 30–35 min, turning. Add onion 10 min before end, garlic at the end.||Serve potato with mushroom sauce.",
                "Hervir patatas 10–15 min.||Cortar cebolla, setas, ajo. Cortar patatas.||Freír champiñones con tomillo.||Añadir polvo de setas, nata, sal, pimienta.||Freír patatas en mantequilla 30–35 min. Añadir cebolla y ajo al final.||Servir con salsa de setas.",
                "", "", "lunch");
        insertRecipe(db, 2,
                "Славный шашлык", "Glorious Shashlik", "Shashlik glorioso",
                "Свиная шея 3000 г||Аджика 2 ст. л.||Соевый соус 200 г||Репчатый лук 200 г||Кефир 200 г||Чеснок 10 зубчиков||Грузинский лаваш (по желанию)||Зелёный лук (по желанию)",
                "3 kg pork neck||2 tbsp adjika||200 g soy sauce||200 g onions||200 g kefir||10 cloves garlic||Lavash (optional)||Green onion (optional)",
                "3 kg cuello de cerdo||2 cucharadas adjika||200 g salsa de soja||200 g cebollas||200 g kéfir||10 dientes ajo||Lavash (opcional)||Cebolleta (opcional)",
                "Мясо нарезать крупными кубиками.||Лук пропустить через мясорубку в сито, получить сок.||К мясу добавить луковый сок, аджику, тёртый чеснок, соевый соус и кефир. Перемешать, мариновать минимум 2 ч.||Разжечь угли, дождаться серого пепла.||Мясо насадить на шампуры, задать корочку с двух сторон, довести до готовности. Подавать с лавашом и зелёным луком.",
                "Dice meat.||Press onion through meat grinder into sieve for juice.||Add onion juice, adjika, garlic, soy sauce and kefir to meat. Mix, marinate at least 2 h.||Light coals, wait until grey and ashy.||Thread meat on skewers, sear both sides, cook through. Serve with lavash and green onion.",
                "Cortar carne en cubos.||Pasar cebolla por picadora, obtener zumo.||Añadir a la carne zumo, adjika, ajo, salsa de soja y kéfir. Marinar 2 h mínimo.||Encender carbón.||Enbrochetar carne, sellar y cocer. Servir con lavash y cebolleta.",
                "", "", "dinner");
        insertRecipe(db, 2,
                "Славные фрикадельки", "Glorious Meatballs", "Albóndigas gloriosas",
                "Мясо куриного бедра 1000 г||Чеснок 10 г||Пармезан 150 г||Яйца 2 шт.||Панировочные сухари 150 г||Петрушка 15 г||Соль 5 г||Черный перец 3 г||Стебель сельдерея 150 г||Морковь 150 г||Репчатый лук 200 г||Помидор 800 г||Базилик 5 г||Спагетти 450 г||Сливочное масло 30 г",
                "1 kg chicken thigh meat||10 g garlic||150 g Parmesan||2 eggs||150 g breadcrumbs||15 g parsley||5 g salt||3 g black pepper||150 g celery||150 g carrots||200 g onions||800 g tomatoes||5 g basil||450 g spaghetti||30 g butter",
                "1 kg muslo de pollo||10 g ajo||150 g parmesano||2 huevos||150 g pan rallado||15 g perejil||5 g sal||3 g pimienta||150 g apio||150 g zanahorias||200 g cebollas||800 g tomates||5 g albahaca||450 g espaguetis||30 g mantequilla",
                "Пропустить мясо через мясорубку. К фаршу добавить чеснок, пармезан, яйца, петрушку, сухари, соль и перец. Оставить на 5 мин.||Морковь, сельдерей и лук нарезать. С помидоров снять кожуру, нарезать. Покрошить базилик.||Сформировать фрикадельки.||Обжарить фрикадельки до золотистой корочки с двух сторон.||Обжарить лук, морковь и сельдерей. Добавить томаты и воду, соль, перец, базилик.||Добавить фрикадельки в соус, при необходимости воду. Тушить 10 мин.||Спагетти отварить аль денте.||Растопить масло, добавить пасту, соус и фрикадельки. Готовить 1–2 мин. Подавать тёплым.",
                "Mince meat. Add garlic, Parmesan, eggs, parsley, breadcrumbs, salt, pepper. Rest 5 min.||Dice carrot, celery, onion. Skin and chop tomatoes. Chop basil.||Shape meatballs.||Fry meatballs until golden on both sides.||Fry onion, carrot, celery. Add tomatoes, water, salt, pepper, basil.||Add meatballs to sauce, add water if needed. Simmer 10 min.||Cook spaghetti al dente.||Melt butter, add pasta, sauce and meatballs. Cook 1–2 min. Serve warm.",
                "Picar carne. Añadir ajo, parmesano, huevos, perejil, pan, sal, pimienta.||Picar zanahoria, apio, cebolla. Pelar y cortar tomates.||Formar albóndigas.||Freír albóndigas.||Sofreír cebolla, zanahoria, apio. Añadir tomates, agua, albahaca.||Añadir albóndigas, cocer 10 min.||Cocer espaguetis.||Añadir mantequilla, pasta y salsa. Servir caliente.",
                "", "", "dinner");
        insertRecipe(db, 2,
                "Славный «Цезарь»", "Glorious Caesar", "César glorioso",
                "Салат фриллис 150 г||Перепелиные яйца 9 шт.||Помидоры черри 100 г||Пармезан 50 г||Белый хлеб 150 г||Филе красной рыбы 300 г||Соль для засолки 500 г||Желтки 3 шт.||Растительное масло 200 г||Оливковое масло 100 г||Вустерширский соус 25 г||Анчоусы 30 г||Каперсы 20 г||Петрушка 15 г||Лимонный сок 7 г||Горчица 10 г||Чеснок 6 г||Соль и перец по вкусу",
                "150 g frisée lettuce||9 quail eggs||100 g cherry tomatoes||50 g Parmesan||150 g white bread||300 g red fish fillet||500 g salt for curing||3 egg yolks||200 g vegetable oil||100 g olive oil||25 g Worcestershire sauce||30 g anchovies||20 g capers||15 g parsley||7 g lemon juice||10 g mustard||6 g garlic||Salt and pepper",
                "150 g lechuga||9 huevos de codorniz||100 g tomates cherry||50 g parmesano||150 g pan blanco||300 g filete de pescado rojo||500 g sal para curar||3 yemas||200 g aceite||100 g aceite oliva||25 g salsa Worcestershire||30 g anchoas||20 g alcaparras||15 g perejil||7 g zumo limón||10 g mostaza||6 g ajo||Sal y pimienta",
                "Филе рыбы засыпать солью в форме с бортиками. Убрать в холодильник на 1 ч.||Хлеб нарезать кубиками, сбрызнуть оливковым маслом, засушить в духовке при 150 °C.||Петрушку, анчоусы и каперсы мелко покрошить.||Смешать желтки с маслами 3/200/100, соль, перец. Взбить до консистенции майонеза.||Добавить анчоусы, каперсы, петрушку, вустерский соус, лимонный сок, горчицу, чеснок. Взбить.||Рыбу промыть от соли, нарезать тонкими слайсами.||Листья салата смешать с соусом заранее.||Собрать салат: листья в соусе, яйца, черри, пармезан, сухарики, красная рыба. Подавать сразу.",
                "Put fish in dish, cover with salt. Refrigerate 1 h.||Cube bread, drizzle olive oil, dry in oven at 150 °C.||Chop parsley, anchovies, capers.||Mix yolks with oils 3/200/100, salt, pepper. Whisk to mayonnaise.||Add anchovies, capers, parsley, Worcestershire, lemon, mustard, garlic. Whisk.||Rinse fish, slice thin.||Toss lettuce with dressing.||Assemble: lettuce, eggs, cherry, Parmesan, croutons, fish. Serve immediately.",
                "Salar filete, refrigerar 1 h.||Cortar pan, aceite, secar al horno.||Picar perejil, anchoas, alcaparras.||Mezclar yemas con aceites, batir como mayonesa.||Añadir anchoas, alcaparras, etc.||Enjuagar pescado, cortar.||Mezclar lechuga con salsa.||Montar: lechuga, huevos, cherry, parmesano, crujientes, pescado.",
                "", "", "lunch");

        insertRecipe(db, 3,
                "Торт", "Cake", "Pastel",
                "3 ведра молока||2 шт сахара||1 яйцо||3 шт пшеница",
                "3 buckets of milk||2 sugar||1 egg||3 wheat",
                "3 cubos de leche||2 azúcar||1 huevo||3 trigo",
                "Яйца взбить с сахаром. Добавить молоко и растопленное масло.||Мука смешать с разрыхлителем и ванилином, всыпать в массу.||Вылить в форму и выпекать 30–35 минут при 180 °C.||Остудить — торт готов, как в Minecraft!",
                "Beat eggs with sugar. Add milk and melted butter.||Mix flour with baking powder and vanilla, fold in.||Pour into tin, bake 30–35 min at 180 °C.||Cool — cake ready, just like in Minecraft!",
                "Batir huevos con azúcar. Añadir leche y mantequilla derretida.||Mezclar harina con levadura y vainilla.||Hornear 30–35 min a 180 °C.||Enfriar.",
                "", "ing_minecraft_1||ing_minecraft_2||ing_minecraft_3||ing_minecraft_4||ing_minecraft_1||ing_minecraft_2||ing_minecraft_3", "breakfast");
        insertRecipe(db, 3,
                "Тушёные грибы", "Mushroom Stew", "Estofado de setas",
                "1 шт коричневый гриб||1 шт красный гриб||1 шт миска",
                "1 brown mushroom||1 red mushroom||1 bowl",
                "1 seta marrón||1 seta roja||1 tazón",
                "Грибы и лук нарезать. Обжарить на масле 5 минут.||Влить бульон и сливки, тушить 10–15 минут.||Посолить, поперчить, добавить зелень.||Подавать в миске — как тушёные грибы в Minecraft!",
                "Slice mushrooms and onion. Fry in oil 5 min.||Add stock and cream, simmer 10–15 min.||Salt, pepper, herbs.||Serve in a bowl — like Minecraft mushroom stew!",
                "Cortar setas y cebolla. Freír 5 min.||Añadir caldo y nata, cocer 10–15 min.||Sal, pimienta, hierbas.||Servir en cuenco.",
                "", "ing_minecraft_1||ing_minecraft_2||ing_minecraft_3||ing_minecraft_4||ing_minecraft_1||ing_minecraft_2||ing_minecraft_3||ing_minecraft_4", "lunch");
        insertRecipe(db, 3,
                "Хлеб", "Bread", "Pan",
                "3 шт пшеница",
                "3 wheat",
                "3 trigo",
                "Дрожжи растворить в тёплой воде с сахаром.||Смешать с мукой и солью, вымесить тесто. Поднять 1 час.||Сформировать батон, выпекать 35–40 минут при 200 °C.||Готовый хлеб — как крафт в Minecraft.",
                "Dissolve yeast in warm water with sugar.||Mix with flour and salt, knead. Rise 1 hour.||Shape loaf, bake 35–40 min at 200 °C.||Just like crafting bread in Minecraft.",
                "Disolver levadura en agua tibia con azúcar.||Mezclar con harina y sal, amasar. Dejar levar 1 h.||Formar barra, hornear 35–40 min a 200 °C.||Listo.",
                "", "ing_minecraft_1||ing_minecraft_2||ing_minecraft_3||ing_minecraft_4||ing_minecraft_1", "breakfast");

        insertRecipe(db, 4,
                "Запеченный кролик с розмарином и трюфелями", "Baked Rabbit with Rosemary and Truffles", "Conejo al horno con romero y trufa",
                "Тушка кролика 1 шт (около 1,2 кг)||Оливковое масло 3 ст.л.||Розмарин свежий 3 веточки||Чеснок 4 зубчика||Трюфельное масло 2 ст.л.||Чёрный трюфель 30 г (или трюфельная паста 1 ст.л.)||Соль и перец по вкусу||Белое сухое вино 100 мл",
                "1 whole rabbit (about 1.2 kg)||3 tbsp olive oil||3 sprigs fresh rosemary||4 cloves garlic||2 tbsp truffle oil||30 g black truffle (or 1 tbsp truffle paste)||Salt and pepper to taste||100 ml dry white wine",
                "1 conejo entero (unos 1,2 kg)||3 cucharadas aceite de oliva||3 ramitas de romero fresco||4 dientes de ajo||2 cucharadas aceite de trufa||30 g trufa negra (o 1 cucharada pasta de trufa)||Sal y pimienta al gusto||100 ml vino blanco seco",
                "Кролика вымыть, обсушить, натереть солью и перцем.||Смешать оливковое и трюфельное масло, смазать тушку снаружи и изнутри.||Положить внутрь розмарин и чеснок, часть трюфеля нарезать пластинами.||Выложить кролика в форму для запекания, полить вином. Духовку разогреть до 180 °C, запекать 50–60 мин, поливая соком.||Перед подачей посыпать оставшимся трюфелем и розмарином.",
                "Rinse and dry the rabbit, rub with salt and pepper.||Mix olive and truffle oil, brush the rabbit inside and out.||Place rosemary and garlic inside, slice some truffle.||Put rabbit in a roasting pan, add wine. Preheat oven to 180 °C, roast 50–60 min, basting.||Before serving, top with remaining truffle and rosemary.",
                "Lavar y secar el conejo, frotar con sal y pimienta.||Mezclar aceite de oliva y de trufa, untar el conejo por dentro y por fuera.||Poner dentro romero y ajo, cortar parte de la trufa en láminas.||Colocar el conejo en bandeja de horno, añadir vino. Hornear a 180 °C 50–60 min, rociando con jugo.||Antes de servir, espolvorear con trufa y romero restantes.",
                "img_0040161", "", "dinner");
    }

    private void insertBook(SQLiteDatabase db, String ru, String en, String es) {
        ContentValues v = new ContentValues();
        v.put("name_ru", ru);
        v.put("name_en", en);
        v.put("name_es", es);
        db.insert(TABLE_BOOKS, null, v);
    }

    private void insertRecipe(SQLiteDatabase db, long cookbookId,
                              String nameRu, String nameEn, String nameEs,
                              String ingRu, String ingEn, String ingEs,
                              String stepsRu, String stepsEn, String stepsEs,
                              String imageDish, String ingredientImages,
                              String mealType) {
        ContentValues v = new ContentValues();
        v.put("cookbook_id", cookbookId);
        v.put("name_ru", nameRu);
        v.put("name_en", nameEn);
        v.put("name_es", nameEs);
        v.put("ingredients_ru", ingRu);
        v.put("ingredients_en", ingEn);
        v.put("ingredients_es", ingEs);
        v.put("steps_ru", stepsRu);
        v.put("steps_en", stepsEn);
        v.put("steps_es", stepsEs);
        v.put("image_dish", imageDish);
        v.put("ingredient_images", ingredientImages);
        v.put("meal_type", mealType != null ? mealType : "lunch");
        db.insert(TABLE_RECIPES, null, v);
    }

    public List<Cookbook> getAllCookbooks() {
        List<Cookbook> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_BOOKS, null, null, null, null, null, "id")) {
            while (c.moveToNext()) {
                list.add(new Cookbook(
                        c.getLong(c.getColumnIndex("id")),
                        c.getString(c.getColumnIndex("name_ru")),
                        c.getString(c.getColumnIndex("name_en")),
                        c.getString(c.getColumnIndex("name_es"))
                ));
            }
        }
        return list;
    }

    private static final String[] RECIPE_COLS = {"id", "cookbook_id", "name_ru", "name_en", "name_es",
            "ingredients_ru", "ingredients_en", "ingredients_es",
            "steps_ru", "steps_en", "steps_es", "image_dish", "ingredient_images", "meal_type"};

    private Recipe recipeFromCursor(Cursor c) {
        return new Recipe(
                c.getLong(0), c.getLong(1),
                c.getString(2), c.getString(3), c.getString(4),
                c.getString(5), c.getString(6), c.getString(7),
                c.getString(8), c.getString(9), c.getString(10),
                c.isNull(11) ? null : c.getString(11),
                c.isNull(12) ? null : c.getString(12),
                c.isNull(13) ? null : c.getString(13)
        );
    }

    public List<Recipe> getRecipesFiltered(Long cookbookIdFilter, String mealTypeFilter) {
        List<Recipe> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder where = new StringBuilder();
        List<String> args = new ArrayList<>();
        if (cookbookIdFilter != null) {
            where.append("cookbook_id = ?");
            args.add(String.valueOf(cookbookIdFilter));
        }
        if (mealTypeFilter != null && !mealTypeFilter.isEmpty() && !"all".equals(mealTypeFilter)) {
            if (where.length() > 0) where.append(" AND ");
            where.append("meal_type = ?");
            args.add(mealTypeFilter);
        }
        String whereStr = where.length() > 0 ? where.toString() : null;
        String[] argArray = args.isEmpty() ? null : args.toArray(new String[0]);
        try (Cursor c = db.query(TABLE_RECIPES, RECIPE_COLS, whereStr, argArray, null, null, "id")) {
            while (c.moveToNext()) list.add(recipeFromCursor(c));
        }
        return list;
    }

    public Recipe getRecipeById(long recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_RECIPES, RECIPE_COLS, "id = ?", new String[]{String.valueOf(recipeId)}, null, null, null)) {
            if (c.moveToFirst()) return recipeFromCursor(c);
        }
        return null;
    }
}
