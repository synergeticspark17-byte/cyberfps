package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.model.WeaponType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders stylized 2D first-person weapons in 3D space with fluid bobbing,
 * recoil kicks, animated muzzle flashes, and glowing cyberpunk accents.
 */
@Composable
fun WeaponViewmodel(
    weapon: WeaponType,
    bobPhase: Float,
    recoil: Float,
    isFiring: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Calculate dynamic bobbing and recoil offset
        val bobX = sin(bobPhase) * (w * 0.02f)
        val bobY = cos(bobPhase * 2f) * (h * 0.015f)
        val recoilY = recoil * 3.5f

        val centerX = w * 0.5f + bobX
        val baseY = h + recoilY + bobY

        when (weapon) {
            WeaponType.BLASTER -> drawDualBlasters(centerX, baseY, w, h, isFiring)
            WeaponType.SHOTGUN -> drawScatterShotgun(centerX, baseY, w, h, isFiring)
            WeaponType.RAILGUN -> drawVaporRailgun(centerX, baseY, w, h, isFiring)
            WeaponType.KATANA -> drawCyberKatana(centerX, baseY, w, h, isFiring)
        }
    }
}

private fun DrawScope.drawDualBlasters(cx: Float, baseY: Float, w: Float, h: Float, isFiring: Boolean) {
    // Left Pistol
    drawSingleBlaster(cx - w * 0.16f, baseY, w * 0.14f, h * 0.35f, isLeft = true, isFiring = isFiring)
    // Right Pistol
    drawSingleBlaster(cx + w * 0.16f, baseY, w * 0.14f, h * 0.35f, isLeft = false, isFiring = isFiring)
}

private fun DrawScope.drawSingleBlaster(x: Float, baseY: Float, width: Float, height: Float, isLeft: Boolean, isFiring: Boolean) {
    val topY = baseY - height
    val p = Path().apply {
        moveTo(x - width * 0.4f, baseY)
        lineTo(x - width * 0.3f, topY + height * 0.3f)
        lineTo(x - width * 0.25f, topY)
        lineTo(x + width * 0.25f, topY)
        lineTo(x + width * 0.3f, topY + height * 0.3f)
        lineTo(x + width * 0.4f, baseY)
        close()
    }

    // Gun body
    drawPath(
        path = p,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF2C3247), Color(0xFF141724)),
            startY = topY,
            endY = baseY
        )
    )

    // Neon cyan sight rail
    drawLine(
        color = Color(0xFF00F5FF),
        start = Offset(x, topY),
        end = Offset(x, topY + height * 0.6f),
        strokeWidth = 6f
    )

    // Muzzle Flash if firing
    if (isFiring) {
        drawMuzzleFlash(x, topY - 15f, size = 60f, color = Color(0xFF00F5FF))
    }
}

private fun DrawScope.drawScatterShotgun(cx: Float, baseY: Float, w: Float, h: Float, isFiring: Boolean) {
    val gunWidth = w * 0.22f
    val gunHeight = h * 0.45f
    val topY = baseY - gunHeight

    // Heavy Double Barrel
    val barrelPath = Path().apply {
        moveTo(cx - gunWidth * 0.35f, baseY)
        lineTo(cx - gunWidth * 0.3f, topY)
        lineTo(cx + gunWidth * 0.3f, topY)
        lineTo(cx + gunWidth * 0.35f, baseY)
        close()
    }

    drawPath(
        path = barrelPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF4A4E69), Color(0xFF1C1D26)),
            startY = topY,
            endY = baseY
        )
    )

    // Glowing Orange Heat Conduits
    drawLine(
        color = Color(0xFFFF9900),
        start = Offset(cx - gunWidth * 0.15f, topY + 20f),
        end = Offset(cx - gunWidth * 0.15f, baseY - 40f),
        strokeWidth = 8f
    )
    drawLine(
        color = Color(0xFFFF9900),
        start = Offset(cx + gunWidth * 0.15f, topY + 20f),
        end = Offset(cx + gunWidth * 0.15f, baseY - 40f),
        strokeWidth = 8f
    )

    // Twin Muzzle Ports
    drawCircle(Color(0xFF111111), radius = 18f, center = Offset(cx - 24f, topY + 10f))
    drawCircle(Color(0xFF111111), radius = 18f, center = Offset(cx + 24f, topY + 10f))

    if (isFiring) {
        drawMuzzleFlash(cx, topY - 20f, size = 110f, color = Color(0xFFFF6600))
    }
}

private fun DrawScope.drawVaporRailgun(cx: Float, baseY: Float, w: Float, h: Float, isFiring: Boolean) {
    val gunWidth = w * 0.16f
    val gunHeight = h * 0.52f
    val topY = baseY - gunHeight

    // Sleek Rail Rifle Barrel
    val railPath = Path().apply {
        moveTo(cx - gunWidth * 0.25f, baseY)
        lineTo(cx - gunWidth * 0.12f, topY)
        lineTo(cx + gunWidth * 0.12f, topY)
        lineTo(cx + gunWidth * 0.25f, baseY)
        close()
    }

    drawPath(
        path = railPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1A1A2E), Color(0xFF0F0F1B)),
            startY = topY,
            endY = baseY
        )
    )

    // Magnetic Accelerator Rings (Glowing Magenta)
    for (i in 1..4) {
        val ringY = topY + i * 45f
        drawLine(
            color = Color(0xFFFF007F),
            start = Offset(cx - gunWidth * 0.2f, ringY),
            end = Offset(cx + gunWidth * 0.2f, ringY),
            strokeWidth = 6f
        )
    }

    if (isFiring) {
        drawMuzzleFlash(cx, topY - 30f, size = 130f, color = Color(0xFFFF00D4))
    }
}

private fun DrawScope.drawCyberKatana(cx: Float, baseY: Float, w: Float, h: Float, isFiring: Boolean) {
    val slashOffset = if (isFiring) -w * 0.15f else 0f
    val bladeStart = Offset(cx + w * 0.22f + slashOffset, baseY - 40f)
    val bladeTip = Offset(cx - w * 0.12f + slashOffset, baseY - h * 0.48f)

    // Katana Neon Blade
    drawLine(
        color = Color(0xFFFF007F),
        start = bladeStart,
        end = bladeTip,
        strokeWidth = 14f
    )
    // Core white light
    drawLine(
        color = Color.White,
        start = bladeStart,
        end = bladeTip,
        strokeWidth = 5f
    )

    // Cyber Hilt
    drawLine(
        color = Color(0xFF222638),
        start = bladeStart,
        end = Offset(bladeStart.x + 35f, bladeStart.y + 70f),
        strokeWidth = 18f
    )

    if (isFiring) {
        // Neon Slash Arc
        val slashPath = Path().apply {
            moveTo(bladeTip.x - 60f, bladeTip.y - 20f)
            quadraticTo(
                bladeTip.x + 100f, bladeTip.y + 40f,
                bladeStart.x + 80f, bladeStart.y - 40f
            )
        }
        drawPath(
            path = slashPath,
            color = Color(0x9900F5FF)
        )
    }
}

private fun DrawScope.drawMuzzleFlash(x: Float, y: Float, size: Float, color: Color) {
    // 8-point bright flash star
    val starPath = Path().apply {
        moveTo(x, y - size)
        lineTo(x + size * 0.25f, y - size * 0.25f)
        lineTo(x + size, y)
        lineTo(x + size * 0.25f, y + size * 0.25f)
        lineTo(x, y + size)
        lineTo(x - size * 0.25f, y + size * 0.25f)
        lineTo(x - size, y)
        lineTo(x - size * 0.25f, y - size * 0.25f)
        close()
    }

    drawPath(path = starPath, color = color)
    drawCircle(Color.White, radius = size * 0.35f, center = Offset(x, y))
}
