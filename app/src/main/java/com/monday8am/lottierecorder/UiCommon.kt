package com.monday8am.lottierecorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.monday8am.lottierecorder.ui.theme.LottieRecorderTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

enum class LottieAnimationId(val value: Int) {
    BIRDS(R.raw.birds_lottie),
    CYCLING(R.raw.cycling_lottie),
    VAN(R.raw.van_lottie),
}

private fun getTextFrom(item: LottieAnimationId): String {
    return when (item) {
        LottieAnimationId.BIRDS -> "Birds"
        LottieAnimationId.CYCLING -> "Cycling"
        LottieAnimationId.VAN -> "Van"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LottieSelector(
    selectedOption: LottieAnimationId,
    options: List<LottieAnimationId>,
    onSelectOption: (option: LottieAnimationId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        Button(
            onClick = { },
            modifier = Modifier
                .height(40.dp)
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(getTextFrom(selectedOption))
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        }
        if (options.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
                    .exposedDropdownSize(true),
            ) {
                options.forEach { singleItem ->
                    DropdownMenuItem(
                        text = { Text(getTextFrom(singleItem)) },
                        onClick = {
                            onSelectOption(singleItem)
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
internal fun LottieSceneEditor(
    items: List<LottieAnimationId>,
    onItemsReordered: (List<LottieAnimationId>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var reorderedList by remember { mutableStateOf(items) }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorderedList = reorderedList.toMutableList().apply {
            add(to.index, removeAt(from.index))
            onItemsReordered(reorderedList)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        val localDensity = LocalDensity.current
        var rowWidth by remember { mutableIntStateOf(300) }
        val iconWidth by remember {
            derivedStateOf {
                with(localDensity) { rowWidth.toDp() / 3  }
            }
        }

        Text("Sort items using drag & drop:")

        LazyRow(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged({ size ->
                    rowWidth = size.width
                }),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                count = reorderedList.size,
                key = { reorderedList[it].name },
                itemContent = { index ->
                    ReorderableItem(reorderableLazyListState, key = reorderedList[index].name) { isDragging ->
                        LottieThumbnail(
                            reorderedList[index].value,
                            modifier = Modifier
                                .width(width = iconWidth)
                                .draggableHandle()
                        )
                    }
                }
            )
        }

        Button(
            onClick = { },
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp),
        ) {
            Text("Render video!")
        }
    }
}

@Composable
private fun LottieThumbnail(
    resId: Int,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(resId)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(modifier = modifier.aspectRatio(1f)) {
        LottieAnimation(
            composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
private fun LottieSceneEditorPreview() {
    LottieRecorderTheme {
        LottieSceneEditor(
            items = LottieAnimationId.entries,
            onItemsReordered = { },
        )
    }
}


@Preview
@Composable
private fun LottieSelectorPreview() {
    LottieRecorderTheme {
        LottieSelector(
            selectedOption = LottieAnimationId.BIRDS,
            options = LottieAnimationId.entries,
            onSelectOption = {}
        )
    }
}

