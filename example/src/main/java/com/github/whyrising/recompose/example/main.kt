package com.github.whyrising.recompose.example

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.coroutineScope
import com.github.whyrising.recompose.example.Ids.startTicking
import com.github.whyrising.recompose.example.composables.DigitalWatch
import com.github.whyrising.recompose.example.composables.InputThemeForm
import com.github.whyrising.recompose.example.db.regAllCofx
import com.github.whyrising.recompose.example.events.regAllEvents
import com.github.whyrising.recompose.example.fx.regAllFx
import com.github.whyrising.recompose.example.subs.regAllSubs
import com.github.whyrising.recompose.example.ui.theme.RecomposeTheme
import com.github.whyrising.recompose.router.dispatch
import com.github.whyrising.y.v
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun MyApp() {
    val systemUiController = rememberSystemUiController()
    RecomposeTheme {
        val colors = MaterialTheme.colors
        val primaryColor = colors.primary
        SideEffect {
            systemUiController.setSystemBarsColor(color = primaryColor)
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Re-compose Sample",
                            color = colors.secondary,
                            style = MaterialTheme.typography.h5,
                        )
                    },
                    actions = {
                        IconButton(onClick = { dispatch(v(Ids.exitApp)) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit"
                            )
                        }
                    },
                )
            },
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center
                ) {
                    DigitalWatch()

                    Spacer(modifier = Modifier.height(32.dp))

                    InputThemeForm(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// -- App Preview --------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApp()
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun DefaultDarkPreview() {
    MyApp()
}

// -- Entry Point --------------------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        regAllEvents()
        regAllCofx()
        regAllFx(lifecycle.coroutineScope)
        setContent {
            regAllSubs(MaterialTheme.colors)
            dispatch(v(startTicking))
            MyApp()
        }
    }
}
