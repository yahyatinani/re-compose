package com.github.whyrising.recompose.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Surface
import androidx.lifecycle.lifecycleScope
import com.github.whyrising.recompose.dispatch
import com.github.whyrising.recompose.events.event
import com.github.whyrising.recompose.sample.Keys.startTicks
import com.github.whyrising.recompose.sample.ui.theme.RecomposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reg(lifecycleScope)

        dispatch(event(startTicks))

        setContent {
            RecomposeTheme {
                Surface {
                    TimeApp()
                }
            }
        }
    }
}
