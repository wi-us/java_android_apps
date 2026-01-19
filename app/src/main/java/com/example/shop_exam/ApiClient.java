package com.example.shop_exam;

import android.content.Context;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

/**
 * Класс для создания HTTP-клиента
 */
public class ApiClient {

    // Адрес сервера
    private static final String BASE_URL = "https://shop41.wi-us.ru/";

    private static Retrofit retrofit = null;

    // Преобразование относительных URL в полные
    public static String resolveUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // URL начинается с "//"
        if (url.startsWith("//")) {
            return "https:" + url;
        }

        // Получаем базовый URL без слеша на конце
        String base = BASE_URL;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        // URL начинается с "/"
        if (url.startsWith("/")) {
            return base + url;
        }

        // URL начинается с "cache/"
        if (url.startsWith("cache/")) {
            return base + "/" + url;
        }

        // URL с localhost - заменяем на наш сервер
        if (url.startsWith("http://127.0.0.1:8003/")) {
            return base + url.substring("http://127.0.0.1:8003".length());
        }
        if (url.startsWith("http://localhost:8003/")) {
            return base + url.substring("http://localhost:8003".length());
        }

        return url;
    }

    // Создание клиента Retrofit
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(context))
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    /**
     * Перехватчик для добавления заголовков к запросам.
     * Добавляет токен авторизации и API-ключ 2ГИС.
     */
    private static class AuthInterceptor implements Interceptor {
        private Context context;

        public AuthInterceptor(Context context) {
            this.context = context;
        }

            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request.Builder builder = original.newBuilder();

                // Токен авторизации
                String token = TokenStore.getToken(context);
                if (token != null && !token.isEmpty()) {
                    builder.header("Authorization", "Bearer " + token);
                }

                // API-ключ 2ГИС
                try {
                    String dgisKey = context.getString(R.string.dgis_map_key);
                    if (dgisKey != null && !dgisKey.isEmpty()) {
                        builder.header("X-DGIS-KEY", dgisKey);
                    }
                } catch (Exception e) {}

                return chain.proceed(builder.build());
            }
    }
}
