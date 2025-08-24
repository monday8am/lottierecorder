package com.monday8am.lottierecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monday8am.lottierecorder.recording.RecordingResult
import com.monday8am.lottierecorder.ui.LottieAnimationId
import com.monday8am.lottierecorder.ui.LottieSceneEditor
import com.monday8am.lottierecorder.ui.MainViewModel
import com.monday8am.lottierecorder.ui.Media3Player
import com.monday8am.lottierecorder.ui.theme.LottieRecorderTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LottieRecorderTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val viewModel: MainViewModel = viewModel()
                    val state by viewModel.uiState.collectAsStateWithLifecycle()

                    Content(
                        state = state,
                        onPressRender = { viewModel.recordLottie(lottieIds = it) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Content(
    state: RecordingResult,
    onPressRender: (List<LottieAnimationId>) -> Unit,
    modifier: Modifier = Modifier
) {
    var orderedItems by remember { mutableStateOf(LottieAnimationId.entries.toList()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(16.dp)
    ) {
        LottieSceneEditor(
            items = LottieAnimationId.entries,
            onItemsReordered = {
                orderedItems = it
            },
        )

        when(state) {
            is RecordingResult.Success -> {
                RendererButton(onPressRender = { onPressRender(orderedItems) })
                Text("Success: ${state.uri}")
                Media3Player(
                    uri = state.uri,
                    onDestroy = { },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is RecordingResult.Error -> Text("Error: ${state.error}")
            is RecordingResult.Rendering -> Text("Rendering... ${state.progress}")
            is RecordingResult.Idle -> RendererButton(onPressRender = { onPressRender(orderedItems) })
        }
    }
}

@Composable
private fun RendererButton(
    onPressRender: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onPressRender,
        modifier = modifier
            .height(40.dp)
            .fillMaxWidth(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp),
    ) {
        Text("Render video!")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LottieRecorderTheme {
        Content(
            state = RecordingResult.Idle,
            onPressRender = {},
        )
    }
}
