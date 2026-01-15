package com.example.shop_exam;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://5.141.90.239:8003/";
    private static Retrofit retrofit = null;

    public static String getBaseUrl() {
        return BASE_URL;
    }

    /**
     * Normalizes URLs coming from backend:
     * - "/cache/..." -> "http://10.0.2.2:8003/cache/..."
     * - "cache/..."  -> "http://10.0.2.2:8003/cache/..."
     * - "http://127.0.0.1:8003/..." or "http://localhost:8003/..." -> replace host with 10.0.2.2
     * - "//cdn..." -> "https://cdn..."
     */
    public static String resolveUrl(String rawUrl) {
        if (rawUrl == null) return null;
        String url = rawUrl.trim();
        if (url.isEmpty()) return url;

        if (url.startsWith("//")) {
            return "https:" + url;
        }

        String base = BASE_URL.endsWith("/") ? BASE_URL.substring(0, BASE_URL.length() - 1) : BASE_URL;

        if (url.startsWith("/")) {
            return base + url;
        }

        if (url.startsWith("cache/")) {
            return base + "/" + url;
        }

        if (url.startsWith("http://127.0.0.1:8003/")) {
            return base + url.substring("http://127.0.0.1:8003".length());
        }

        if (url.startsWith("http://localhost:8003/")) {
            return base + url.substring("http://localhost:8003".length());
        }

        return url;
    }

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                String token = TokenStore.getToken(context);
                // 2GIS key (for address suggest/geocode via backend fallback)
                String dgisKey = null;
                try {
                    dgisKey = context.getString(R.string.dgis_map_key);
                } catch (Exception e) {
                    // ignore
                }
                if (token == null || token.isEmpty()) {
                    Request.Builder b = original.newBuilder();
                    if (dgisKey != null && !dgisKey.trim().isEmpty()) {
                        b.header("X-DGIS-KEY", dgisKey.trim());
                    }
                    return chain.proceed(b.build());
                }
                Request.Builder builder = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        ;
                if (dgisKey != null && !dgisKey.trim().isEmpty()) {
                    builder.header("X-DGIS-KEY", dgisKey.trim());
                }
                return chain.proceed(builder.build());
            });
            
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return retrofit;
    }
}

