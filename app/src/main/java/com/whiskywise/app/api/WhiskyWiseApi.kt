package com.whiskywise.app.api

import com.whiskywise.app.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface WhiskyWiseApi {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("api/auth/token")
    suspend fun login(@Body body: LoginRequest): Response<TokenDataResponse>

    @GET("api/auth/tokens")
    suspend fun listTokens(): Response<TokenListResponse>

    @DELETE("api/auth/token/{tid}")
    suspend fun revokeToken(@Path("tid") tid: Int): Response<DeleteResponse>

    @GET("api/auth/sessions")
    suspend fun listSessions(): Response<SessionListResponse>

    @DELETE("api/auth/session/{sid}")
    suspend fun revokeSession(@Path("sid") sid: Int): Response<DeleteResponse>

    // ── Stats ─────────────────────────────────────────────────────────────────

    @GET("api/v1/stats")
    suspend fun getStats(): Response<StatsResponse>

    // ── Barcode lookup ────────────────────────────────────────────────────────

    @GET("api/barcode-lookup")
    suspend fun barcodeLookup(
        @Query("code") code: String,
    ): Response<BarcodeLookupResponse>

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
        @Query("retired")   retired: String? = null,
    ): Response<CollectionResponse>

    // ── Whisky CRUD ───────────────────────────────────────────────────────────

    @GET("api/v1/whisky/{id}")
    suspend fun getWhisky(@Path("id") id: Int): Response<WhiskyResponse>

    @POST("api/v1/whisky")
    suspend fun createWhisky(@Body body: WhiskyRequest): Response<WhiskyResponse>

    @PUT("api/v1/whisky/{id}")
    suspend fun updateWhisky(
        @Path("id") id: Int,
        @Body body: WhiskyRequest,
    ): Response<WhiskyResponse>

    @DELETE("api/v1/whisky/{id}")
    suspend fun deleteWhisky(@Path("id") id: Int): Response<DeleteResponse>

    // ── Wishlist ──────────────────────────────────────────────────────────────

    @GET("api/v1/wishlist")
    suspend fun getWishlist(
        @Query("sort")  sort: String?  = null,
        @Query("order") order: String? = null,
    ): Response<WhiskyListResponse>

    @POST("api/v1/wishlist")
    suspend fun createWishlistItem(@Body body: WhiskyRequest): Response<WhiskyResponse>

    @PUT("api/v1/wishlist/{id}")
    suspend fun updateWishlistItem(
        @Path("id") id: Int,
        @Body body: WhiskyRequest,
    ): Response<WhiskyResponse>

    // ── Photos ────────────────────────────────────────────────────────────────

    @Multipart
    @POST("api/v1/whisky/{id}/photo/{slot}")
    suspend fun uploadPhoto(
        @Path("id")   id: Int,
        @Path("slot") slot: String,
        @Part         photo: MultipartBody.Part,
    ): Response<PhotoResponse>

    @DELETE("api/v1/whisky/{id}/photo/{slot}")
    suspend fun deletePhoto(
        @Path("id")   id: Int,
        @Path("slot") slot: String,
    ): Response<PhotoResponse>

    @POST("api/photo/{id}/{slot}/rotate")
    suspend fun rotatePhoto(
        @Path("id")   id: Int,
        @Path("slot") slot: String,
    ): Response<PhotoResponse>
}
