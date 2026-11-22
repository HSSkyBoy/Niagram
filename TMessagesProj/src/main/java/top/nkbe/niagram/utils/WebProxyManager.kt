package top.nkbe.niagram.utils

import okhttp3.Authenticator as OkHttpAuthenticator
import okhttp3.Credentials
import okhttp3.Response
import okhttp3.Route
import org.telegram.messenger.FileLog
import org.telegram.messenger.SharedConfig
import org.telegram.utils.proxy.ProxySettings
import top.nkbe.niagram.config.NyaConfig
import java.io.IOException
import java.net.Authenticator as JavaAuthenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

object WebProxyManager {

    private val javaAuthInstalled = java.util.concurrent.atomic.AtomicBoolean(false)
    private val systemProxySelector: ProxySelector? = ProxySelector.getDefault()

    init {
        ensureJavaAuthenticator()
    }

    private fun ensureJavaAuthenticator() {
        if (javaAuthInstalled.compareAndSet(false, true)) {
            try {
                JavaAuthenticator.setDefault(object : JavaAuthenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication? {
                        if (requestorType == RequestorType.PROXY) {
                            val creds = getEffectiveProxyCredentials()
                            if (creds != null && creds.first.isNotEmpty()) {
                                return PasswordAuthentication(creds.first, creds.second.toCharArray())
                            }
                        }
                        return super.getPasswordAuthentication()
                    }
                })
            } catch (e: Throwable) {
                FileLog.e("WebProxyManager: failed to set default Java Authenticator", e)
            }
        }
    }

    @JvmStatic
    fun getEffectiveProxy(): Proxy {
        return when (NyaConfig.webProxyMode.Int()) {
            NyaConfig.WEB_PROXY_MODE_FOLLOW_TELEGRAM -> {
                if (SharedConfig.isProxyEnabled() && SharedConfig.currentProxy != null) {
                    val info = SharedConfig.currentProxy
                    if (info.settings.type == ProxySettings.Type.SOCKS5 && !info.address.isNullOrEmpty() && info.port in 1..65535) {
                        try {
                            Proxy(Proxy.Type.SOCKS, InetSocketAddress(info.address, info.port))
                        } catch (e: Throwable) {
                            FileLog.e("WebProxyManager: invalid TG SOCKS5 proxy", e)
                            Proxy.NO_PROXY
                        }
                    } else {
                        // MTProto / Web proxy cannot handle arbitrary HTTP/HTTPS traffic
                        Proxy.NO_PROXY
                    }
                } else {
                    Proxy.NO_PROXY
                }
            }
            NyaConfig.WEB_PROXY_MODE_CUSTOM -> {
                val host = NyaConfig.webProxyHost.String()?.trim()
                val port = NyaConfig.webProxyPort.String()?.trim()?.toIntOrNull() ?: 0
                if (!host.isNullOrEmpty() && port in 1..65535) {
                    try {
                        val proxyType = if (NyaConfig.webProxyType.Int() == NyaConfig.WEB_PROXY_TYPE_SOCKS5) {
                            Proxy.Type.SOCKS
                        } else {
                            Proxy.Type.HTTP
                        }
                        Proxy(proxyType, InetSocketAddress(host, port))
                    } catch (e: Throwable) {
                        FileLog.e("WebProxyManager: invalid custom proxy address", e)
                        Proxy.NO_PROXY
                    }
                } else {
                    Proxy.NO_PROXY
                }
            }
            else -> Proxy.NO_PROXY
        }
    }

    @JvmStatic
    fun getEffectiveProxyCredentials(): Pair<String, String>? {
        return when (NyaConfig.webProxyMode.Int()) {
            NyaConfig.WEB_PROXY_MODE_FOLLOW_TELEGRAM -> {
                if (SharedConfig.isProxyEnabled() && SharedConfig.currentProxy != null) {
                    val info = SharedConfig.currentProxy
                    if (info.settings.type == ProxySettings.Type.SOCKS5 && !info.username.isNullOrEmpty()) {
                        Pair(info.username ?: "", info.password ?: "")
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            NyaConfig.WEB_PROXY_MODE_CUSTOM -> {
                val user = NyaConfig.webProxyUsername.String()?.trim() ?: ""
                val pass = NyaConfig.webProxyPassword.String()?.trim() ?: ""
                if (user.isNotEmpty()) {
                    Pair(user, pass)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    @JvmStatic
    fun onProxyConfigChanged() {
        HttpClient.evictAll()
    }

    @JvmStatic
    fun createDynamicProxySelector(): ProxySelector {
        ensureJavaAuthenticator()
        return object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> {
                val mode = NyaConfig.webProxyMode.Int()
                if (mode == NyaConfig.WEB_PROXY_MODE_DIRECT) {
                    return listOf(Proxy.NO_PROXY)
                }
                val proxy = getEffectiveProxy()
                if (proxy != Proxy.NO_PROXY) {
                    return listOf(proxy)
                }
                if (mode == NyaConfig.WEB_PROXY_MODE_FOLLOW_TELEGRAM && uri != null) {
                    val systemProxies = try {
                        systemProxySelector?.select(uri)
                    } catch (_: Throwable) {
                        null
                    }
                    if (!systemProxies.isNullOrEmpty()) {
                        return systemProxies
                    }
                }
                return listOf(Proxy.NO_PROXY)
            }

            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                FileLog.e("WebProxyManager: proxy connection failed for URI: $uri, target: $sa", ioe)
                if (getEffectiveProxy() == Proxy.NO_PROXY) {
                    try {
                        systemProxySelector?.connectFailed(uri, sa, ioe)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    @JvmStatic
    val okHttpProxyAuthenticator: OkHttpAuthenticator = OkHttpAuthenticator { _: Route?, response: Response ->
        if (response.code == 407) {
            val creds = getEffectiveProxyCredentials()
            if (creds != null && creds.first.isNotEmpty()) {
                val credentialHeader = Credentials.basic(creds.first, creds.second)
                return@OkHttpAuthenticator response.request.newBuilder()
                    .header("Proxy-Authorization", credentialHeader)
                    .build()
            }
        }
        null
    }
}
