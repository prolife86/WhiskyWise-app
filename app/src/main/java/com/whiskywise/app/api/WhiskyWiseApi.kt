package com.whiskywise.app.api

import com.whiskywise.app.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface WhiskyWiseApi {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("api/auth/token")
    suspend fun login(@Body body: LoginRequest): Response<SingleResponse<TokenData>>

    @GET("api/auth/tokens")
    suspend fun listTokens(): Response<SingleResponse<List<TokenListItem>>>

    @DELETE("api/auth/token/{tid}")
    suspend fun revokeToken(@Path("tid") tid: Int): Response<SingleResponse<Map<String, Int>>>

    // ── Stats ─────────────────────────────────────────────────────────────────

    @GET("api/v1/stats")
    suspend fun getStats(): Response<SingleResponse<Stats>>

    // ── Collection ────────────────────────────────────────────────────────────

    @GET("api/v1/collection")
    suspend fun getCollection(
        @Query("q")         query: String?  = null,
        @Query("flavor")    flavor: String? = null,
        @Query("min_score") minScore: Float? = null,
        @Query("max_price") maxPrice: Float? = null,
        @Query("status")    status: String? = null,
        @Query("sort")      sort: String?   = null,
        @Query("order")     order: String?  = null,
        @Query("limit")     limit: Int      = 200,
        @Query("offset")    offset: Int     = 0,
    ): Response<CollectionResponse>

    // ── Whisky CRUD ───────────────────────────────────────────────────────────

    @GET("api/v1/whisky/{id}")
    suspend fun getWhisky(@Path("id") id: Int): Response<SingleResponse<Whisky>>

    @POST("api/v1/whisky")
    suspend fun createWhisky(@Body body: WhiskyRequest): Response<SingleResponse<Whisky>>

    @PUT("api/v1/whisky/{id}")
    suspend fun updateWhisky(
        @Path("id") id: Int,
        @Body body: WhiskyRequest,
    ): Response<SingleResponse<Whisky>>

    @DELETE("api/v1/whisky/{id}")
    suspend fun deleteWhisky(@Path("id") id: Int): Response<SingleResponse<Map<String, Int>>>

    // ── Wishlist ──────────────────────────────────────────────────────────────

    @GET("api/v1/wishlist")
    suspend fun getWishlist(): Response<SingleResponse<List<Whisky>>>

    @POST("api/v1/wishlist")
    suspend fun createWishlistItem(@Body body: WhiskyRequest): Response<SingleResponse<Whisky>>

    @PUT("api/v1/wishlist/{id}")
    suspend fun updateWishlistItem(
        @Path("id") id: Int,
        @Body body: WhiskyRequest,
    ): Response<SingleResponse<Whisky>>

    // ── Photos ────────────────────────────────────────────────────────────────

    @Multipart
    @POST("api/v1/whisky/{id}/photo/{slot}")
    suspend fun uploadPhoto(
        @Path("id")   id: Int,
        @Path("slot") slot: String,
        @Part         photo: MultipartBody.Part,
    ): Response<SingleResponse<Map<String, String>>>

    @DELETE("api/v1/whisky/{id}/photo/{slot}")
    suspend fun deletePhoto(
        @Path("id")   id: Int,
        @Path("slot") slot: String,
    ): Response<SingleResponse<Map<String, String?>>>

    // ── Barcode lookup ────────────────────────────────────────────────────────

    @GET("api/barcode-lookup")
    suspend fun lookupBarcode(@Query("barcode") barcode: String): Response<Map<String, Any?>>
}
