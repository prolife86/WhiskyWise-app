# WhiskyWise ProGuard rules

# Keep data model classes (Gson serialisation)
-keep class com.whiskywise.app.model.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

# MLKit barcode
-keep class com.google.mlkit.** { *; }

# AndroidX Security / EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }
