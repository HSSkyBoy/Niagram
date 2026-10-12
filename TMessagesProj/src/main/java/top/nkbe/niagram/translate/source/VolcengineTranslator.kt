package top.nkbe.niagram.translate.source

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.TranslateAlert2
import top.nkbe.niagram.translate.HTMLKeeper
import top.nkbe.niagram.translate.Translator
import top.nkbe.niagram.utils.HttpClient
import java.io.IOException

object VolcengineTranslator : Translator {

    private val httpClient = HttpClient.instance

    private val targetLanguages = listOf(
        "ar", "as", "bn", "bs", "bg", "yue", "ca", "zh", "zh-Hans", "zh-Hant",
        "hr", "cs", "da", "prs", "nl", "en", "et", "fj", "fil", "fi",
        "fr", "de", "el", "gu", "ht", "he", "hi", "mww", "hu", "is",
        "id", "ga", "it", "ja", "kn", "kk", "tlh", "ko", "ku", "kmr",
        "lv", "lt", "mg", "ms", "ml", "mt", "mi", "mr", "nb", "or", "ps",
        "fa", "pl", "pt", "pa", "otq", "ro", "ru", "sm", "sr", "sk", "sl",
        "es", "sw", "sv", "ty", "ta", "te", "th", "to", "tr", "uk", "ur",
        "vi", "cy", "yua"
    )

    override suspend fun doTranslate(
        from: String, to: String, query: String, entities: ArrayList<TLRPC.MessageEntity>
    ): TLRPC.TL_textWithEntities {
        val targetLang = when {
            to.equals("zh-CN", ignoreCase = true) -> "zh-Hans"
            to.equals("zh-TW", ignoreCase = true) || to.equals("zh-HK", ignoreCase = true) -> "zh-Hant"
            else -> to
        }
        if (targetLang !in targetLanguages) {
            throw UnsupportedOperationException(getString(R.string.TranslateApiUnsupported) + " " + to)
        }

        val originalText = TLRPC.TL_textWithEntities().apply {
            this.text = query
            this.entities = entities
        }

        val textToTranslate = if (entities.isNotEmpty()) HTMLKeeper.entitiesToHtml(
            query, entities, false
        ) else query

        val payload = JSONObject().apply {
            put("target_language", targetLang)
            put("text", textToTranslate)
        }.toString()

        val request = Request.Builder()
            .url("https://translate.volcengine.com/crx/translate/v1/")
            .header("Accept", "application/json, text/plain, */*")
            .header("Origin", "chrome-extension://klgfhbiooeogfpknjdcbablpceialkdj")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-Mode", "cors")
            .header("Sec-Fetch-Dest", "empty")
            .post(payload.toRequestBody(HttpClient.MEDIA_TYPE_JSON))
            .build()

        val responseString = try {
            httpClient.newCall(request).await().use { response ->
                val bodyString = response.body.string()
                if (!response.isSuccessful) {
                    error("HTTP ${response.code} : $bodyString")
                }
                bodyString
            }
        } catch (e: IOException) {
            error("Volcengine request failed due to network issue: ${e.message}")
        }

        val translated = try {
            JSONObject(responseString).getString("translation")
        } catch (e: JSONException) {
            error("Volcengine response parsing failed: ${e.message}")
        }
        if (translated.isEmpty()) error("Empty translation result")

        var finalText = TLRPC.TL_textWithEntities()
        if (entities.isNotEmpty()) {
            val resultPair = HTMLKeeper.htmlToEntities(translated, entities, false)
            finalText.text = resultPair.first
            finalText.entities = resultPair.second
            finalText = TranslateAlert2.preprocess(originalText, finalText)
        } else {
            finalText.text = translated
        }
        return finalText
    }
}
