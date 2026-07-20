# UniTrack — release (R8/ProGuard) kuralları.
# build.gradle.kts release buildType bu dosyayı referans alıyor ama dosya
# eksikti; isMinifyEnabled = true yapıldığında derleme bu olmadan patlardı
# ya da (daha kötüsü) sessizce kırık bir AAB üretebilirdi.

# ── Genel ────────────────────────────────────────────────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes Exceptions, *Annotation*

# Satır numaralarını obfuscate etme ama dosya adını sakla — çökme
# raporlarını (Play Console) okunabilir tutmak için.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ── kotlinx.serialization ───────────────────────────────────────────
# Resmi öneri: https://github.com/Kotlin/kotlinx.serialization#android
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Uygulamanın kendi @Serializable DTO'ları (com.unitrack.app.data.dto.*)
-keep,includedescriptorclasses class com.unitrack.app.**$$serializer { *; }
-keepclassmembers class com.unitrack.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.unitrack.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.unitrack.app.data.dto.** { *; }

# ── Retrofit / OkHttp (resmi kurallar) ──────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions

-keepclasseswithmembers interface com.unitrack.app.data.api.** { *; }

# Retrofit, servis arayüzü metodlarındaki generic dönüş tipini (Response<T>,
# suspend fun -> Continuation) reflection ile okur.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers class kotlin.coroutines.Continuation

# ── Hilt / Dagger ────────────────────────────────────────────────────
# AGP + Hilt Gradle plugin gerekli consumer-proguard kurallarını otomatik
# ekler; burada ekstra bir şey gerekmiyor.

# ── Credential Manager / Google Identity (Google ile giriş) ────────
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# ── Genel Kotlin uyarıları ───────────────────────────────────────────
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
