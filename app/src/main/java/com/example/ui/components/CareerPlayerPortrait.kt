package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CareerPlayerAppearance
import com.example.data.model.FacialFeaturePreset
import com.example.data.model.HairstylePreset
import com.example.ui.theme.AuctionGold
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NavyMidnight
import com.example.ui.theme.NavySurfaceHigh

/**
 * Lightweight vector Canvas avatar for Player Career Mode.
 * Renders the player's chosen Hairstyle, Facial Features, Jersey/Kit theme, Jersey Number,
 * and Bat/Equipment style with zero bitmap overhead.
 */
@Composable
fun CareerPlayerPortrait(
    appearance: CareerPlayerAppearance,
    size: Dp = 68.dp,
    showJerseyNumber: Boolean = true,
    isCaptain: Boolean = false,
    modifier: Modifier = Modifier
) {
    val kitPrimary = Color(appearance.kitTheme.primaryArgb)
    val kitAccent = Color(appearance.kitTheme.accentArgb)
    val kitTrim = Color(appearance.kitTheme.trimArgb)
    val batBlade = Color(appearance.batStyle.bladeArgb)
    val batSticker = Color(appearance.batStyle.stickerArgb)
    val batGrip = Color(appearance.batStyle.gripArgb)
    val skinTone = Color(0xFFD9A077)
    val skinShade = Color(0xFFC2855C)
    val hairColor = Color(0xFF1E1A18)

    Box(modifier = modifier.size(size)) {
        Surface(
            shape = CircleShape,
            color = NavySurfaceHigh,
            border = BorderStroke(
                width = if (isCaptain) 2.2.dp else 1.6.dp,
                color = if (isCaptain) AuctionGold else kitAccent
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(kitPrimary.copy(alpha = 0.42f), NavyMidnight)
                        )
                    )
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                ) {
                    val w = this.size.width
                    val h = this.size.height
                    val cx = w * 0.48f

                    // Cricket Bat resting behind right shoulder
                    rotate(degrees = 20f, pivot = Offset(w * 0.78f, h * 0.56f)) {
                        // Handle / grip
                        drawRoundRect(
                            color = batGrip,
                            topLeft = Offset(w * 0.76f, h * 0.14f),
                            size = Size(w * 0.055f, h * 0.24f),
                            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
                        )
                        // Blade
                        drawRoundRect(
                            color = batBlade,
                            topLeft = Offset(w * 0.71f, h * 0.36f),
                            size = Size(w * 0.155f, h * 0.48f),
                            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                        )
                        // Sticker stripe
                        drawRoundRect(
                            color = batSticker,
                            topLeft = Offset(w * 0.73f, h * 0.46f),
                            size = Size(w * 0.115f, h * 0.13f),
                            cornerRadius = CornerRadius(w * 0.015f, w * 0.015f)
                        )
                    }

                    // Shoulders & Jersey torso
                    val jerseyPath = Path().apply {
                        moveTo(w * 0.12f, h * 1.02f)
                        quadraticTo(w * 0.16f, h * 0.68f, cx - w * 0.12f, h * 0.64f)
                        lineTo(cx + w * 0.12f, h * 0.64f)
                        quadraticTo(w * 0.80f, h * 0.68f, w * 0.84f, h * 1.02f)
                        close()
                    }
                    drawPath(jerseyPath, color = kitPrimary)

                    // Shoulder trim stripes
                    drawLine(
                        color = kitTrim,
                        start = Offset(w * 0.22f, h * 0.72f),
                        end = Offset(w * 0.28f, h * 0.96f),
                        strokeWidth = w * 0.035f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = kitTrim,
                        start = Offset(w * 0.74f, h * 0.72f),
                        end = Offset(w * 0.68f, h * 0.96f),
                        strokeWidth = w * 0.035f,
                        cap = StrokeCap.Round
                    )

                    // Collar V-trim
                    val collar = Path().apply {
                        moveTo(cx - w * 0.09f, h * 0.64f)
                        lineTo(cx, h * 0.75f)
                        lineTo(cx + w * 0.09f, h * 0.64f)
                        close()
                    }
                    drawPath(collar, color = kitAccent)

                    // Neck
                    drawRoundRect(
                        color = skinShade,
                        topLeft = Offset(cx - w * 0.065f, h * 0.54f),
                        size = Size(w * 0.13f, h * 0.13f),
                        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                    )

                    // Long hair back layer if LONG_FLOWING
                    if (appearance.hairstyle == HairstylePreset.LONG_FLOWING) {
                        drawRoundRect(
                            color = hairColor,
                            topLeft = Offset(cx - w * 0.23f, h * 0.24f),
                            size = Size(w * 0.46f, h * 0.40f),
                            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
                        )
                    }

                    // Head
                    val headRadius = w * 0.19f
                    val headCenter = Offset(cx, h * 0.40f)
                    drawCircle(color = skinTone, radius = headRadius, center = headCenter)

                    // Eyes
                    val eyeY = h * 0.39f
                    drawCircle(color = Color(0xFF1A1A1A), radius = w * 0.02f, center = Offset(cx - w * 0.065f, eyeY))
                    drawCircle(color = Color(0xFF1A1A1A), radius = w * 0.02f, center = Offset(cx + w * 0.065f, eyeY))

                    // Facial features (Beard / Stubble / Mustache)
                    when (appearance.facialFeature) {
                        FacialFeaturePreset.CLEAN_SHAVEN -> {
                            drawLine(
                                color = skinShade,
                                start = Offset(cx - w * 0.045f, h * 0.485f),
                                end = Offset(cx + w * 0.045f, h * 0.485f),
                                strokeWidth = w * 0.018f,
                                cap = StrokeCap.Round
                            )
                        }
                        FacialFeaturePreset.LIGHT_STUBBLE -> {
                            drawArc(
                                color = hairColor.copy(alpha = 0.35f),
                                startAngle = 15f,
                                sweepAngle = 150f,
                                useCenter = false,
                                topLeft = Offset(cx - headRadius * 0.88f, headCenter.y - headRadius * 0.72f),
                                size = Size(headRadius * 1.76f, headRadius * 1.66f)
                            )
                        }
                        FacialFeaturePreset.FULL_BEARD -> {
                            val beard = Path().apply {
                                moveTo(cx - headRadius * 0.88f, h * 0.42f)
                                quadraticTo(cx - headRadius * 0.85f, h * 0.59f, cx, h * 0.61f)
                                quadraticTo(cx + headRadius * 0.85f, h * 0.59f, cx + headRadius * 0.88f, h * 0.42f)
                                quadraticTo(cx, h * 0.50f, cx - headRadius * 0.88f, h * 0.42f)
                                close()
                            }
                            drawPath(beard, color = hairColor)
                        }
                        FacialFeaturePreset.MUSTACHE -> {
                            drawLine(
                                color = hairColor,
                                start = Offset(cx - w * 0.085f, h * 0.475f),
                                end = Offset(cx, h * 0.462f),
                                strokeWidth = w * 0.028f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = hairColor,
                                start = Offset(cx, h * 0.462f),
                                end = Offset(cx + w * 0.085f, h * 0.475f),
                                strokeWidth = w * 0.028f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Hairstyle top layer
                    when (appearance.hairstyle) {
                        HairstylePreset.SHORT_FADE -> {
                            drawArc(
                                color = hairColor,
                                startAngle = 185f,
                                sweepAngle = 170f,
                                useCenter = true,
                                topLeft = Offset(cx - headRadius * 1.03f, headCenter.y - headRadius * 1.12f),
                                size = Size(headRadius * 2.06f, headRadius * 1.45f)
                            )
                        }
                        HairstylePreset.SPIKY_MODERN -> {
                            val spikes = Path().apply {
                                moveTo(cx - headRadius * 0.95f, h * 0.34f)
                                lineTo(cx - headRadius * 0.7f, h * 0.18f)
                                lineTo(cx - headRadius * 0.3f, h * 0.25f)
                                lineTo(cx, h * 0.15f)
                                lineTo(cx + headRadius * 0.32f, h * 0.24f)
                                lineTo(cx + headRadius * 0.72f, h * 0.17f)
                                lineTo(cx + headRadius * 0.95f, h * 0.34f)
                                close()
                            }
                            drawPath(spikes, color = hairColor)
                        }
                        HairstylePreset.CLASSIC_PART -> {
                            drawArc(
                                color = hairColor,
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = true,
                                topLeft = Offset(cx - headRadius * 1.05f, headCenter.y - headRadius * 1.08f),
                                size = Size(headRadius * 2.1f, headRadius * 1.4f)
                            )
                            drawLine(
                                color = skinTone.copy(alpha = 0.8f),
                                start = Offset(cx - headRadius * 0.35f, h * 0.22f),
                                end = Offset(cx - headRadius * 0.12f, h * 0.29f),
                                strokeWidth = w * 0.015f
                            )
                        }
                        HairstylePreset.LONG_FLOWING -> {
                            drawArc(
                                color = hairColor,
                                startAngle = 178f,
                                sweepAngle = 184f,
                                useCenter = true,
                                topLeft = Offset(cx - headRadius * 1.12f, headCenter.y - headRadius * 1.15f),
                                size = Size(headRadius * 2.24f, headRadius * 1.55f)
                            )
                        }
                        HairstylePreset.BUZZ_CUT -> {
                            drawArc(
                                color = hairColor.copy(alpha = 0.85f),
                                startAngle = 192f,
                                sweepAngle = 156f,
                                useCenter = true,
                                topLeft = Offset(cx - headRadius * 0.98f, headCenter.y - headRadius * 1.01f),
                                size = Size(headRadius * 1.96f, headRadius * 1.2f)
                            )
                        }
                    }
                }
            }
        }

        if (showJerseyNumber) {
            Surface(
                color = NavyMidnight.copy(alpha = 0.92f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, kitAccent),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(1.dp)
            ) {
                Text(
                    text = "#${appearance.jerseyNumber}",
                    color = kitAccent,
                    fontSize = 9.sp,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
