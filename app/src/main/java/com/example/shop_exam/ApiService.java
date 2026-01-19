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

    // Регистрация
    @POST("api/users")
    Call<RegisterResponse> registerUser(@Body RegisterRequest request);

    // Список товаров
    @GET("api/game_variants")
    Call<List<GameVariantForList>> getGameVariants();

    // Детали товара
    @GET("api/game_variants/{id}")
    Call<GameDetail> getGameDetails(@Path("id") int id);

    // Корзина
    @GET("api/cart")
    Call<CartResponse> getCart();

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
