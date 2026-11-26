package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Modifier.themedScreenBackground(): Modifier {
    val spec = LocalAppThemeSpec.current
    return this.drawBehind {
        if (spec.isOledBlack) {
            drawRect(color = Color.Black)
        } else {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(spec.bgGradientTop, spec.bgGradientBottom)
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        spec.accentBright.copy(alpha = if (spec.hasSpatialGlow) 0.11f else 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.10f),
                    radius = size.maxDimension * 0.56f
                ),
                radius = size.maxDimension * 0.56f,
                center = Offset(size.width * 0.5f, size.height * 0.10f)
            )
            if (spec.hasSpatialGlow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            spec.tertiaryAccent.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.85f),
                        radius = size.maxDimension * 0.45f
                    ),
                    radius = size.maxDimension * 0.45f,
                    center = Offset(size.width * 0.88f, size.height * 0.85f)
                )
            }
        }
    }
}

@Composable
fun themedCardBorder(accentFocused: Boolean = false): BorderStroke {
    val spec = LocalAppThemeSpec.current
    return when {
        accentFocused -> BorderStroke(
            width = if (spec.isOledBlack) 1.2.dp else 1.5.dp,
            color = spec.accentBright.copy(alpha = if (spec.isOledBlack) 0.85f else 0.72f)
        )
        spec.hasSpecularRim -> BorderStroke(
            width = spec.cardBorderWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (spec.isOledBlack) 0.24f else 0.30f),
                    spec.cardBorderColor.copy(alpha = 0.35f),
                    spec.accentBright.copy(alpha = 0.16f)
                )
            )
        )
        else -> BorderStroke(spec.cardBorderWidth, spec.cardBorderColor)
    }
}

@Composable
fun BrandTopBar(
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    showThemeButton: Boolean = false,
    onThemeClick: (() -> Unit)? = null,
    showSettings: Boolean = false,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spec = LocalAppThemeSpec.current
    val usePillChrome = spec.hasSpecularRim || spec.hasSpatialGlow || spec.isOledBlack

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .height(52.dp)
    ) {
        if (showBack && onBack != null) {
            if (usePillChrome) {
                Surface(
                    onClick = onBack,
                    shape = spec.pillShape,
                    color = if (spec.isOledBlack) Color(0xFF0A0B0E) else spec.surfaceCard.copy(alpha = 0.82f),
                    border = themedCardBorder(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .size(48.dp)
                        .testTag("back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = spec.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .minimumInteractiveComponentSize()
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = spec.textPrimary
                    )
                }
            }
        } else if (showThemeButton && onThemeClick != null) {
            Surface(
                onClick = onThemeClick,
                shape = spec.pillShape,
                color = if (spec.isOledBlack) Color(0xFF0A0B0E) else spec.surfaceCard.copy(alpha = 0.86f),
                border = themedCardBorder(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .testTag("theme_ui_quick_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = "Theme UI",
                        tint = spec.accentBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (spec.isOledBlack && spec.style.id != "oled") {
                            "${spec.style.title} • OLED"
                        } else {
                            spec.style.title
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = spec.textPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = if (spec.isOledBlack) spec.accentBright else spec.textPrimary,
                    radius = radius - 1.5f,
                    center = center,
                    style = Stroke(width = 2.2f)
                )
                drawStar(
                    center = center,
                    outerRadius = radius * 0.72f,
                    innerRadius = radius * 0.30f,
                    color = spec.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "PROTECTSTAR®",
                color = spec.textPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp
            )
        }

        if (showSettings && onSettingsClick != null) {
            if (usePillChrome) {
                Surface(
                    onClick = onSettingsClick,
                    shape = spec.pillShape,
                    color = if (spec.isOledBlack) Color(0xFF0A0B0E) else spec.surfaceCard.copy(alpha = 0.82f),
                    border = themedCardBorder(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(48.dp)
                        .testTag("settings_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = spec.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .minimumInteractiveComponentSize()
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = spec.textPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StandardTitleTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    val spec = LocalAppThemeSpec.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = spec.textPrimary
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = spec.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

@Composable
fun WizardStepperBar(
    activeStep: Int, // 1, 2, or 3
    onStepClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spec = LocalAppThemeSpec.current
    val steps = listOf(
        1 to stringResource(R.string.step_data),
        2 to stringResource(R.string.step_method),
        3 to stringResource(R.string.step_shred)
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = spec.stepperShape,
        color = spec.surfaceCard,
        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEach { (stepIndex, label) ->
                val isActive = stepIndex == activeStep
                val bgColor = if (isActive) spec.accentPrimary else Color.Transparent
                val textColor = if (isActive) Color.White else spec.textSecondary
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(spec.stepperShape)
                        .background(bgColor)
                        .then(
                            if (onStepClick != null && stepIndex < activeStep) {
                                Modifier.clickable { onStepClick(stepIndex) }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PrimaryShredButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val spec = LocalAppThemeSpec.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(spec.bgGradientBottom.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = spec.buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = spec.accentPrimary,
                contentColor = Color.White,
                disabledContainerColor = spec.surfaceElevated,
                disabledContentColor = spec.textSecondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(testTag)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun ShredderHeroGraphic(
    isAnimated: Boolean = false,
    sizeDp: Dp = 236.dp,
    modifier: Modifier = Modifier
) {
    val spec = LocalAppThemeSpec.current
    val infiniteTransition = rememberInfiniteTransition(label = "shredder_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blade_pulse"
    )
    val stripBounce = if (isAnimated) pulse * 10f else 0f

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * 0.44f

            // Outer circular halo ring + tinted fill
            val ringRadius = w * 0.39f
            drawCircle(
                color = spec.accentTintBg,
                radius = ringRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = spec.accentBright,
                radius = ringRadius,
                center = Offset(cx, cy),
                style = Stroke(width = w * 0.036f)
            )

            // Device body (top half above blade)
            val bodyLeft = w * 0.25f
            val bodyWidth = w * 0.50f
            val bodyTop = h * 0.07f
            val bladeY = h * 0.50f

            // Subtle side hardware buttons
            drawRoundRect(
                color = spec.bgDeep,
                topLeft = Offset(bodyLeft + bodyWidth - 2f, bodyTop + h * 0.12f),
                size = Size(w * 0.02f, h * 0.09f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Main device body
            drawRoundRect(
                color = spec.bgDeep,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bladeY - bodyTop + 4f),
                cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
            )
            // Inner slate panel
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(spec.surfaceElevated, spec.surfaceCard)
                ),
                topLeft = Offset(bodyLeft + w * 0.015f, bodyTop + w * 0.015f),
                size = Size(bodyWidth - w * 0.03f, bladeY - bodyTop),
                cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
            )

            // Front camera punch hole
            drawCircle(
                color = spec.bgDeep,
                radius = w * 0.014f,
                center = Offset(cx, bodyTop + h * 0.038f)
            )

            // 5 Shredded strips below the blade
            val stripLengths = listOf(0.36f, 0.34f, 0.28f, 0.23f, 0.30f)
            val stripCount = 5
            val stripGap = w * 0.028f
            val totalStripsW = bodyWidth * 0.86f
            val stripW = (totalStripsW - stripGap * (stripCount - 1)) / stripCount
            val stripsStartX = cx - totalStripsW / 2f

            stripLengths.forEachIndexed { idx, lenRatio ->
                val sx = stripsStartX + idx * (stripW + stripGap)
                val sh = h * lenRatio + (if (idx % 2 == 0) stripBounce else -stripBounce * 0.6f)
                val path = Path().apply {
                    moveTo(sx, bladeY + w * 0.015f)
                    lineTo(sx + stripW, bladeY + w * 0.015f)
                    lineTo(sx + stripW, bladeY + sh)
                    lineTo(sx, bladeY + sh - w * 0.022f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(spec.surfaceElevated, spec.surfaceCard)
                    )
                )
            }

            // Center circular SHRED emblem on the device
            val badgeCenter = Offset(cx, bodyTop + (bladeY - bodyTop) * 0.52f)
            val badgeOuterR = w * 0.145f
            drawCircle(
                color = Color(0xFFF2F4F6),
                radius = badgeOuterR,
                center = badgeCenter
            )
            drawCircle(
                color = spec.accentBright,
                radius = badgeOuterR * 0.78f,
                center = badgeCenter,
                style = Stroke(width = w * 0.018f)
            )
            drawStar(
                center = Offset(badgeCenter.x, badgeCenter.y - badgeOuterR * 0.26f),
                outerRadius = badgeOuterR * 0.18f,
                innerRadius = badgeOuterR * 0.075f,
                color = spec.accentBright
            )

            // Horizontal shredder blade bar
            val barWidth = bodyWidth + w * 0.08f
            val barLeft = cx - barWidth / 2f
            val barHeight = w * 0.042f
            drawRoundRect(
                color = if (isAnimated) spec.accentGlow else spec.accentBright,
                topLeft = Offset(barLeft, bladeY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
            )
            // Two small indicator LEDs on right of blade
            drawCircle(
                color = Color.White,
                radius = barHeight * 0.24f,
                center = Offset(barLeft + barWidth - barHeight * 1.4f, bladeY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = barHeight * 0.24f,
                center = Offset(barLeft + barWidth - barHeight * 0.7f, bladeY)
            )
        }

        // Crisp Compose text inside the SHRED badge
        Text(
            text = "SHRED",
            color = spec.accentPrimary,
            fontSize = (sizeDp.value * 0.056f).sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(bottom = sizeDp * 0.34f)
        )
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = Path()
    val angleStep = PI / 5.0
    var angle = -PI / 2.0
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += angleStep
    }
    path.close()
    drawPath(path = path, color = color)
}

@Composable
fun PasscodeLockOverlay(
    onVerifyPin: (String) -> Boolean
) {
    val spec = LocalAppThemeSpec.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = spec.bgDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .themedScreenBackground()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(spec.accentPrimary.copy(alpha = 0.18f))
                    .border(1.5.dp, spec.accentBright, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "App Locked",
                    tint = spec.accentBright,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "iShredder Protected",
                style = MaterialTheme.typography.titleLarge,
                color = spec.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enter your 4-digit passcode to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = spec.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { idx ->
                    val filled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (filled) spec.accentBright else Color.Transparent)
                            .border(2.dp, if (filled) spec.accentBright else spec.textSecondary, CircleShape)
                    )
                }
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMsg!!,
                    color = spec.danger,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("CLR", "0", "DEL")
            )
            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    row.forEach { key ->
                        Surface(
                            onClick = {
                                errorMsg = null
                                when (key) {
                                    "CLR" -> enteredPin = ""
                                    "DEL" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                    else -> {
                                        if (enteredPin.length < 4) {
                                            enteredPin += key
                                            if (enteredPin.length == 4) {
                                                val ok = onVerifyPin(enteredPin)
                                                if (!ok) {
                                                    errorMsg = "Incorrect passcode. Try again."
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = spec.surfaceCard,
                            border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("pin_key_$key")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "DEL") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete digit",
                                        tint = spec.textPrimary
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = if (key == "CLR") 13.sp else 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = spec.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
