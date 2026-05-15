package com.whiskywise.app.model

import com.google.gson.annotations.SerializedName

// ── Whisky ────────────────────────────────────────────────────────────────────

/**
 * Radar data as returned by the server's _whisky_to_dict():
 *   "radar": { "woody": 3, "smoky": 1, ... }
 *
 * Note: the server accepts EITHER this nested form OR flat radar_woody keys
 * on write, but it always RETURNS the nested form on read.
 */
data class RadarData(
    val woody:     Int = 0,
    val smoky:     Int = 0,
    val cereal:    Int = 0,
    val floral:    Int = 0,
    val fruity:    Int = 0,
    val medicinal: Int = 0,
    val fiery:     Int = 0,
)

data class Whisky(
    val id: Int = 0,
    val name: String = "",
    val distillery: String? = null,
    val region: String? = null,
    val age: String? = null,
    val abv: Double? = null,
    val barcode: String? = null,
    val status: String? = "stashed",
    val retired: Boolean = false,
    val price: Double? = null,
    val store: String? = null,
    val notes: String? = null,
    val nose: String? = null,
    val palate: String? = null,
    val finish: String? = null,
    @SerializedName("flavor_profile") val flavorProfile: String? = null,
    val score: Double? = null,
    // The server returns radar values as a nested object, not flat fields.
    // Gson maps "radar": {...} to this property automatically.
    val radar: RadarData? = null,
    @SerializedName("photo_front")     val photoFront: String? = null,
    @SerializedName("photo_back")      val photoBack: String? = null,
    @SerializedName("photo_cask")      val photoCask: String? = null,
    @SerializedName("photo_barcode")   val photoBarcode: String? = null,
    val wishlist: Boolean = false,
    @SerializedName("wishlist_notes")  val wishlistNotes: String? = null,
    @SerializedName("last_tasted")     val lastTasted: String? = null,
    @SerializedName("created_at")      val createdAt: String? = null,
    @SerializedName("updated_at")      val updatedAt: String? = null,
) {
    // Convenience accessors so call sites don't need null-safe chaining everywhere.
    val radarWoody:     Int get() = radar?.woody     ?: 0
    val radarSmoky:     Int get() = radar?.smoky     ?: 0
    val radarCereal:    Int get() = radar?.cereal    ?: 0
    val radarFloral:    Int get() = radar?.floral    ?: 0
    val radarFruity:    Int get() = radar?.fruity    ?: 0
    val radarMedicinal: Int get() = radar?.medicinal ?: 0
    val radarFiery:     Int get() = radar?.fiery     ?: 0
}

// ── API response wrappers — concrete classes, no generics (avoids Gson type erasure) ──

data class WhiskyResponse(val data: Whisky)
data class WhiskyListResponse(val data: List<Whisky>)
data class TokenDataResponse(val data: TokenData)
data class TokenListResponse(val data: List<TokenListItem>)
data class StatsResponse(val data: Stats)
data class DeleteResponse(val data: Map<String, Int>)
data class PhotoResponse(val data: Map<String, String?>)
data class BarcodeLookupResponse(val found: Boolean, val id: Int? = null, val name: String? = null)

data class CollectionResponse(
    val data: List<Whisky>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val username: String,
    val password: String,
    val name: String = "WhiskyWise Android",
)

data class TokenData(
    val token: String,
    val id: Int,
    val name: String,
    val created: String,
)

data class TokenListItem(
    val id: Int,
    val name: String,
    val created: String,
    @SerializedName("last_used")      val lastUsed: String?,
    @SerializedName("origin_ip")      val originIp: String?,
    @SerializedName("client_version") val clientVersion: String?,
)

data class ApiSession(
    val id: Int,
    val created: String,
    @SerializedName("last_seen")      val lastSeen: String?,
    @SerializedName("origin_ip")      val originIp: String?,
    @SerializedName("client_version") val clientVersion: String?,
    @SerializedName("user_agent")     val userAgent: String?,
    val current: Boolean,
)

data class SessionListResponse(val data: List<ApiSession>)

// ── Stats ─────────────────────────────────────────────────────────────────────

data class Stats(
    val total: Int,
    val open: Int,
    val stashed: Int,
    val finished: Int = 0,
    @SerializedName("wishlist_count") val wishlistCount: Int,
    val top10: List<Whisky>,
    @SerializedName("dominant_flavours") val dominantFlavours: List<String>,
)

// ── Request bodies ────────────────────────────────────────────────────────────

data class WhiskyRequest(
    val name: String,
    val distillery: String? = null,
    val region: String? = null,
    val age: String? = null,
    val abv: Double? = null,
    val barcode: String? = null,
    val status: String? = null,
    val retired: Boolean? = null,
    val price: Double? = null,
    val store: String? = null,
    val notes: String? = null,
    val nose: String? = null,
    val palate: String? = null,
    val finish: String? = null,
    @SerializedName("flavor_profile") val flavorProfile: String? = null,
    val score: Double? = null,
    // Send as flat keys — the server accepts both flat and nested on write.
    // Non-nullable Int so 0 is always serialised (never omitted as null).
    @SerializedName("radar_woody")     val radarWoody: Int = 0,
    @SerializedName("radar_smoky")     val radarSmoky: Int = 0,
    @SerializedName("radar_cereal")    val radarCereal: Int = 0,
    @SerializedName("radar_floral")    val radarFloral: Int = 0,
    @SerializedName("radar_fruity")    val radarFruity: Int = 0,
    @SerializedName("radar_medicinal") val radarMedicinal: Int = 0,
    @SerializedName("radar_fiery")     val radarFiery: Int = 0,
    @SerializedName("wishlist_notes")  val wishlistNotes: String? = null,
    @SerializedName("last_tasted")     val lastTasted: String? = null,
)
