package com.unitrack.app.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming

interface ExportApiService {

    // Ham gövde olarak alınıyor (tipli DTO'ya çevrilmiyor): dışa aktarma
    // sadece kullanıcıya olduğu gibi bir JSON dosyası indirmek için, ayrıştırıp
    // ekranda göstermiyoruz. @Streaming, büyük gövdeyi belleğe tek seferde
    // yüklemek yerine parça parça diske yazabilmemizi sağlar.
    @Streaming
    @GET("export")
    suspend fun exportMyData(): Response<ResponseBody>
}
