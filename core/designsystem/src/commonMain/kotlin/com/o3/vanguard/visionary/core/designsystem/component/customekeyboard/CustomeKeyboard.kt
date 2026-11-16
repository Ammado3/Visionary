package com.o3.vanguard.visionary.core.designsystem.component.customekeyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.o3.vanguard.visionary.core.designsystem.AppTheme


val keyboardDataList = arrayListOf(
    KeypadModel(value = "1"),
    KeypadModel(value = "2"),
    KeypadModel(value = "3"),
    KeypadModel(value = "4"),
    KeypadModel(value = "5"),
    KeypadModel(value = "6"),
    KeypadModel(value = "7"),
    KeypadModel(value = "8"),
    KeypadModel(value = "9"),
    KeypadModel(type = ButtonType.Action),
    KeypadModel(value = "0"),
    KeypadModel(type = ButtonType.Back),
)

@Composable
fun VisionaryCustomKeyBoard(
    modifier: Modifier = Modifier,
    onNumberClick: (String) -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
    textColor: Color = AppTheme.colorScheme.black,
    actionContent: @Composable () -> Unit = {},
    deleteContent: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = keyboardDataList.chunked(3)
        for (rowItems in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (item in rowItems) {
                    KeypadButtonCell(
                        model = item,
                        textColor = textColor,
                        actionContent = actionContent,
                        deleteContent = deleteContent,
                        onClick = {
                            when (item.type) {
                                ButtonType.Number -> {
                                    item.value?.let { onNumberClick(it) }
                                }
                                ButtonType.Back -> {
                                    onDeleteClick()
                                }
                                ButtonType.Action -> {
                                    onActionClick()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButtonCell(
    model: KeypadModel,
    textColor: Color,
    actionContent: @Composable () -> Unit,
    deleteContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        when (model.type) {
            ButtonType.Number -> {
                Text(
                    text = model.value ?: "",
                    color = textColor,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
            }
            ButtonType.Action -> {
                actionContent()
            }
            ButtonType.Back -> {
                deleteContent()
            }
        }
    }
}
