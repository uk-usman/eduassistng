package ng.eduassist.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.eduassist.app.data.ApiException
import ng.eduassist.app.data.Step

val Navy = Color(0xFF0B2A5B)
val NavyDark = Color(0xFF071D41)
val Gold = Color(0xFFC9A227)
val Teal = Color(0xFF0E7C86)
val Bg = Color(0xFFF3F5F9)
val Muted = Color(0xFF5B6577)
val Line = Color(0xFFE6EAF2)
val Green = Color(0xFF1F9D5B)

@Composable
fun EduTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Navy, onPrimary = Color.White, secondary = Gold, onSecondary = Color(0xFF1A1405), background = Bg, surface = Color.White, error = Color(0xFFD32F2F)),
        content = content,
    )
}

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = Color.White)
    val shape = RoundedCornerShape(16.dp)
    val border = BorderStroke(1.dp, Line)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) { Column(Modifier.padding(16.dp), content = content) }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) { Column(Modifier.padding(16.dp), content = content) }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, gold: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(14.dp),
        colors = if (gold) ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1405)) else ButtonDefaults.buttonColors(containerColor = Navy),
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Navy)) {
        Text(text, color = Navy, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Navy)
        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 14.sp)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Navy, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
fun StatusChip(status: String, label: String) {
    val (bg, fg) = when (status) {
        "completed", "paid", "successful" -> Color(0xFFDDF3E7) to Color(0xFF14663D)
        "awaiting_payment", "submitted", "draft", "awaiting_user_info", "pending" -> Color(0xFFFFF0C2) to Color(0xFF7A5C00)
        "cancelled", "rejected", "failed", "refunded" -> Color(0xFFFBE0E0) to Color(0xFF9B1C1C)
        else -> Color(0xFFDCE8FB) to Color(0xFF1D4FA8)
    }
    Text(label.ifBlank { status.replace('_', ' ').replaceFirstChar { it.uppercase() } }, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
fun StepsRow(steps: List<Step>) {
    if (steps.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        steps.forEach { s ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val done = s.state == "done"
                val cur = s.state == "current"
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(if (done) Green else if (cur) Color.White else Color(0xFFDDE3EE))
                        .then(if (cur) Modifier.background(Color.White) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (cur) Box(Modifier.size(14.dp).clip(CircleShape).background(Navy))
                }
                Text(s.label, fontSize = 11.sp, color = if (s.state == "todo") Muted else Color(0xFF1C2433), fontWeight = if (s.state == "todo") FontWeight.Normal else FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ErrorBox(message: String?) {
    if (message.isNullOrBlank()) return
    Text(message, color = Color(0xFF9B1C1C), fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFDECEC)).padding(12.dp))
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Navy) }
}

@Composable
fun EmptyState(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontWeight = FontWeight.ExtraBold, color = Navy, fontSize = 17.sp)
        Text(text, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Result of a screen load. */
class Load<T>(val value: T?, val error: String?, val loading: Boolean)

/** Runs [block] when the screen opens (and again when any key changes, e.g. after returning from payment). */
@Composable
fun <T> load(vararg keys: Any?, block: suspend () -> T): State<Load<T>> =
    produceState(Load<T>(null, null, true), *keys) {
        value = Load(value.value, null, true)
        value = try {
            Load(block(), null, false)
        } catch (e: ApiException) {
            if (e.status == 401) App.loggedIn = false
            Load(null, e.message, false)
        }
    }
