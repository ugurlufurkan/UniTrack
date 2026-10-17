package com.unitrack.app.data.session

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.util.Base64

internal fun tokenOwner(token: String?): String? = runCatching {
    val payload = token?.split('.')?.getOrNull(1) ?: return null
    Json.parseToJsonElement(String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8))
        .jsonObject["userId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
}.getOrNull()

@PublishedApi
internal suspend fun <T> readThroughAccountCache(
    key: String,
    owner: suspend () -> String?,
    network: suspend () -> T,
    read: suspend (String) -> T?,
    write: suspend (String, T) -> Unit
): T {
    val account = owner() ?: return network()
    val scoped = "$account:$key"
    return try {
        val fresh = network()
        if (owner() != account) throw CancellationException("Account changed during request")
        write(scoped, fresh)
        fresh
    } catch (error: IOException) {
        if (owner() != account) throw CancellationException("Account changed during request")
        read(scoped) ?: throw error
    }
}
