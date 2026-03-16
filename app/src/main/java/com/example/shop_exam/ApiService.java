package com.example.shop_exam;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Интерфейс API сервера
 */
public interface ApiService {

    // Авторизация
    @FormUrlEncoded
    @POST("api/login")
    Call<LoginResponse> loginUser(
            @Field("username") String username,
            @Field("password") String password
    );

    // Авторизация через Google (создаёт пользователя если нет)
    @POST("api/auth/google")
    Call<LoginResponse> googleAuth(@Body GoogleAuthRequest request);

    // Регистрация (с отправкой кода верификации на почту)
    @POST("api/auth/register")
    Call<RegisterVerifyResponse> registerUser(@Body RegisterRequest request);

    // Подтверждение email кодом
    @POST("api/auth/verify-email")
    Call<LoginResponse> verifyEmail(@Body VerifyEmailRequest request);

    // Повторная отправка кода верификации
    @POST("api/auth/resend-code")
    Call<Void> resendCode(@Body ResendCodeRequest request);

    // Список товаров (поиск + фильтры)
    @GET("api/game_variants")
    Call<List<GameVariantForList>> getGameVariants(
            @Query("q") String searchQuery,
            @Query("min_players") Integer minPlayers,
            @Query("max_players") Integer maxPlayers,
            @Query("min_age") Integer minAge,
            @Query("price_min") Double priceMin,
            @Query("price_max") Double priceMax,
            @Query("in_stock") Boolean inStock,
            @Query("genres") String genres,
            @Query("complexity") String complexity,
            @Query("playtime_min") Integer playtimeMin,
            @Query("playtime_max") Integer playtimeMax
    );

    // Список жанров для фильтров
    @GET("api/genres")
    Call<List<GenreItem>> getGenres();

    // Детали товара
    @GET("api/game_variants/{id}")
    Call<GameDetail> getGameDetails(@Path("id") int id);

    // Корзина (детали с товарами — только для авторизованных)
    @GET("api/cart")
    Call<ServerCartResponse> getCart();

    // Добавление в корзину
    @POST("api/cart/items")
    Call<CartResponse> addToCart(@Body CartAddRequest request);

    // Изменение количества товара в корзине
    @PUT("api/cart/items/{game_variant_id}")
    Call<CartResponse> setCartItemQuantity(
            @Path("game_variant_id") int variantId,
            @Body CartSetQuantityRequest request
    );

    // Профиль пользователя
    @GET("api/me")
    Call<User> getMe();

    // Обновление профиля
    @PUT("api/me")
    Call<User> updateProfile(@Body UpdateProfileRequest request);

    // Список заказов пользователя
    @GET("api/my_orders")
    Call<List<OrderResponse>> getMyOrders();

    // Детали заказа
    @GET("api/my_orders/{order_id}")
    Call<OrderDetailResponse> getOrderDetail(@Path("order_id") int orderId);

    // Поиск адреса (подсказки)
    @GET("api/address_suggest")
    Call<List<AddressSuggestion>> addressSuggest(@Query("q") String query);

    // Детали адреса (геокодирование)
    @GET("api/address_details")
    Call<AddressDetails> addressDetails(@Query("q") String query);

    // Обратное геокодирование (координаты в адрес)
    @GET("api/address_reverse")
    Call<AddressDetails> addressReverse(
            @Query("lat") double lat,
            @Query("lon") double lon
    );

    // Оформление заказа
    @POST("api/orders/checkout")
    Call<CheckoutResponse> checkout(@Body CheckoutRequest request);
}
