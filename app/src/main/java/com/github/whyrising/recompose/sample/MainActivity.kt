package com.github.whyrising.recompose.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.whyrising.recompose.*
import com.github.whyrising.recompose.events.event
import com.github.whyrising.recompose.sample.Keys.*
import com.github.whyrising.recompose.sample.ui.theme.RecomposeTheme

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Composable
fun App(name: String, counter: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Greeting(name)
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Text(text = "Counter:")
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = counter)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { dispatch(event(inc)) }) {
            Text(text = "Inc")
        }
    }
}

class MainActivity : ComponentActivity() {
    val framework = Framework()

    override fun onDestroy() {
        super.onDestroy()

        framework.halt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        regSub(text) { db: AppSchema, _: ArrayList<Any> ->
            db.text.uppercase()
        }

        regEventDb(inc) { db, _ ->
            (db as AppSchema).copy(counter = db.counter + 1)
        }

        regSub(counter) { db: AppSchema, _: ArrayList<Any> ->
            "${db.counter}"
        }

        setContent {
            RecomposeTheme {
                Surface(color = MaterialTheme.colors.background) {
                    App(
                        name = subscribe(event(text)),
                        counter = subscribe(event(counter))
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RecomposeTheme {
        App("Android", "0")
    }
}
