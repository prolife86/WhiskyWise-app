package com.whiskywise.app.model

import com.google.gson.annotations.SerializedName

// ── Whisky ────────────────────────────────────────────────────────────────────

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
    @SerializedName("radar_woody")     val radarWoody: Int = 0,
    @SerializedName("radar_smoky")     val radarSmoky: Int = 0,
    @SerializedName("radar_cereal")    val radarCereal: Int = 0,
    @SerializedName("radar_floral")    val radarFloral: Int = 0,
    @SerializedName("radar_fruity")    val radarFruity: Int = 0,
    @SerializedName("radar_medicinal") val radarMedicinal: Int = 0,
    @SerializedName("radar_fiery")     val radarFiery: Int = 0,
    @SerializedName("photo_front")     val photoFront: String? = null,
    @SerializedName("photo_back")      val photoBack: String? = null,
    @SerializedName("photo_cask")      val photoCask: String? = null,
    @SerializedName("photo_barcode")   val photoBarcode: String? = null,
    val wishlist: Boolean = false,
    @SerializedName("wishlist_notes")  val wishlistNotes: String? = null,
    @SerializedName("created_at")      val createdAt: String? = null,
    @SerializedName("updated_at")      val updatedAt: String? = null,
)

// ── API response wrappers — concrete classes, no generics (avoids Gson type erasure) ──

data class WhiskyResponse(val data: Whisky)
data class WhiskyListResponse(val data: List<Whisky>)
data class TokenDataResponse(val data: TokenData)
data class TokenListResponse(val data: List<TokenListItem>)
data class StatsResponse(val data: Stats)
data class DeleteResponse(val data: Map<String, Int>)
data class PhotoResponse(val data: Map<String, String?>)

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
    @SerializedName("last_used") val lastUsed: String?,
)

// ── Stats ─────────────────────────────────────────────────────────────────────

data class Stats(
    val total: Int,
    val open: Int,
    val stashed: Int,
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
    @SerializedName("radar_woody")     val radarWoody: Int? = null,
    @SerializedName("radar_smoky")     val radarSmoky: Int? = null,
    @SerializedName("radar_cereal")    val radarCereal: Int? = null,
    @SerializedName("radar_floral")    val radarFloral: Int? = null,
    @SerializedName("radar_fruity")    val radarFruity: Int? = null,
    @SerializedName("radar_medicinal") val radarMedicinal: Int? = null,
    @SerializedName("radar_fiery")     val radarFiery: Int? = null,
    @SerializedName("wishlist_notes")  val wishlistNotes: String? = null,
)
