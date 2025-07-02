package com.monday8am.lottierecorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.monday8am.lottierecorder.ui.theme.LottieRecorderTheme

enum class LottieAnimationId(val value: Int) {
    BIRDS(R.raw.birds_lottie),
    CYCLING(R.raw.cycling_lottie),
    VAN(R.raw.van_lottie),
    ALL(-1),
}

private fun getTextFrom(item: LottieAnimationId): String {
    return when (item) {
        LottieAnimationId.BIRDS -> "Birds"
        LottieAnimationId.CYCLING -> "Cycling"
        LottieAnimationId.VAN -> "Van"
        LottieAnimationId.ALL -> "All together!"
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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

