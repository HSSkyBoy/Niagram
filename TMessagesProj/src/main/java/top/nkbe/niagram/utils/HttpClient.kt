package top.nkbe.niagram.utils

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object HttpClient {

    @JvmField
    val MEDIA_TYPE_JSON: MediaType = "application/json; charset=utf-8".toMediaType()

    private val initialized = AtomicBoolean(false)

    val instance: OkHttpClient by lazy {
        initialized.set(true)
        OkHttpClient.Builder()
            .proxySelector(WebProxyManager.createDynamicProxySelector())
            .proxyAuthenticator(WebProxyManager.okHttpProxyAuthenticator)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val llmInstance: OkHttpClient by lazy {
        instance.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    val transcribeInstance: OkHttpClient by lazy {
        llmInstance.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @JvmStatic
    fun evictAll() {
        if (initialized.get()) {
            instance.connectionPool.evictAll()
        }
    }
}
