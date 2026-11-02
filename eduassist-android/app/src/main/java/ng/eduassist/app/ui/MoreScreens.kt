package ng.eduassist.app.ui

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ng.eduassist.app.data.*
import org.json.JSONObject
import java.util.UUID

@Composable
fun ProfileScreen() {
    val me by load(App.resumeTick) { Api.data("GET", "/me").toUser() }
    val u = me.value ?: App.user
    var phone by remember(u?.phone) { mutableStateOf(u?.phone ?: "") }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    ScreenTitle("Profile")
    ErrorBox(me.error)
    if (u == null) { Loading(); return }
    AppCard {
        Text(u.name, fontWeight = FontWeight.ExtraBold, color = Navy, style = MaterialTheme.typography.titleLarge)
        Text(u.email, color = Muted)
        Text(if (u.accountType == "existing_student") "Existing student" else "Aspirant", color = Muted)
        Field(phone, { phone = it }, "Phone number", keyboard = KeyboardType.Phone)
        msg?.let { Text(it, color = Green) }
        SecondaryButton("Save phone number") {
            scope.launch { try { Api.data("PATCH", "/me", JSONObject().put("phone", phone.trim())); msg = "Saved." } catch (e: ApiException) { msg = e.message } }
        }
    }
    Spacer(Modifier.height(12.dp))
    AppCard(onClick = { App.push(Route.Notifications) }) { Text("Notifications (${u.unread} unread)", fontWeight = FontWeight.Bold, color = Navy) }
    Spacer(Modifier.height(8.dp))
    AppCard(onClick = { App.push(Route.Tickets) }) { Text("Help & support", fontWeight = FontWeight.Bold, color = Navy) }
    if (u.referralLink.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        AppCard(onClick = {
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Get admission help on EduAssist NG: ${u.referralLink}"), "Share"))
        }) { Text("Share your referral link", fontWeight = FontWeight.Bold, color = Navy); Text(u.referralLink, color = Muted) }
    }
    Spacer(Modifier.height(16.dp))
    SecondaryButton("Sign out") {
        scope.launch { ng.eduassist.app.push.PushRegistrar.unregister(); try { Api.call("POST", "/logout") } catch (_: ApiException) {}; App.signedOut() }
    }
}

@Composable
fun TicketsScreen() {
    val items by load(App.resumeTick) { Api.list("/tickets").objects() }
    BackRow("Help & support")
    PrimaryButton("New support ticket") { App.push(Route.NewTicket) }
    Spacer(Modifier.height(12.dp))
    ErrorBox(items.error)
    if (items.loading && items.value == null) Loading()
    items.value?.let { list ->
        if (list.isEmpty()) EmptyState("No tickets", "Need help? Open a ticket and we will reply.")
        list.forEach { t ->
            AppCard(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(t.str("subject"), fontWeight = FontWeight.Bold, color = Navy)
                Text("${t.str("reference")} · ${t.str("status")}", color = Muted)
            }
        }
    }
}

@Composable
fun NewTicketScreen() {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    BackRow("New ticket")
    AppCard {
        ErrorBox(error)
        Field(subject, { subject = it }, "Subject")
        Field(message, { message = it }, "How can we help? (at least 10 characters)", minLines = 5)
        Spacer(Modifier.height(8.dp))
        PrimaryButton(if (busy) "Sending…" else "Send", enabled = !busy && subject.isNotBlank() && message.length >= 10) {
            busy = true; error = null
            scope.launch {
                try { Api.call("POST", "/tickets", JSONObject().put("subject", subject.trim()).put("message", message.trim())); App.pop() }
                catch (e: ApiException) { error = e.message }
                busy = false
            }
        }
    }
}

fun openCheckout(ctx: android.content.Context, url: String) {
    CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(ctx, Uri.parse(url))
}

@Composable
fun RequestDetailScreen(number: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var paying by remember { mutableStateOf(false) }
    var waitingPayment by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val req by load(number, App.resumeTick, refresh) { Api.data("GET", "/requests/$number") }

    // Back from the payment page: ask the server to confirm the payment.
    LaunchedEffect(App.resumeTick) {
        if (waitingPayment) {
            try {
                val d = Api.data("POST", "/requests/$number/verify-payment")
                note = d.str("message").ifBlank { null }
                waitingPayment = false
                refresh++
            } catch (e: ApiException) { error = e.message }
        }
    }

    BackRow("Request $number")
    ErrorBox(error ?: req.error)
    note?.let { Text(it, color = Green, modifier = Modifier.padding(bottom = 8.dp)) }
    if (req.loading && req.value == null) Loading()
    val d = req.value ?: return
    val item = d.toRequestItem()
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.service, fontWeight = FontWeight.ExtraBold, color = Navy, modifier = Modifier.weight(1f))
            StatusChip(item.status, item.statusLabel)
        }
        Text(item.institution, color = Muted)
        StepsRow(item.steps)
        Text(naira(item.total), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp))
    }
    if (item.payable) {
        Spacer(Modifier.height(12.dp))
        PrimaryButton(if (paying) "Opening payment…" else "Pay ${naira(item.total)}", enabled = !paying, gold = true) {
            paying = true; error = null
            scope.launch {
                try {
                    val r = Api.data("POST", "/requests/$number/pay", JSONObject())
                    waitingPayment = true
                    openCheckout(ctx, r.getString("checkout_url"))
                } catch (e: ApiException) { error = e.message }
                paying = false
            }
        }
    }
    val summary = d.str("completion_summary")
    if (summary.isNotBlank()) { SectionTitle("Result"); AppCard { Text(summary) } }
    val details = d.optJSONArray("details")?.objects().orEmpty()
    if (details.isNotEmpty()) {
        SectionTitle("Your details")
        AppCard { details.forEach { Text(it.str("label"), color = Muted); Text(it.str("value"), modifier = Modifier.padding(bottom = 6.dp)) } }
    }
    if (item.status !in listOf("cancelled", "refunded", "rejected")) DocumentsSection(number)
    val tl = d.optJSONArray("timeline")?.objects().orEmpty()
    if (tl.isNotEmpty()) {
        SectionTitle("Timeline")
        AppCard { tl.forEach { Text(it.str("status").replace('_', ' ') + " · " + it.str("created_at"), fontWeight = FontWeight.SemiBold); if (it.str("note").isNotBlank()) Text(it.str("note"), color = Muted); Spacer(Modifier.height(6.dp)) } }
    }
}

@Composable
fun NewRequestScreen() {
    val scope = rememberCoroutineScope()
    val catalog by load { Api.list("/catalog").objects().map { it.toInstitution() } }
    var inst by remember { mutableStateOf<CatalogInstitution?>(null) }
    var svc by remember { mutableStateOf<CatalogService?>(null) }
    var form by remember { mutableStateOf<JSONObject?>(null) }
    var values by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var sessionId by remember { mutableStateOf<Int?>(null) }
    var agree by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fieldErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var idem by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var query by remember { mutableStateOf("") }

    BackRow("New request")
    ErrorBox(error ?: catalog.error)

    // Step 1: institution
    if (inst == null) {
        Field(query, { query = it }, "Search institution")
        if (catalog.loading && catalog.value == null) Loading()
        catalog.value?.filter { query.isBlank() || it.name.contains(query, true) || it.acronym.contains(query, true) }?.forEach { i ->
            AppCard(onClick = { inst = i }, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(i.name, fontWeight = FontWeight.Bold, color = Navy); Text("${i.acronym} · ${i.state}", color = Muted)
            }
        }
        return
    }
    val institution = inst!!
    // Step 2: service
    if (svc == null) {
        Text(institution.name, color = Muted, modifier = Modifier.padding(bottom = 8.dp))
        if (institution.services.isEmpty()) EmptyState("No services yet", "This institution has no services available right now.")
        institution.services.forEach { s ->
            AppCard(onClick = {
                svc = s; error = null
                scope.launch {
                    try { form = Api.data("GET", "/services/${s.slug}/form?institution=${institution.slug}") } catch (e: ApiException) { error = e.message; svc = null }
                }
            }, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(s.name, fontWeight = FontWeight.Bold, color = Navy); Text(s.description, color = Muted)
                if (s.verifiedOnly) Text("Verified students only", color = Gold)
            }
        }
        TextButton(onClick = { inst = null }) { Text("Change institution", color = Navy) }
        return
    }
    // Step 3: form
    val f = form
    if (f == null) { Loading(); return }
    val service = svc!!
    val fields = f.optJSONArray("fields")?.objects()?.map { it.toField() }.orEmpty()
    val sessions = f.optJSONArray("sessions")?.objects().orEmpty()
    val quote = f.optJSONObject("quote")
    AppCard {
        Text(service.name, fontWeight = FontWeight.ExtraBold, color = Navy)
        Text(institution.name, color = Muted)
        if (quote != null) Text(naira(quote.dbl("total")), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp))
        f.optString("quote_error").takeIf { it.isNotBlank() && it != "null" }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!f.bool("eligible")) Text("This service needs an Existing Student account with approved verification.", color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(10.dp))
    if (sessions.isNotEmpty()) {
        Text("Admission session", color = Muted)
        sessions.forEach { s ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(sessionId == s.optInt("id"), { sessionId = s.optInt("id") }); Text(s.str("label"))
            }
        }
    }
    fields.forEach { fld ->
        val label = fld.label + if (fld.required) " *" else ""
        val v = values[fld.key].orEmpty()
        if (fld.type == "select" && fld.options.isNotEmpty()) {
            Text(label, color = Muted, modifier = Modifier.padding(top = 6.dp))
            fld.options.forEach { o -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { RadioButton(v == o, { values = values + (fld.key to o) }); Text(o) } }
            fieldErrors[fld.key]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } else {
            Field(v, { values = values + (fld.key to it) }, label, fieldErrors[fld.key],
                if (fld.type == "number") KeyboardType.Number else if (fld.type == "email") KeyboardType.Email else KeyboardType.Text, minLines = if (fld.type == "textarea") 3 else 1)
        }
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(agree, { agree = it })
        Text("I confirm my information is accurate and accept the service terms.", modifier = Modifier.weight(1f))
    }
    fieldErrors["confirm_terms"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Spacer(Modifier.height(8.dp))
    PrimaryButton(if (busy) "Submitting…" else "Submit request", enabled = !busy && f.bool("eligible") && quote != null) {
        busy = true; error = null; fieldErrors = emptyMap()
        scope.launch {
            try {
                val body = JSONObject().put("institution", institution.slug).put("service", service.slug).put("confirm_terms", agree)
                    .put("fields", JSONObject(values as Map<*, *>))
                sessionId?.let { body.put("session_id", it) }
                val created = Api.data("POST", "/requests", body, idem)
                idem = UUID.randomUUID().toString()
                App.pop(); App.push(Route.Detail(created.str("number")))
            } catch (e: ApiException) { error = e.message; fieldErrors = e.fields }
            busy = false
        }
    }
    TextButton(onClick = { svc = null; form = null }) { Text("Choose another service", color = Navy) }
}

@Composable
fun DocumentsSection(number: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var activeKey by remember { mutableStateOf<String?>(null) }
    var busyKey by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val docs by load(number, refresh) { Api.data("GET", "/requests/$number/documents") }

    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: Uri? ->
        val key = activeKey
        if (uri == null || key == null) return@rememberLauncherForActivityResult
        busyKey = key; err = null; msg = null
        scope.launch {
            try {
                val (name, mime, bytes) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val cr = ctx.contentResolver
                    var n = "document"
                    cr.query(uri, null, null, null, null)?.use { c ->
                        val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (c.moveToFirst() && i >= 0) n = c.getString(i) ?: n
                    }
                    val data = cr.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    Triple(n, cr.getType(uri) ?: "application/octet-stream", data)
                }
                val maxMb = docs.value?.optInt("max_mb", 5) ?: 5
                if (bytes.isEmpty()) throw ApiException("empty", "That file could not be read.")
                if (bytes.size > maxMb * 1024 * 1024) throw ApiException("too_big", "File is too large. Maximum is $maxMb MB.")
                Api.upload("/requests/$number/documents", mapOf("doc_key" to key), name, mime, bytes)
                msg = "Uploaded."
                refresh++
            } catch (e: ApiException) { err = e.message }
            busyKey = null
        }
    }

    SectionTitle("Documents")
    ErrorBox(err ?: docs.error)
    msg?.let { Text(it, color = Green, modifier = Modifier.padding(bottom = 6.dp)) }
    if (docs.loading && docs.value == null) { Loading(); return }
    val d = docs.value ?: return
    val slots = d.optJSONArray("slots")?.objects().orEmpty()
    val files = d.optJSONArray("files")?.objects().orEmpty()
    if (slots.isEmpty()) { AppCard { Text("No documents are needed for this service.", color = Muted) }; return }
    slots.forEach { slot ->
        val key = slot.str("key")
        val mine = files.filter { it.str("key") == key }
        AppCard(modifier = Modifier.padding(bottom = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(slot.str("label") + if (slot.bool("required")) " *" else "", fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.weight(1f))
                if (mine.isNotEmpty()) Text("✓ Uploaded", color = Green, fontWeight = FontWeight.SemiBold)
            }
            mine.forEach { Text(it.str("name"), color = Muted) }
            Spacer(Modifier.height(8.dp))
            SecondaryButton(if (busyKey == key) "Uploading…" else if (mine.isEmpty()) "Choose file (PDF, JPG, PNG)" else "Upload another") {
                if (busyKey == null) { activeKey = key; picker.launch("*/*") }
            }
        }
    }
    Text("Maximum ${d.optInt("max_mb", 5)} MB per file.", color = Muted)
}
