package top.nkbe.niagram.translate.source

import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.TranslateAlert2
import top.nkbe.niagram.config.NyaConfig
import top.nkbe.niagram.translate.HTMLKeeper
import top.nkbe.niagram.translate.Translator
import top.nkbe.niagram.utils.HttpClient
import java.io.IOException
import java.util.Locale

object DeepLXTranslator : Translator {

    private val httpClient = HttpClient.instance

    override suspend fun doTranslate(
        from: String, to: String, query: String, entities: ArrayList<TLRPC.MessageEntity>
    ): TLRPC.TL_textWithEntities {
        if (to.isEmpty()) {
            throw UnsupportedOperationException(getString(R.string.TranslateApiUnsupported) + " " + to)
        }

        val endpoint = NyaConfig.deepLxCustomApi.String().trim()
        if (endpoint.isEmpty()) error("Missing DeepLX API URL")

        val originalText = TLRPC.TL_textWithEntities().apply {
            this.text = query
            this.entities = entities
        }

        val textToTranslate = if (entities.isNotEmpty()) HTMLKeeper.entitiesToHtml(
            query, entities, false
        ) else query

        val payload = JSONObject().apply {
            put("text", textToTranslate)
            put("source_lang", if (from.isEmpty() || from == "auto") "auto" else from.uppercase(Locale.ROOT))
            put(
                "target_lang", when (to.lowercase(Locale.ROOT)) {
                    "zh", "zh-cn", "zh-hans" -> "ZH-HANS"
                    "zh-tw", "zh-hk", "zh-hant" -> "ZH-HANT"
                    else -> to.uppercase(Locale.ROOT)
                }
            )
            if (entities.isNotEmpty()) put("tag_handling", "html")
        }.toString()

        val request = Request.Builder()
            .url(endpoint)
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
            error("DeepLX request failed due to network issue: ${e.message}")
        }

        val translated = try {
            val json = JSONObject(responseString)
            if (json.has("error")) error(json.optString("message", json.getString("error")))
            json.getString("data")
        } catch (e: JSONException) {
            error("DeepLX response parsing failed: ${e.message}")
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
