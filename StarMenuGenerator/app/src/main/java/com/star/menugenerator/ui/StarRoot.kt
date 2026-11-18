package com.star.menugenerator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.star.menugenerator.ui.screens.BackupScreen
import com.star.menugenerator.ui.screens.EditorScreen
import com.star.menugenerator.ui.screens.HomeScreen
import com.star.menugenerator.ui.screens.MyMenusScreen
import com.star.menugenerator.ui.screens.PreviewScreen
import com.star.menugenerator.ui.screens.SplashScreen
import com.star.menugenerator.ui.screens.TemplateScreen
import kotlinx.coroutines.launch

/** Screen switcher. Back button pops the stack; the editor autosaves whenever the app goes to the background. */
@Composable
fun StarRoot(vm: AppViewModel = viewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.flush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onError: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    BackHandler(enabled = vm.stack.size > 1 && vm.screen != Screen.Splash) { vm.back() }

    Box(Modifier.fillMaxSize()) {
        when (vm.screen) {
            Screen.Splash -> SplashScreen(onFinished = { vm.replaceAll(Screen.Home) })
            Screen.Home -> HomeScreen(vm, onError)
            Screen.MyMenus -> MyMenusScreen(vm)
            Screen.Editor -> EditorScreen(vm)
            Screen.Templates -> TemplateScreen(vm)
            Screen.Preview -> PreviewScreen(vm)
            Screen.Backup -> BackupScreen(vm)
        }
        SnackbarHost(snackbar, Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
    }
}
