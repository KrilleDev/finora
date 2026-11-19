package app.finora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.finora.ui.FinoraApp
import app.finora.ui.MainViewModel
import app.finora.ui.theme.FinoraTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var addRequests by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null && isAddIntent(intent)) addRequests++
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            FinoraTheme(settings.theme) {
                FinoraApp(vm, addRequest = addRequests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isAddIntent(intent)) addRequests++
    }

    private fun isAddIntent(i: Intent?) =
        i?.action == ACTION_ADD || i?.getBooleanExtra(EXTRA_ADD, false) == true

    companion object {
        const val ACTION_ADD = "app.finora.ADD_EXPENSE"
        const val EXTRA_ADD = "open_add"
    }
}
