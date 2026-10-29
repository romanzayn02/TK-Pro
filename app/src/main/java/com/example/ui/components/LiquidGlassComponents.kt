package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TallyCyanRefraction

/**
 * Clean solid background with proper Material 3 content color propagation so text is always readable
 * in both Dark and Light modes.
 */
@Composable
fun LiquidGlassBackdrop(
    useDarkGlass: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.background
    val onBgColor = MaterialTheme.colorScheme.onBackground

    CompositionLocalProvider(LocalContentColor provides onBgColor) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            content()
        }
    }
}

/**
 * Clean, flat Material 3 card with high-contrast text and borders in both Dark and Light themes.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    useDarkGlass: Boolean = false,
    accentTint: Color = TallyCyanRefraction,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )

    val shape: Shape = RoundedCornerShape(cornerRadius)

    val surfaceColor = if (useDarkGlass) {
        Color(0xFF1E293B)
    } else {
        Color.White
    }

    val onSurfaceColor = if (useDarkGlass) {
        Color(0xFFF8FAFC)
    } else {
        Color(0xFF0F172A)
    }

    val borderColor = if (useDarkGlass) {
        Color(0xFF334155)
    } else {
        Color(0xFFE2E8F0)
    }

    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(color = accentTint),
            onClick = onClick
        )
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalContentColor provides onSurfaceColor) {
        Box(
            modifier = taggedModifier
                .scale(scale)
                .clip(shape)
                .background(surfaceColor)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .then(clickableModifier),
            content = content
        )
    }
}

/**
 * Clean flat action button with automatic high-contrast text color based on background luminance.
 */
@Composable
fun SpringyLiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = TallyCyanRefraction,
    secondaryColor: Color = accentColor,
    contentColor: Color = if (accentColor.luminance() > 0.55f) Color(0xFF0F172A) else Color.White,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    testTag: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )

    val shape = RoundedCornerShape(cornerRadius)
    val taggedMod = if (testTag != null) modifier.testTag(testTag) else modifier

    Box(
        modifier = taggedMod
            .scale(scale)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .background(accentColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

/**
 * High-contrast pill chip for filters, categories, and payment methods.
 */
@Composable
fun LiquidFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    useDarkGlass: Boolean,
    accentColor: Color = TallyCyanRefraction,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String? = null
) {
    val shape = RoundedCornerShape(50)
    val bgColor = if (selected) {
        accentColor
    } else {
        if (useDarkGlass) Color(0xFF334155) else Color(0xFFE2E8F0)
    }

    val borderColor = if (selected) {
        accentColor
    } else {
        if (useDarkGlass) Color(0xFF475569) else Color(0xFFCBD5E1)
    }

    val textColor = if (selected) {
        if (accentColor.luminance() > 0.55f) Color(0xFF0F172A) else Color.White
    } else {
        if (useDarkGlass) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }

    val taggedMod = if (testTag != null) modifier.testTag(testTag) else modifier

    CompositionLocalProvider(LocalContentColor provides textColor) {
        Row(
            modifier = taggedMod
                .defaultMinSize(minHeight = 40.dp)
                .clip(shape)
                .background(bgColor)
                .border(1.dp, borderColor, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
