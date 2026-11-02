package ng.eduassist.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ng.eduassist.app.R
import ng.eduassist.app.data.*
import org.json.JSONObject

@Composable
fun Field(value: String, onChange: (String) -> Unit, label: String, error: String? = null, keyboard: KeyboardType = KeyboardType.Text, password: Boolean = false, minLines: Int = 1) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, isError = error != null, minLines = minLines,
        supportingText = if (error != null) ({ Text(error) }) else null,
        singleLine = minLines == 1, keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
fun AuthHost() {
    val route = App.stack.lastOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Image(painterResource(R.drawable.logo_emblem), contentDescription = "EduAssist NG", modifier = Modifier.size(72.dp))
        Text("EduAssist NG", fontWeight = FontWeight.ExtraBold, color = Navy, style = MaterialTheme.typography.headlineMedium)
        Text("Your Admission Journey, Made Easier.", color = Muted)
        Spacer(Modifier.height(20.dp))
        when (route) {
            is Route.Register -> RegisterScreen()
            is Route.Verify -> VerifyScreen(route.email)
            else -> LoginScreen()
        }
    }
}

@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AppCard {
        Text("Sign in", fontWeight = FontWeight.ExtraBold, color = Navy, style = MaterialTheme.typography.titleLarge)
        ErrorBox(error)
        Field(email, { email = it }, "Email", keyboard = KeyboardType.Email)
        Field(pass, { pass = it }, "Password", password = true)
        Spacer(Modifier.height(8.dp))
        PrimaryButton(if (busy) "Signing in…" else "Sign in", enabled = !busy && email.isNotBlank() && pass.isNotBlank()) {
            busy = true; error = null
            scope.launch {
                try {
                    val d = Api.data("POST", "/login", JSONObject().put("email", email.trim()).put("password", pass).put("device_name", "Android app"))
                    Session.save(d.getString("token"))
                    App.signedIn()
                } catch (e: ApiException) {
                    if (e.code == "email_not_verified") App.push(Route.Verify(email.trim())) else error = e.message
                }
                busy = false
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    SecondaryButton("Create an account") { App.push(Route.Register) }
}

@Composable
fun RegisterScreen() {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("aspirant") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fields by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    AppCard {
        Text("Create your account", fontWeight = FontWeight.ExtraBold, color = Navy, style = MaterialTheme.typography.titleLarge)
        ErrorBox(error)
        Field(name, { name = it }, "Full legal name", fields["full_name"])
        Field(email, { email = it }, "Email", fields["email"], KeyboardType.Email)
        Field(phone, { phone = it }, "Phone number", fields["phone"], KeyboardType.Phone)
        Field(pass, { pass = it }, "Password (10+ characters, letters and numbers)", fields["password"], password = true)
        Text("I am a…", color = Muted, modifier = Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(type == "aspirant", { type = "aspirant" }); Text("Aspirant")
            Spacer(Modifier.width(12.dp))
            RadioButton(type == "existing_student", { type = "existing_student" }); Text("Existing student")
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton(if (busy) "Please wait…" else "Create account", enabled = !busy) {
            busy = true; error = null; fields = emptyMap()
            scope.launch {
                try {
                    Api.call("POST", "/register", JSONObject().put("full_name", name.trim()).put("email", email.trim()).put("phone", phone.trim()).put("password", pass).put("account_type", type))
                    App.pop(); App.push(Route.Verify(email.trim().lowercase()))
                } catch (e: ApiException) { error = e.message; fields = e.fields }
                busy = false
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    SecondaryButton("I already have an account") { App.pop() }
}

@Composable
fun VerifyScreen(email: String) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AppCard {
        Text("Verify your email", fontWeight = FontWeight.ExtraBold, color = Navy, style = MaterialTheme.typography.titleLarge)
        Text("We sent a 6-digit code to $email.", color = Muted, modifier = Modifier.padding(vertical = 6.dp))
        ErrorBox(error)
        info?.let { Text(it, color = Green, modifier = Modifier.padding(bottom = 6.dp)) }
        Field(code, { code = it.filter(Char::isDigit).take(6) }, "6-digit code", keyboard = KeyboardType.NumberPassword)
        Spacer(Modifier.height(8.dp))
        PrimaryButton(if (busy) "Checking…" else "Verify", enabled = !busy && code.length == 6) {
            busy = true; error = null
            scope.launch {
                try {
                    Api.call("POST", "/verify", JSONObject().put("email", email).put("code", code))
                    App.stack.clear()
                    info = "Email verified. You can now sign in."
                    App.push(Route.Login)
                } catch (e: ApiException) { error = e.message }
                busy = false
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    SecondaryButton("Resend code") {
        scope.launch { try { Api.call("POST", "/resend", JSONObject().put("email", email)); info = "A new code has been sent." } catch (e: ApiException) { error = e.message } }
    }
    TextButton(onClick = { App.stack.clear() }) { Text("Back to sign in", color = Navy) }
}
