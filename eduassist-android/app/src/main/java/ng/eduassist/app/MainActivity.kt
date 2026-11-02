package ng.eduassist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import ng.eduassist.app.data.Session
import ng.eduassist.app.push.PushRegistrar
import ng.eduassist.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Session.init(this)
        App.init()
        setContent {
            EduTheme {
                // When the app comes back to the foreground (e.g. after the payment page), refresh the screens.
                val owner = LocalLifecycleOwner.current
                DisposableEffect(owner) {
                    val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) App.resumeTick++ }
                    owner.lifecycle.addObserver(obs)
                    onDispose { owner.lifecycle.removeObserver(obs) }
                }
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val askPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { PushRegistrar.register(ctx) }
                LaunchedEffect(App.loggedIn) {
                    if (App.loggedIn) {
                        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                            androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) askPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS) else PushRegistrar.register(ctx)
                    }
                }
                BackHandler(enabled = App.stack.isNotEmpty()) { App.pop() }
                Surface(Modifier.fillMaxSize(), color = Bg) {
                    if (!App.loggedIn) AuthHost() else MainHost()
                }
            }
        }
    }
}
