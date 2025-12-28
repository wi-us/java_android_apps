package com.example.lab8;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

// Один файл: и сеть, и DTO. Так часто делают в учебных работах.
public class Api {

    private static final String BASE_URL = "https://nti.urfu.ru/";

    private static ApiService api;

    public static ApiService getApi() {
        if (api != null) return api;

        HttpLoggingInterceptor log = new HttpLoggingInterceptor();
        log.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient ok = new OkHttpClient.Builder()
                .addInterceptor(log)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(ok)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(ApiService.class);
        return api;
    }

    public interface ApiService {
        @Headers("Content-Type: application/json")
        @POST("ege-calc/json")
        Call<List<SpecialtyDto>> postScores(@Body Map<String, Integer> scores);
    }

    public static class SpecialtyDto {
        @SerializedName("id")
        public int id;

        @SerializedName("slug")
        public String slug;

        @SerializedName("specialty_name")
        public String specialtyName;

        @SerializedName("specialty_name_en")
        public String specialtyNameEn;

        @SerializedName("math_point")
        public int mathPoint;

        @SerializedName("rus_point")
        public int rusPoint;

        @SerializedName("itk_point")
        public int itkPoint;

        @SerializedName("chem_point")
        public int chemPoint;

        @SerializedName("soc_point")
        public int socPoint;

        @SerializedName("phys_point")
        public int physPoint;
    }
}


