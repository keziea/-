package com.zhuri.daybyday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.zhuri.daybyday.ui.LaunchSplash
import com.zhuri.daybyday.ui.Root
import com.zhuri.daybyday.ui.theme.ZhuriTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            // Activity 声明了 configChanges=uiMode，切换深浅色不会重建，需要手动更新系统栏图标颜色
            LaunchedEffect(dark) { applySystemBarIcons(dark) }

            ZhuriTheme(darkTheme = dark) {
                var showSplash by remember { mutableStateOf(true) }

                Box(Modifier.fillMaxSize()) {
                    // 主界面先完成首帧，日升动画盖在上面，淡出后立即可交互
                    ZhuriRoot()
                    if (showSplash) {
                        LaunchSplash(onFinish = { showSplash = false })
                    }
                }
            }
        }
    }

    private fun applySystemBarIcons(dark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

@Composable
private fun ZhuriRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as ZhuriApp
    Root(repo = app.repo)
}
