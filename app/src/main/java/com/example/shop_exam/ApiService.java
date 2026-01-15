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

public interface ApiService {

    @FormUrlEncoded
    @POST("api/login")
    Call<LoginResponse> loginUser(@Field("username") String username, @Field("password") String password);

    @POST("api/users")
    Call<RegisterResponse> registerUser(@Body RegisterRequest registerRequest);

    @GET("api/game_variants")
    Call<List<GameVariantForList>> getGameVariants();

    /**
     * Получает детальную информацию об одной игре по ее ID.
     */
    @GET("api/game_variants/{id}")
    Call<GameDetail> getGameDetails(@Path("id") int variantId);

    /**
     * Добавляет товар в корзину.
     */
    @GET("api/cart")
    Call<CartResponse> getCart();

    @POST("api/cart/items")
    Call<CartResponse> addToCart(@Body CartAddRequest req);

    @PUT("api/cart/items/{game_variant_id}")
    Call<CartResponse> setCartItemQuantity(@Path("game_variant_id") int gameVariantId, @Body CartSetQuantityRequest req);

    // Profile
    @GET("api/me")
    Call<User> getMe();

    // Address autocomplete
    @GET("api/address_suggest")
    Call<List<AddressSuggestion>> addressSuggest(
            @Query("q") String q
    );

    @GET("api/address_details")
    Call<AddressDetails> addressDetails(@Query("q") String q);

    @GET("api/address_reverse")
    Call<AddressDetails> addressReverse(@Query("lat") double lat, @Query("lon") double lon);

    // Checkout
    @POST("api/orders/checkout")
    Call<CheckoutResponse> checkout(@Body CheckoutRequest req);
}
