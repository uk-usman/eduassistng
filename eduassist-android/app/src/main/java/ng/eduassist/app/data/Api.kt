package ng.eduassist.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ng.eduassist.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Error returned by the EduAssist API (or a network problem). */
class ApiException(
    val code: String,
    message: String,
    val status: Int = 0,
    val fields: Map<String, String> = emptyMap(),
) : Exception(message)

/** Remembers the login token on this phone (app-private storage). Tokens do not expire unless your admin sets a lifetime. */
object Session {
    private const val FILE = "eduassist_session"
    private lateinit var appContext: Context
    var token: String? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        token = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("token", null)
    }

    fun save(newToken: String) {
        token = newToken
        appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("token", newToken).apply()
    }

    fun clear() {
        token = null
        appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

object Api {
    val baseUrl: String get() = BuildConfig.BASE_URL.trimEnd('/')
    private const val PREFIX = "/api/v1/customer"
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    /** Calls the API and returns the full JSON response (use ["data"]). Throws [ApiException]. */
    suspend fun call(method: String, path: String, body: JSONObject? = null, idempotencyKey: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(baseUrl + PREFIX + path).header("Accept", "application/json")
            Session.token?.let { builder.header("Authorization", "Bearer $it") }
            idempotencyKey?.let { builder.header("Idempotency-Key", it) }
            val payload = (body ?: JSONObject()).toString().toRequestBody(JSON)
            when (method) {
                "GET" -> builder.get()
                "POST" -> builder.post(payload)
                "PATCH" -> builder.patch(payload)
                else -> builder.method(method, payload)
            }
            try {
                client.newCall(builder.build()).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    val json = try { JSONObject(text) } catch (e: Exception) { JSONObject() }
                    if (!resp.isSuccessful) {
                        val err = json.optJSONObject("error")
                        val fields = mutableMapOf<String, String>()
                        err?.optJSONObject("fields")?.let { f -> f.keys().forEach { k -> fields[k] = f.optString(k) } }
                        if (resp.code == 401 && Session.token != null) Session.clear()
                        throw ApiException(
                            err?.optString("code") ?: "http_${resp.code}",
                            err?.optString("message")?.takeIf { it.isNotBlank() } ?: "Something went wrong (${resp.code}). Please try again.",
                            resp.code, fields,
                        )
                    }
                    json
                }
            } catch (e: ApiException) {
                throw e
            } catch (e: Exception) {
                throw ApiException("network", "Cannot reach the server. Check your internet connection and try again.")
            }
        }

    suspend fun data(method: String, path: String, body: JSONObject? = null, idem: String? = null): JSONObject =
        call(method, path, body, idem).optJSONObject("data") ?: JSONObject()

    suspend fun list(path: String): JSONArray = call("GET", path).optJSONArray("data") ?: JSONArray()

    /** Uploads a file (multipart) to a customer endpoint. */
    suspend fun upload(path: String, fields: Map<String, String>, fileName: String, mime: String, bytes: ByteArray): JSONObject =
        withContext(Dispatchers.IO) {
            val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            fields.forEach { (k, v) -> form.addFormDataPart(k, v) }
            form.addFormDataPart("file", fileName, bytes.toRequestBody(mime.toMediaType()))
            val builder = Request.Builder().url(baseUrl + PREFIX + path).header("Accept", "application/json").post(form.build())
            Session.token?.let { builder.header("Authorization", "Bearer $it") }
            try {
                client.newCall(builder.build()).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    val json = try { JSONObject(text) } catch (e: Exception) { JSONObject() }
                    if (!resp.isSuccessful) {
                        val err = json.optJSONObject("error")
                        throw ApiException(err?.optString("code") ?: "http_${resp.code}", err?.optString("message")?.takeIf { it.isNotBlank() } ?: "Upload failed (${resp.code}).", resp.code)
                    }
                    json
                }
            } catch (e: ApiException) {
                throw e
            } catch (e: Exception) {
                throw ApiException("network", "Upload failed. Check your internet connection and try again.")
            }
        }
}
