/*
 * Mushotoku — a privacy-focused, offline productivity app.
 * Copyright (C) 2026 Tom Frischmuth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mushotoku.app.ui.components

import android.view.SoundEffectConstants
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch

class ClickSoundIndication(
    private val delegate: IndicationNodeFactory,
) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        ClickSoundNode(interactionSource, delegate.create(interactionSource))

    override fun equals(other: Any?): Boolean =
        other is ClickSoundIndication && other.delegate == delegate

    override fun hashCode(): Int = delegate.hashCode()
}

private class ClickSoundNode(
    private val interactionSource: InteractionSource,
    rippleNode: DelegatableNode,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {

    init {
        delegate(rippleNode)
    }

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    currentValueOf(LocalView).playSoundEffect(SoundEffectConstants.CLICK)
                }
            }
        }
    }
}

val AppClickSoundIndication: ClickSoundIndication = ClickSoundIndication(ripple())

// Material components hardcode their ripple and ignore LocalIndication, so the
// app-wide ClickSoundIndication never reaches them — wrap their onClick instead.
@Composable
fun soundClick(onClick: () -> Unit): () -> Unit {
    val view = LocalView.current
    return {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        onClick()
    }
}

@Composable
fun soundCheck(onCheckedChange: ((Boolean) -> Unit)?): ((Boolean) -> Unit)? {
    if (onCheckedChange == null) return null
    val view = LocalView.current
    return { checked ->
        view.playSoundEffect(SoundEffectConstants.CLICK)
        onCheckedChange(checked)
    }
}
