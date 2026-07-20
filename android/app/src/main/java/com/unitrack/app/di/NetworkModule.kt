package com.unitrack.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.unitrack.app.BuildConfig
import com.unitrack.app.data.api.AcademicApiService
import com.unitrack.app.data.api.AttendanceApiService
import com.unitrack.app.data.api.AuthApiService
import com.unitrack.app.data.api.CalendarApiService
import com.unitrack.app.data.api.ExportApiService
import com.unitrack.app.data.api.SettingsApiService
import com.unitrack.app.data.api.TaskApiService
import com.unitrack.app.data.network.RetryInterceptor
import com.unitrack.app.data.network.api.auth.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            // BUG FIX: kotlinx.serialization varsayılan olarak, bir alanın değeri
            // Kotlin'deki default'una eşitse (ör. GradeBandDto.min = 0.0) o alanı
            // JSON'dan tamamen SİLİYOR. Backend Zod şeması ise o alanı zorunlu
            // sayısal alan olarak bekliyor — sonuç: "Expected number, received
            // undefined". En düşük harf notu min=0 OLMAK ZORUNDA olduğu için bu
            // hemen her not skalası kaydında tetikleniyordu. encodeDefaults=true
            // ile 0/""/null gibi varsayılan değerler de artık isteğe dahil ediliyor.
            encodeDefaults = true
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {

        // BUG FIX: BODY seviyesi (token/şifre/transkript verisi dahil tüm
        // request-response gövdesi) daha önce release build'de de sabitti —
        // logcat'e prod'da bile hassas veri yazılıyordu. Artık sadece
        // debug build'de açık; release'de hiç log basmıyor.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            // En dışta: bir istek retry edildiğinde authInterceptor/logging
            // de her denemede yeniden çalışsın (ör. token bu sırada değiştiyse
            // güncel olanı taşısın).
            .addInterceptor(RetryInterceptor())
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            // 401 alındığında gerçek refresh akışını devreye sokan asıl bağlantı buydu, eksikti.
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json
    ): Retrofit {

        return Retrofit.Builder()
            // Backend artık /api/v1/ altında versiyonlu; eski /api/ hâlâ bir süre daha
            // geriye dönük uyumluluk alias'ı olarak çalışıyor ama biz baştan kanonik
            // path'i kullanalım, deprecated olanı taşımayalım.
            //
            // Adres artık build type'a göre değişiyor (bkz. app/build.gradle.kts):
            // debug -> emulator'ın host'a baktığı 10.0.2.2, release -> gerçek prod domain.
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json".toMediaType()
                )
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(
        retrofit: Retrofit
    ): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideAcademicApiService(
        retrofit: Retrofit
    ): AcademicApiService =
        retrofit.create(AcademicApiService::class.java)

    @Provides
    @Singleton
    fun provideCalendarApiService(
        retrofit: Retrofit
    ): CalendarApiService =
        retrofit.create(CalendarApiService::class.java)

    @Provides
    @Singleton
    fun provideTaskApiService(
        retrofit: Retrofit
    ): TaskApiService =
        retrofit.create(TaskApiService::class.java)

    @Provides
    @Singleton
    fun provideAttendanceApiService(
        retrofit: Retrofit
    ): AttendanceApiService =
        retrofit.create(AttendanceApiService::class.java)

    @Provides
    @Singleton
    fun provideSettingsApiService(
        retrofit: Retrofit
    ): SettingsApiService =
        retrofit.create(SettingsApiService::class.java)

    @Provides
    @Singleton
    fun provideExportApiService(
        retrofit: Retrofit
    ): ExportApiService =
        retrofit.create(ExportApiService::class.java)
}