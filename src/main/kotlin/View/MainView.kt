package View

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gst.GstPlayerK
import tools.AnimatedGif
import javax.imageio.ImageIO


@Composable
fun MainView(gstPlayer: GstPlayerK) {
    //var flag by remember { mutableStateOf(gstPlayer.isVisible) }
    val gif1 = remember {
            useResource("images/de.gif"){
                AnimatedGif(ImageIO.createImageInputStream(it))
            }
    }

    gstPlayer.start()
    Column() {
        Text("请确保您的设备和电脑在同一个WIFI网络环境下",
            fontSize = 25.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(15.dp)
        )

        Row(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxHeight().width(550.dp)) {
                Text("使用说明:")
                OneRow("images/number-1.png",".打开苹果控制中心")
                OneRow("images/number-2.png",".点击屏幕镜像")
                OneRow("images/number-3.png",".找到并点击‘小苹果’")
                Spacer(Modifier.height(270.dp))
                Divider(
                    //设置分割线的高度
                    thickness = 0.5.dp,
                    //设置分割线的颜色
                    color = Color.Gray,
                    //设置分割线首缩进的大小
                    startIndent = 0.dp
                )

                Text("Version 1.0", modifier = Modifier.height(30.dp).padding(5.dp))
            }
            AnimatedGif(gif1, Modifier.size(200.dp,400.dp).border(BorderStroke(2.dp, Color.Gray)))

        }


//        if (flag){
//            Window(
//                    title = "小苹果投屏",
//                    onCloseRequest = {},
//                    state = rememberWindowState(width = Dp.Unspecified, height = Dp.Unspecified),
//            ){
//                SwingPanel(
//                        background = Color.White,
//                        modifier = Modifier.fillMaxSize(),
//                        factory = {
//                            gstPlayer.vc
//                        },
//                        update = {
//                        }
//                )
//            }
//        }
    }
}

@Composable
fun OneRow(imagePath:String,text: String){
    Row {
        Image(
            painter = painterResource(imagePath),
            contentDescription = "number",
            modifier = Modifier.size(25.dp,25.dp)
        )
        Text(text, fontSize = 20.sp)
    }
}



