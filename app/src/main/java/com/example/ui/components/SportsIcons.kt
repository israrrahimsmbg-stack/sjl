package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun BatIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFD4A017)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Draw the rubber handle (wrapped grip) at the top
        drawLine(
            color = Color(0xFFA16207), // Deep brown handle
            start = Offset(w * 0.5f, h * 0.05f),
            end = Offset(w * 0.5f, h * 0.35f),
            strokeWidth = w * 0.15f
        )
        
        // Handle grip patterns (subtle lines)
        for (i in 1..4) {
            val y = h * (0.05f + i * 0.06f)
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(w * 0.4f, y),
                end = Offset(w * 0.6f, y),
                strokeWidth = 2f
            )
        }

        // Draw the wooden shoulder connection
        val shoulderWidth = w * 0.22f
        drawRect(
            color = color,
            topLeft = Offset(w * 0.5f - shoulderWidth * 0.5f, h * 0.35f),
            size = Size(shoulderWidth, h * 0.1f)
        )

        // Draw the main blade of the bat
        val bladeWidth = w * 0.35f
        drawRect(
            color = color,
            topLeft = Offset(w * 0.5f - bladeWidth * 0.5f, h * 0.45f),
            size = Size(bladeWidth, h * 0.45f)
        )

        // Draw the curved toe of the bat at the bottom
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.5f - bladeWidth * 0.5f, h * 0.85f),
            size = Size(bladeWidth, h * 0.12f)
        )
    }
}

@Composable
fun BallIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFDC2626) // Vibrant athletic red
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2
        val cx = size.width / 2
        val cy = size.height / 2

        // Draw the main leather ball body
        drawCircle(
            color = color,
            radius = r,
            center = Offset(cx, cy)
        )

        // Draw shiny leather lighting highlights
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = r * 0.3f,
            center = Offset(cx - r * 0.3f, cy - r * 0.3f)
        )

        // Draw the classic stitched seam (white thread curve)
        drawArc(
            color = Color.White.copy(alpha = 0.85f),
            startAngle = -60f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(cx - r * 1.2f, cy - r * 1.0f),
            size = Size(r * 2.0f, r * 2.0f),
            style = Stroke(width = r * 0.14f)
        )

        // Draw stitched dashes along the seam for realism
        drawArc(
            color = Color.White.copy(alpha = 0.4f),
            startAngle = -60f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(cx - r * 1.1f, cy - r * 0.95f),
            size = Size(r * 1.8f, r * 1.8f),
            style = Stroke(width = r * 0.05f)
        )
    }
}

@Composable
fun SjlLogo(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val sizeMin = size.minDimension

        // 1. Draw Crossed Cricket Bats in Gold in the Background
        // Bat 1 (Top-Left to Bottom-Right)
        rotate(degrees = 45f, pivot = Offset(cx, cy)) {
            // Handle
            drawLine(
                color = Color(0xFFA16207),
                start = Offset(cx, cy - h * 0.45f),
                end = Offset(cx, cy - h * 0.1f),
                strokeWidth = w * 0.05f
            )
            // Blade
            drawRect(
                color = Color(0xFFEAB308), // Vibrant Gold
                topLeft = Offset(cx - w * 0.06f, cy - h * 0.1f),
                size = Size(w * 0.12f, h * 0.5f)
            )
        }

        // Bat 2 (Top-Right to Bottom-Left)
        rotate(degrees = -45f, pivot = Offset(cx, cy)) {
            // Handle
            drawLine(
                color = Color(0xFFA16207),
                start = Offset(cx, cy - h * 0.45f),
                end = Offset(cx, cy - h * 0.1f),
                strokeWidth = w * 0.05f
            )
            // Blade
            drawRect(
                color = Color(0xFFEAB308), // Gold
                topLeft = Offset(cx - w * 0.06f, cy - h * 0.1f),
                size = Size(w * 0.12f, h * 0.5f)
            )
        }

        // 2. Draw Premium Black-and-Gold Shield Base
        val shieldWidth = sizeMin * 0.65f
        val shieldHeight = sizeMin * 0.75f
        val topY = cy - shieldHeight * 0.52f
        val bottomY = cy + shieldHeight * 0.48f

        // Draw outer gold shield outline
        val shieldPathOuter = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, topY) // Top center
            lineTo(cx + shieldWidth * 0.5f, topY + shieldHeight * 0.15f) // Top right corner
            lineTo(cx + shieldWidth * 0.5f, cy + shieldHeight * 0.15f) // Mid right
            cubicTo(
                cx + shieldWidth * 0.5f, cy + shieldHeight * 0.35f,
                cx + shieldWidth * 0.25f, bottomY,
                cx, bottomY
            ) // Curve to bottom tip
            cubicTo(
                cx - shieldWidth * 0.25f, bottomY,
                cx - shieldWidth * 0.5f, cy + shieldHeight * 0.35f,
                cx - shieldWidth * 0.5f, cy + shieldHeight * 0.15f
            ) // Curve to mid left
            lineTo(cx - shieldWidth * 0.5f, topY + shieldHeight * 0.15f) // Top left corner
            close()
        }
        drawPath(
            path = shieldPathOuter,
            color = Color(0xFFD97706) // Rich Amber Gold border
        )

        // Draw inner dark emerald-black filled shield
        val innerScale = 0.88f
        val innerWidth = shieldWidth * innerScale
        val innerHeight = shieldHeight * innerScale
        val innerTopY = cy - innerHeight * 0.52f
        val innerBottomY = cy + innerHeight * 0.48f

        val shieldPathInner = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, innerTopY)
            lineTo(cx + innerWidth * 0.5f, innerTopY + innerHeight * 0.15f)
            lineTo(cx + innerWidth * 0.5f, cy + innerHeight * 0.15f)
            cubicTo(
                cx + innerWidth * 0.5f, cy + innerHeight * 0.35f,
                cx + innerWidth * 0.25f, innerBottomY,
                cx, innerBottomY
            )
            cubicTo(
                cx - innerWidth * 0.25f, innerBottomY,
                cx - innerWidth * 0.5f, cy + innerHeight * 0.35f,
                cx - innerWidth * 0.5f, cy + innerHeight * 0.15f
            )
            lineTo(cx - innerWidth * 0.5f, innerTopY + innerHeight * 0.15f)
            close()
        }
        drawPath(
            path = shieldPathInner,
            color = Color(0xFF111827) // Midnight Charcoal Black background
        )

        // 3. Draw Red Cricket Ball in the Center of the Shield
        val ballRadius = sizeMin * 0.18f
        // Ball body
        drawCircle(
            color = Color(0xFFDC2626), // Athletic red
            radius = ballRadius,
            center = Offset(cx, cy)
        )
        // Seam highlight
        drawArc(
            color = Color.White.copy(alpha = 0.9f),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - ballRadius * 1.15f, cy - ballRadius * 0.9f),
            size = Size(ballRadius * 1.9f, ballRadius * 1.8f),
            style = Stroke(width = ballRadius * 0.15f)
        )

        // 4. Draw flanking Gold Stars
        val starRadius = sizeMin * 0.05f
        
        fun drawMiniStar(centerOffsetX: Float, centerOffsetY: Float) {
            val starPath = androidx.compose.ui.graphics.Path().apply {
                val scx = cx + centerOffsetX
                val scy = cy + centerOffsetY
                for (step in 0..4) {
                    val angle1 = (step * 2 * Math.PI / 5 - Math.PI / 2).toFloat()
                    val px1 = scx + starRadius * Math.cos(angle1.toDouble()).toFloat()
                    val py1 = scy + starRadius * Math.sin(angle1.toDouble()).toFloat()
                    if (step == 0) moveTo(px1, py1) else lineTo(px1, py1)

                    val angle2 = (step * 2 * Math.PI / 5 + Math.PI / 5 - Math.PI / 2).toFloat()
                    val px2 = scx + (starRadius * 0.4f) * Math.cos(angle2.toDouble()).toFloat()
                    val py2 = scy + (starRadius * 0.4f) * Math.sin(angle2.toDouble()).toFloat()
                    lineTo(px2, py2)
                }
                close()
            }
            drawPath(path = starPath, color = Color(0xFFFBBF24)) // Bright Amber Star
        }

        // Draw 3 stars: One top center, two flanking the mid sides
        drawMiniStar(0f, -shieldHeight * 0.38f)
        drawMiniStar(-shieldWidth * 0.65f, -shieldHeight * 0.05f)
        drawMiniStar(shieldWidth * 0.65f, -shieldHeight * 0.05f)
    }
}
