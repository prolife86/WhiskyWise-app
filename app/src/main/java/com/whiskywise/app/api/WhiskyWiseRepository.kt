package com.whiskywise.app.api

import com.whiskywise.app.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File

class WhiskyWiseRepository {

    private val api get() = RetrofitClient.api

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun login(username: String, password: String, deviceName: String = "WhiskyWise Android"): Result<TokenData> =
        safeCall { api.login(LoginRequest(username, password, deviceName)) }.map { it.data }

    suspend fun listTokens(): Result<List<TokenListItem>> =
        safeCall { api.listTokens() }.map { it.data }

    suspend fun revokeToken(id: Int): Result<Unit> =
        safeCall { api.revokeToken(id) }.map { }

    suspend fun listSessions(): Result<List<ApiSession>> =
        safeCall { api.listSessions() }.map { it.data }

    suspend fun revokeSession(id: Int): Result<Unit> =
        safeCall { api.revokeSession(id) }.map { }

    // ── Stats ─────────────────────────────────────────────────────────────────

    suspend fun getStats(): Result<Stats> =
        safeCall { api.getStats() }.map { it.data }

    suspend fun barcodeLookup(code: String): Result<BarcodeLookupResponse> =
        safeCall { api.barcodeLookup(code) }.map { it }

    /** Promote a wishlist item to the collection by flipping wishlist=false and setting status. */
    suspend fun promoteToCollection(id: Int, status: String): Result<Whisky> {
        // GET works on both collection and wishlist entries (no wishlist filter).
        // PUT /api/v1/whisky/{id} filters wishlist=False and returns 404 for
        // wishlist items, so we must use PUT /api/v1/wishlist/{id} instead.
        val current = getWhisky(id).getOrElse { return Result.failure(it) }
        val req = WhiskyRequest(
            name     = current.name,
            wishlist = false,
            status   = status,
        )
        return safeCall { api.updateWishlistItem(id, req) }.map { it.data }
    }

    // ── Collection ────────────────────────────────────────────────────────────

    suspend fun getCollection(
        query: String? = null,
        flavor: String? = null,
        minScore: Float? = null,
        maxPrice: Float? = null,
        status: String? = null,
        sort: String? = null,
        order: String? = null,
        retired: String? = null,
        limit: Int = 200,
        offset: Int = 0,
    ): Result<CollectionResponse> =
        safeCall { api.getCollection(query, flavor, minScore, maxPrice, status, sort, order, limit, offset, retired) }

    // ── Whisky CRUD ───────────────────────────────────────────────────────────

    suspend fun getWhisky(id: Int): Result<Whisky> =
        safeCall { api.getWhisky(id) }.map { it.data }

    suspend fun createWhisky(body: WhiskyRequest): Result<Whisky> =
        safeCall { api.createWhisky(body) }.map { it.data }

    suspend fun updateWhisky(id: Int, body: WhiskyRequest): Result<Whisky> =
        safeCall { api.updateWhisky(id, body) }.map { it.data }

    suspend fun deleteWhisky(id: Int): Result<Unit> =
        safeCall { api.deleteWhisky(id) }.map { }

    // ── Wishlist ──────────────────────────────────────────────────────────────

    suspend fun getWishlist(
        sort: String? = null,
        order: String? = null,
    ): Result<List<Whisky>> =
        safeCall { api.getWishlist(sort, order) }.map { it.data }

    suspend fun createWishlistItem(body: WhiskyRequest): Result<Whisky> =
        safeCall { api.createWishlistItem(body) }.map { it.data }

    suspend fun updateWishlistItem(id: Int, body: WhiskyRequest): Result<Whisky> =
        safeCall { api.updateWishlistItem(id, body) }.map { it.data }

    // ── Photos ────────────────────────────────────────────────────────────────

    suspend fun uploadPhoto(whiskyId: Int, slot: String, file: File): Result<Unit> {
        val body = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("photo", file.name, body)
        return safeCall { api.uploadPhoto(whiskyId, slot, part) }.map { }
    }

    suspend fun deletePhoto(whiskyId: Int, slot: String): Result<Unit> =
        safeCall { api.deletePhoto(whiskyId, slot) }.map { }

    suspend fun rotatePhoto(whiskyId: Int, slot: String): Result<Unit> =
        safeCall { api.rotatePhoto(whiskyId, slot) }.map { }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun <T> safeCall(block: suspend () -> Response<T>): Result<T> = try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) Result.success(body)
            else Result.failure(Exception("Empty response body"))
        } else {
            val errorMsg = response.errorBody()?.string() ?: "HTTP ${response.code()}"
            Result.failure(Exception(errorMsg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
