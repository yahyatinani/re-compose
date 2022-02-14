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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.whyrising.recompose.example.home.DigitalWatch
import com.github.whyrising.recompose.example.home.ThemeForm
import com.github.whyrising.recompose.example.ui.theme.RecomposeTheme

@Composable
fun MyApp() {
    RecomposeTheme {
        Scaffold(
            topBar = {
                TopAppBar {
                    Text(
                        text = "Re-compose Sample",
                        style = MaterialTheme.typography.h5,
                    )
                }
            },
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    DigitalWatch()

                    Spacer(modifier = Modifier.height(32.dp))

                    ThemeForm(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// -- App Preview --------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RecomposeTheme {
        MyApp()
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun DefaultDarkPreview() {
    RecomposeTheme {
        MyApp()
    }
}

// -- Entry Point --------------------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}
