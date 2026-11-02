package ng.eduassist.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ng.eduassist.app.data.*

private data class Tab(val label: String, val icon: String)

@Composable
fun MainHost() {
    val route = App.stack.lastOrNull()
    val tabs = listOf(Tab("Home", "⌂"), Tab("Requests", "☰"), Tab("Payments", "₦"), Tab("Profile", "☺"))
    Scaffold(
        containerColor = Bg,
        bottomBar = {
            if (route == null) NavigationBar(containerColor = Color.White) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = App.tab == i, onClick = { App.tab = i },
                        icon = { Text(t.icon) }, label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Navy, selectedTextColor = Navy, indicatorColor = Color(0xFFE3EAF7)),
                    )
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            when (route) {
                is Route.Detail -> RequestDetailScreen(route.number)
                is Route.NewRequest -> NewRequestScreen()
                is Route.Notifications -> NotificationsScreen()
                is Route.Tickets -> TicketsScreen()
                is Route.NewTicket -> NewTicketScreen()
                else -> when (App.tab) {
                    0 -> HomeScreen()
                    1 -> RequestsScreen()
                    2 -> PaymentsScreen()
                    else -> ProfileScreen()
                }
            }
        }
    }
}

@Composable
fun BackRow(title: String) {
    TextButton(onClick = { App.pop() }, contentPadding = PaddingValues(0.dp)) { Text("← Back", color = Navy) }
    ScreenTitle(title)
}

@Composable
fun RequestCard(r: RequestItem) {
    AppCard(onClick = { App.push(Route.Detail(r.number)) }, modifier = Modifier.padding(bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(r.service, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Navy, modifier = Modifier.weight(1f))
            StatusChip(r.status, r.statusLabel)
        }
        Text("${r.institution} · ${r.number}", color = Muted, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
        StepsRow(r.steps)
        Text(naira(r.total), fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun HomeScreen() {
    val me by load(App.resumeTick) { Api.data("GET", "/me").toUser() }
    val reqs by load(App.resumeTick) { Api.list("/requests").objects().map { it.toRequestItem() } }
    LaunchedEffect(me.value) { me.value?.let { App.user = it } }
    ScreenTitle("Hello, ${App.user?.name?.substringBefore(' ') ?: ""}", "What would you like to do today?")
    PrimaryButton("Start a new request", gold = true) { App.push(Route.NewRequest) }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppCard(Modifier.weight(1f), onClick = { App.tab = 1 }) { Text("${App.user?.active ?: 0}", style = MaterialTheme.typography.headlineMedium, color = Navy); Text("Active requests", color = Muted) }
        AppCard(Modifier.weight(1f), onClick = { App.push(Route.Notifications) }) { Text("${App.user?.unread ?: 0}", style = MaterialTheme.typography.headlineMedium, color = Navy); Text("Unread alerts", color = Muted) }
    }
    SectionTitle("Recent requests")
    ErrorBox(reqs.error)
    if (reqs.loading && reqs.value == null) Loading()
    reqs.value?.let { list ->
        if (list.isEmpty()) EmptyState("No requests yet", "Tap “Start a new request” to begin.")
        list.take(5).forEach { RequestCard(it) }
    }
}

@Composable
fun RequestsScreen() {
    val reqs by load(App.resumeTick) { Api.list("/requests").objects().map { it.toRequestItem() } }
    ScreenTitle("My requests")
    ErrorBox(reqs.error)
    if (reqs.loading && reqs.value == null) Loading()
    reqs.value?.let { list ->
        if (list.isEmpty()) EmptyState("Nothing here yet", "Your admission requests will show up here.")
        list.forEach { RequestCard(it) }
    }
    Spacer(Modifier.height(8.dp))
    PrimaryButton("New request", gold = true) { App.push(Route.NewRequest) }
}

@Composable
fun PaymentsScreen() {
    val pays by load(App.resumeTick) { Api.list("/payments").objects().map { it.toPayment() } }
    ScreenTitle("Payments")
    ErrorBox(pays.error)
    if (pays.loading && pays.value == null) Loading()
    pays.value?.let { list ->
        if (list.isEmpty()) EmptyState("No payments yet", "Receipts appear here after you pay for a request.")
        list.forEach { p ->
            AppCard(modifier = Modifier.padding(bottom = 10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(naira(p.amount), fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = Navy)
                    StatusChip(p.status, "")
                }
                Text("Request ${p.request}", color = Muted)
                if (p.receipt.isNotBlank()) Text("Receipt ${p.receipt}", color = Muted)
                Text(p.date, color = Muted)
            }
        }
    }
}

@Composable
fun NotificationsScreen() {
    val items by load(App.resumeTick) { Api.list("/notifications").objects().map { it.toNotice() } }
    LaunchedEffect(items.value) { if (items.value?.any { !it.read } == true) try { Api.call("POST", "/notifications/read") } catch (_: ApiException) {} }
    BackRow("Notifications")
    ErrorBox(items.error)
    if (items.loading && items.value == null) Loading()
    items.value?.let { list ->
        if (list.isEmpty()) EmptyState("All caught up", "No notifications yet.")
        list.forEach { n ->
            AppCard(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(n.title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Navy)
                Text(n.body, modifier = Modifier.padding(vertical = 4.dp))
                Text(n.date, color = Muted)
            }
        }
    }
}
