// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import View.MainView
import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.serezhka.jap2server.AirPlayServer
import gst.GstPlayerJ
import gst.GstPlayerK

@Composable
@Preview
fun App(gstPlayer: GstPlayerK) {
    MaterialTheme {
        MainView(gstPlayer)
    }
}



fun main() = application {
    val state = remember { MyAirServerState() }
    var isVisible by remember { mutableStateOf(true) }
    //gstPlayer = GstPlayerJ()

    Window(
        title = "小苹果投屏",
        onCloseRequest = { isVisible = false },
        visible = isVisible,
        icon= painterResource("images/appleWhite.png"),
        resizable = false,
        state = rememberWindowState(width = 800.dp, height = 500.dp)
    ) {
        App(state.gstPlayer)
    }
    if (!isVisible) {
        Tray(
            painterResource("images/appleWhite.png"),//TrayIcon,
            tooltip = "小苹果投屏",
            onAction = { isVisible = true },
            menu = {
                Item("Exit App", onClick = ::exitApplication)
            },
        )
    }
}
object TrayIcon : Painter() {
    override val intrinsicSize = Size(256f, 256f)

    override fun DrawScope.onDraw() {
        drawOval(Color(0xFFFFA500))
    }
}

private class MyAirServerState {
    lateinit var airPlayServer: AirPlayServer

    //lateinit var gstPlayer: GstPlayerJ
    lateinit var gstPlayer: GstPlayerK
    init {
        gstPlayer = GstPlayerK()
        airPlayServer = AirPlayServer("小苹果", 15614, 5001, gstPlayer)
        airPlayServer.start()
    }
}