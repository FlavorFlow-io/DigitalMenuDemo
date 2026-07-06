package dev.lssoftware.digitalmenu.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tiny read-only client for the Cloud Firestore REST API.
 *
 * Deliberately uses no Firebase SDK and no `google-services.json`: that file binds
 * to specific package names, which would fight the white-label model where every
 * client ships a different applicationId. A single Firebase project serves all of
 * them, and public menu data is read via plain HTTPS subject to security rules.
 */
class FirestoreClient(
    private val projectId: String,
    private val apiKey: String = "",
) {

    /** Fetches every document in [collectionPath] (following pagination). */
    suspend fun listDocuments(collectionPath: String): List<FirestoreDocument> =
        withContext(Dispatchers.IO) {
            val docs = mutableListOf<FirestoreDocument>()
            var pageToken: String? = null
            do {
                val json = JSONObject(get(buildUrl(collectionPath, pageToken)))
                json.optJSONArray("documents")?.let { array ->
                    for (i in 0 until array.length()) {
                        val doc = array.getJSONObject(i)
                        val id = doc.getString("name").substringAfterLast('/')
                        val fields = doc.optJSONObject("fields") ?: JSONObject()
                        docs += FirestoreDocument(id, fields)
                    }
                }
                pageToken = json.optString("nextPageToken").ifEmpty { null }
            } while (pageToken != null)
            docs
        }

    private fun buildUrl(collectionPath: String, pageToken: String?): String =
        buildString {
            append(BASE)
            append("/projects/").append(projectId)
            append("/databases/(default)/documents/").append(collectionPath)
            append("?pageSize=300")
            if (apiKey.isNotEmpty()) append("&key=").append(apiKey)
            if (pageToken != null) append("&pageToken=").append(pageToken)
        }

    private fun get(urlString: String): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("Firestore GET failed: HTTP $code $err")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val BASE = "https://firestore.googleapis.com/v1"
        const val TIMEOUT_MS = 10_000
    }
}

/**
 * One Firestore document: its id plus the raw `fields` object. Firestore encodes
 * every value with its type (`{"stringValue": "..."}`), so accessors unwrap them.
 */
class FirestoreDocument(val id: String, private val fields: JSONObject) {

    fun string(key: String): String =
        fields.optJSONObject(key)?.optString("stringValue").orEmpty()

    fun double(key: String): Double {
        val field = fields.optJSONObject(key) ?: return 0.0
        return when {
            field.has("doubleValue") -> field.optDouble("doubleValue")
            field.has("integerValue") -> field.optString("integerValue").toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    fun int(key: String): Int = double(key).toInt()
}
