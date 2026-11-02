package ng.eduassist.app.ui

import androidx.compose.runtime.*
import ng.eduassist.app.data.Session
import ng.eduassist.app.data.User

sealed interface Route {
    data object Login : Route
    data object Register : Route
    data class Verify(val email: String) : Route
    data class Detail(val number: String) : Route
    data object NewRequest : Route
    data object Notifications : Route
    data object Tickets : Route
    data object NewTicket : Route
}

/** One place for app-wide state: who is signed in and which screen is open. */
object App {
    var loggedIn by mutableStateOf(false)
    var user by mutableStateOf<User?>(null)
    var tab by mutableIntStateOf(0)
    /** Bumped every time the app returns to the foreground (e.g. after the payment page closes). */
    var resumeTick by mutableIntStateOf(0)
    var pendingPassword: String? = null
    val stack = mutableStateListOf<Route>()

    fun init() { loggedIn = Session.token != null }
    fun push(r: Route) { stack.add(r) }
    fun pop(): Boolean = if (stack.isNotEmpty()) { stack.removeAt(stack.lastIndex); true } else false
    fun signedOut() { Session.clear(); loggedIn = false; user = null; stack.clear(); tab = 0 }
    fun signedIn() { loggedIn = true; stack.clear(); tab = 0 }
}
