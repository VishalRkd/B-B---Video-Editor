package com.bbvideoeditor.app.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bbvideoeditor.core.*

/** Ordered EffectSpec draft. Apply is one ordinary undoable command; Cancel discards the draft. */
@Composable
fun ColorEffectsInspector(clip: Clip, onDismiss: () -> Unit, onApply: (List<EffectSpec>) -> Unit) {
    var effects by remember(clip.id) { mutableStateOf(clip.effects) }
    fun replace(index: Int, effect: EffectSpec) { effects = effects.toMutableList().apply { set(index, effect) } }
    fun move(index: Int, destination: Int) { effects = effects.toMutableList().apply { add(destination, removeAt(index)) } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Color effects") }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
            Text("Effects render from top to bottom. Apply updates the preview.")
            ColorAdjustments.all.forEach { definition ->
                TextButton(onClick = { effects = effects + EffectSpec(type = definition.type, parameters = mapOf("amount" to definition.default)) }) {
                    Text("Add ${definition.label}")
                }
            }
            effects.forEachIndexed { index, effect ->
                key(effect.id, index) {
                    val definition = ColorAdjustments.find(effect.type)
                    HorizontalDivider()
                    Row {
                        Text("${index + 1}. ${definition?.label ?: effect.type}", Modifier.weight(1f))
                        Switch(effect.enabled, { replace(index, effect.copy(enabled = it)) })
                    }
                    if (definition != null && effect.properties.isEmpty()) {
                        val amount = effect.parameters["amount"] ?: definition.default
                        Text("%.2f".format(amount))
                        Slider(value = amount.takeIf { it.isFinite() }?.coerceIn(definition.range) ?: definition.default,
                            onValueChange = { replace(index, effect.copy(parameters = effect.parameters + ("amount" to it))) },
                            valueRange = definition.range, enabled = effect.enabled,
                            modifier = Modifier.testTag("effect-amount-$index"))
                    } else Text("This effect cannot be adjusted here. It is preserved until removed.")
                    Row {
                        TextButton(onClick = { move(index, index - 1) }, enabled = index > 0) { Text("Up") }
                        TextButton(onClick = { move(index, index + 1) }, enabled = index < effects.lastIndex) { Text("Down") }
                        TextButton(onClick = { effects = effects.filterIndexed { i, _ -> i != index } }) { Text("Remove") }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { onApply(effects) }) { Text("Apply effects") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
