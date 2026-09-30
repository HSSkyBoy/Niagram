/*
 * This is the source code of NiagramX for Android.
 * It is licensed under GNU GPL v2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 */

package top.nkbe.niagram.utils

import android.net.Uri
import top.nkbe.niagram.config.NyaConfig

object UrlCleaner {

    private val GLOBAL_TRACKING_PARAMS = setOf(
        // Google / Analytics / UTM
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "utm_id",
        "utm_name",
        "utm_cid",
        "utm_reader",
        "utm_viz_id",
        "utm_pubreferrer",
        "utm_swu",
        "gclid",
        "gclsrc",
        "dclid",
        "gad_source",
        "gbraid",
        "wbraid",
        // Facebook / Meta / Instagram
        "fbclid",
        "igsh",
        "igshid",
        "fref",
        // Microsoft / Bing
        "msclkid",
        "ocid",
        // Marketing / Newsletter
        "mc_cid",
        "mc_eid",
        "_hsenc",
        "_hsmi",
        "mkt_tok",
        "vero_id",
        "vero_conv",
        "nr_email_referer",
        // Other analytics
        "yclid",
        "_openstat"
    )

    private val TWITTER_HOSTS = setOf("twitter.com", "x.com", "mobile.twitter.com")
    private val YOUTUBE_HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be")
    private val BILIBILI_HOSTS = setOf("bilibili.com", "www.bilibili.com", "m.bilibili.com", "b23.tv")
    private val TIKTOK_HOSTS = setOf("tiktok.com", "www.tiktok.com", "m.tiktok.com", "douyin.com", "www.douyin.com")
    private val REDDIT_HOSTS = setOf("reddit.com", "www.reddit.com")
    private val SPOTIFY_HOSTS = setOf("spotify.com", "open.spotify.com")
    private val TAOBAO_HOSTS = setOf("taobao.com", "tmall.com", "jd.com", "aliexpress.com")

    @JvmStatic
    fun clean(url: String?): String {
        if (url.isNullOrBlank()) return url ?: ""
        if (!NyaConfig.cleanTrackingParams.Bool()) return url
        val trimmed = url.trim()
        val lower = trimmed.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return url
        }
        return try {
            val uri = Uri.parse(trimmed)
            clean(uri).toString()
        } catch (_: Throwable) {
            url
        }
    }

    @JvmStatic
    fun clean(uri: Uri?): Uri {
        if (uri == null) return Uri.EMPTY
        if (!NyaConfig.cleanTrackingParams.Bool()) return uri
        val scheme = uri.scheme?.lowercase() ?: return uri
        if (scheme != "http" && scheme != "https") return uri
        if (uri.query.isNullOrEmpty()) return uri

        return try {
            val host = uri.host?.lowercase() ?: ""
            val queryParamNames = uri.queryParameterNames ?: return uri

            var hasTrackingParam = false
            for (param in queryParamNames) {
                if (isTrackingParam(host, param)) {
                    hasTrackingParam = true
                    break
                }
            }

            if (!hasTrackingParam) {
                return uri
            }

            val builder = uri.buildUpon().clearQuery()
            for (param in queryParamNames) {
                if (!isTrackingParam(host, param)) {
                    for (value in uri.getQueryParameters(param)) {
                        builder.appendQueryParameter(param, value)
                    }
                }
            }
            builder.build()
        } catch (_: Throwable) {
            uri
        }
    }

    private fun isTrackingParam(host: String, param: String): Boolean {
        val lowerParam = param.lowercase()
        if (GLOBAL_TRACKING_PARAMS.contains(lowerParam)) {
            return true
        }
        if (lowerParam.startsWith("utm_")) {
            return true
        }

        // Host specific
        if (matchesHost(host, TWITTER_HOSTS)) {
            if (lowerParam in listOf("s", "t", "ref_src", "ref_url")) {
                return true
            }
        } else if (matchesHost(host, YOUTUBE_HOSTS)) {
            if (lowerParam in listOf("si", "feature", "pp")) {
                return true
            }
        } else if (matchesHost(host, BILIBILI_HOSTS)) {
            if (lowerParam in listOf("spm_id_from", "from_source", "from", "share_source", "share_medium", "share_plat", "share_tag", "share_session_id", "bbid", "ts", "vd_source")) {
                return true
            }
        } else if (matchesHost(host, TIKTOK_HOSTS)) {
            if (lowerParam in listOf("is_from_webapp", "sender_device", "tt_from", "_r", "_d", "checksum")) {
                return true
            }
        } else if (matchesHost(host, REDDIT_HOSTS)) {
            if (lowerParam in listOf("ref", "ref_source", "share_id")) {
                return true
            }
        } else if (matchesHost(host, SPOTIFY_HOSTS)) {
            if (lowerParam in listOf("si", "context")) {
                return true
            }
        } else if (matchesHost(host, TAOBAO_HOSTS)) {
            if (lowerParam in listOf("spm", "scm", "pvid", "trackid")) {
                return true
            }
        }

        return false
    }

    private fun matchesHost(host: String, targetHosts: Set<String>): Boolean {
        for (target in targetHosts) {
            if (host == target || host.endsWith(".$target")) {
                return true
            }
        }
        return false
    }
}
