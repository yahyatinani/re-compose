package io.github.yahyatinani.recompose.example

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import io.github.yahyatinani.recompose.dispatch
import io.github.yahyatinani.recompose.dispatchSync
import io.github.yahyatinani.recompose.example.Ids.about_dialog
import io.github.yahyatinani.recompose.example.Ids.initDb
import io.github.yahyatinani.recompose.example.Ids.startTicking
import io.github.yahyatinani.recompose.example.composables.DigitalWatch
import io.github.yahyatinani.recompose.example.composables.InputThemeForm
import io.github.yahyatinani.recompose.example.db.AppDb
import io.github.yahyatinani.recompose.example.db.regAllCofx
import io.github.yahyatinani.recompose.example.events.regAllEvents
import io.github.yahyatinani.recompose.example.fx.regAllFx
import io.github.yahyatinani.recompose.example.subs.regAllSubs
import io.github.yahyatinani.recompose.example.theme.RecomposeTheme
import io.github.yahyatinani.recompose.regEventDb
import io.github.yahyatinani.recompose.regFx
import io.github.yahyatinani.recompose.watch
import io.github.yahyatinani.y.core.v
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
              style = MaterialTheme.typography.h5
            )
          },
          actions = {
            IconButton(onClick = { dispatch(v(about_dialog, true)) }) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info"
              )
            }
            IconButton(onClick = { dispatch(v(Ids.exitApp)) }) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit"
              )
            }
          }
        )
      }
    ) { padding ->
      Surface(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        color = colors.background
      ) {
        if (watch(query = v(about_dialog))) {
          AlertDialog(
            onDismissRequest = { dispatch(v(about_dialog, false)) },
            properties = DialogProperties(),
            title = { Text(text = "Info") },
            text = {
              Text(text = watch<String>(query = v(Ids.info)))
            },
            buttons = {}
          )
        }

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

fun initAppDb() {
  regEventDb<Any>(id = initDb) { _, _ ->
    AppDb(info = "Best app!")
  }
  dispatchSync(v(initDb))
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    regAllEvents()
    regAllCofx()
    regAllFx()

    setContent {
      val scope = rememberCoroutineScope()

      regFx(id = Ids.ticker) {
        scope.launch(Dispatchers.Default) {
          while (true) {
            dispatch(v(Ids.nextTick))
            delay(1_000)
          }
        }
      }

      SideEffect {
        dispatch(v(startTicking))
      }
      regAllSubs(MaterialTheme.colors)
      MyApp()
    }
  }

/*  override fun onBackPressed() {
    super.onBackPressed()

    finish()
  }*/
}
