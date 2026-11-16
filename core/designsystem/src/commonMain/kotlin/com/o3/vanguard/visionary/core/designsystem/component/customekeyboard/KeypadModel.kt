package com.o3.vanguard.visionary.core.designsystem.component.customekeyboard

enum class ButtonType {
    Number,
    Back,
    Action
}

data class KeypadModel(
    val value: String? = null,
    val iconResName: String? = null,
    val type: ButtonType = ButtonType.Number
)
