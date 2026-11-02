package ng.eduassist.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** org.json returns the text "null" for JSON null; these helpers give clean values. */
fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")
fun JSONObject.dbl(key: String): Double = if (isNull(key)) 0.0 else optDouble(key, 0.0)
fun JSONObject.bool(key: String): Boolean = !isNull(key) && optBoolean(key, false)
fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

fun naira(amount: Double): String = "₦" + String.format(Locale.US, "%,.2f", amount)

data class Step(val label: String, val state: String)

data class RequestItem(
    val number: String, val status: String, val statusLabel: String, val service: String, val institution: String,
    val total: Double, val payable: Boolean, val steps: List<Step>, val updatedAt: String, val raw: JSONObject,
) {
    val isDone: Boolean get() = status in listOf("completed", "cancelled", "rejected", "refunded")
}

fun JSONObject.toRequestItem() = RequestItem(
    number = str("number"), status = str("status"), statusLabel = str("status_label"), service = str("service"), institution = str("institution"),
    total = dbl("total"), payable = bool("payable"),
    steps = optJSONArray("steps")?.objects()?.map { Step(it.str("label"), it.str("state")) } ?: emptyList(),
    updatedAt = str("updated_at"), raw = this,
)

data class PaymentItem(val reference: String, val request: String, val amount: Double, val status: String, val receipt: String, val date: String)

fun JSONObject.toPayment() = PaymentItem(str("order_reference"), str("request_number"), dbl("amount"), str("status"), str("receipt_number"), str("created_at"))

data class Notice(val id: Int, val title: String, val body: String, val read: Boolean, val date: String)

fun JSONObject.toNotice() = Notice(optInt("id"), str("title"), str("body"), bool("read"), str("created_at"))

data class CatalogService(val slug: String, val name: String, val description: String, val verifiedOnly: Boolean)
data class CatalogInstitution(val slug: String, val name: String, val acronym: String, val state: String, val services: List<CatalogService>)

fun JSONObject.toInstitution() = CatalogInstitution(
    str("slug"), str("name"), str("acronym"), str("state"),
    optJSONArray("services")?.objects()?.map { CatalogService(it.str("slug"), it.str("name"), it.str("description"), it.bool("verified_students_only")) } ?: emptyList(),
)

data class FormField(val key: String, val label: String, val type: String, val options: List<String>, val required: Boolean)

fun JSONObject.toField() = FormField(
    str("key"), str("label"), str("type"),
    str("options").split(",").map { it.trim() }.filter { it.isNotEmpty() },
    optInt("required", 0) == 1 || optBoolean("required", false),
)

data class User(val name: String, val email: String, val phone: String, val accountType: String, val state: String, val referralLink: String, val unread: Int, val active: Int)

fun JSONObject.toUser() = User(
    str("full_name"), str("email"), str("phone"), str("account_type"), str("state"), str("referral_link"),
    optInt("unread_notifications", 0), optInt("active_requests", 0),
)
