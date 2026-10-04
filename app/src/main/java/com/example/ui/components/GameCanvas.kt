package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import com.example.game.RacingGameEngine
import com.example.model.Collectible
import com.example.model.CollectibleType
import com.example.model.ParticleType
import com.example.model.TrackTheme
import com.example.model.TrafficVehicle
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun GameCanvas(
  engine: RacingGameEngine,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val canvasW = size.width
    val canvasH = size.height
    val centerX = canvasW * 0.5f

    // Road dimensions
    val roadWidth = canvasW * 0.72f
    val roadLeft = centerX - roadWidth * 0.5f
    val roadRight = centerX + roadWidth * 0.5f

    // Camera screen shake
    val shakeX = if (engine.cameraShake > 0f) (sin(engine.playerY * 0.8f) * engine.cameraShake * 2f) else 0f
    val shakeY = if (engine.cameraShake > 0f) (sin(engine.playerY * 1.2f) * engine.cameraShake * 2f) else 0f

    // Draw Backdrop / Scenery
    drawTrackBackground(engine.theme, canvasW, canvasH, engine.playerY)

    // Draw Curving Asphalt Road & Rumble Strips
    drawRoad(
      engine = engine,
      roadLeft = roadLeft + shakeX,
      roadRight = roadRight + shakeX,
      roadWidth = roadWidth,
      canvasH = canvasH
    )

    // Draw Collectibles
    for (col in engine.collectibles) {
      drawCollectible(col, engine, roadLeft + shakeX, roadRight + shakeX, canvasH)
    }

    // Draw Traffic & Rival Vehicles
    for (traffic in engine.trafficList) {
      drawTrafficVehicle(traffic, engine, roadLeft + shakeX, roadRight + shakeX, canvasH)
    }

    // Draw Particles (behind and around player)
    drawParticles(engine, canvasW, canvasH, roadLeft + shakeX, roadRight + shakeX)

    // Draw Player Car
    drawPlayerCar(engine, roadLeft + shakeX, roadRight + shakeX, canvasH)

    // Draw Speed Lines if going super fast
    if (engine.playerSpeed > 180f || engine.isNitroActive) {
      drawSpeedLines(engine.playerSpeed, engine.isNitroActive, canvasW, canvasH, engine.playerY)
    }

    // Draw Floating Text Popups
    drawFloatingMessages(engine, roadLeft + shakeX, roadRight + shakeX, canvasH)
  }
}

private fun DrawScope.drawTrackBackground(
  theme: TrackTheme,
  width: Float,
  height: Float,
  playerY: Float
) {
  // Atmospheric Sky Gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(Color(theme.skyColorTop), Color(theme.skyColorBottom)),
      startY = 0f,
      endY = height
    ),
    topLeft = Offset.Zero,
    size = Size(width, height)
  )

  // Roadside Neon Buildings / Desert Dunes / Scenery parallax
  val sceneryShift = (playerY * 0.25f) % 180f
  for (i in 0..12) {
    val segY = i * 180f - sceneryShift
    // Left decorative structures
    drawRect(
      color = Color(theme.shoulderColor).copy(alpha = 0.85f),
      topLeft = Offset(0f, segY),
      size = Size(width * 0.12f, 150f)
    )
    // Left neon edge
    drawLine(
      color = Color(theme.barrierColor).copy(alpha = 0.5f),
      start = Offset(width * 0.12f, segY),
      end = Offset(width * 0.12f, segY + 150f),
      strokeWidth = 3f
    )
    // Right decorative structures
    drawRect(
      color = Color(theme.shoulderColor).copy(alpha = 0.85f),
      topLeft = Offset(width * 0.88f, segY),
      size = Size(width * 0.12f, 150f)
    )
    // Right neon edge
    drawLine(
      color = Color(theme.barrierColor).copy(alpha = 0.5f),
      start = Offset(width * 0.88f, segY),
      end = Offset(width * 0.88f, segY + 150f),
      strokeWidth = 3f
    )
  }
}

private fun DrawScope.drawRoad(
  engine: RacingGameEngine,
  roadLeft: Float,
  roadRight: Float,
  roadWidth: Float,
  canvasH: Float
) {
  val theme = engine.theme

  // Main asphalt surface
  drawRect(
    color = Color(theme.roadColor),
    topLeft = Offset(roadLeft, 0f),
    size = Size(roadWidth, canvasH)
  )

  // Rumble Strips / Curbs (alternating segments)
  val curbWidth = 14f
  val segLength = 40f
  val scrollOffset = (engine.playerY * 4.5f) % (segLength * 2f)

  var curY = -scrollOffset
  var segIndex = 0
  while (curY < canvasH) {
    val isRed = (segIndex % 2 == 0)
    val curbColor = if (isRed) Color(0xFFFF1744) else Color(0xFFEEEEEE)

    // Left curb
    drawRect(
      color = curbColor,
      topLeft = Offset(roadLeft - curbWidth, curY),
      size = Size(curbWidth, segLength)
    )
    // Right curb
    drawRect(
      color = curbColor,
      topLeft = Offset(roadRight, curY),
      size = Size(curbWidth, segLength)
    )

    curY += segLength
    segIndex++
  }

  // Guardrail barrier glow lines
  drawLine(
    color = Color(theme.barrierColor),
    start = Offset(roadLeft - curbWidth, 0f),
    end = Offset(roadLeft - curbWidth, canvasH),
    strokeWidth = 5f
  )
  drawLine(
    color = Color(theme.barrierColor),
    start = Offset(roadRight + curbWidth, 0f),
    end = Offset(roadRight + curbWidth, canvasH),
    strokeWidth = 5f
  )

  // Dashed Lane Markings
  val numLanes = engine.numLanes
  val dashLength = 35f
  val dashGap = 25f
  val dashTotal = dashLength + dashGap
  val laneScroll = (engine.playerY * 5f) % dashTotal

  for (lane in 1 until numLanes) {
    val laneX = roadLeft + (roadWidth / numLanes) * lane
    var dashY = -laneScroll
    while (dashY < canvasH) {
      drawLine(
        color = Color(theme.lineColor).copy(alpha = 0.75f),
        start = Offset(laneX, dashY),
        end = Offset(laneX, dashY + dashLength),
        strokeWidth = 4f
      )
      dashY += dashTotal
    }
  }
}

private fun DrawScope.drawPlayerCar(
  engine: RacingGameEngine,
  roadLeft: Float,
  roadRight: Float,
  canvasH: Float
) {
  val roadCenterX = (roadLeft + roadRight) * 0.5f
  val roadWidth = roadRight - roadLeft

  // Player position on screen
  val playerScreenX = roadCenterX + (engine.playerX * roadWidth * 0.5f)
  val playerScreenY = canvasH * 0.75f

  val carWidth = 44f
  val carHeight = 76f
  val jumpScale = 1f + engine.airTimeHeight * 0.12f

  // Drop Shadow underneath car
  val shadowOffsetY = 6f + engine.airTimeHeight * 14f
  drawRoundRect(
    color = Color.Black.copy(alpha = (0.45f - engine.airTimeHeight * 0.08f).coerceAtLeast(0.1f)),
    topLeft = Offset(playerScreenX - carWidth * 0.5f, playerScreenY - carHeight * 0.5f + shadowOffsetY),
    size = Size(carWidth, carHeight),
    cornerRadius = CornerRadius(12f, 12f)
  )

  // Neon Underglow
  drawRoundRect(
    color = Color(engine.customNeonColor).copy(alpha = if (engine.isNitroActive) 0.9f else 0.5f),
    topLeft = Offset(playerScreenX - carWidth * 0.65f, playerScreenY - carHeight * 0.55f),
    size = Size(carWidth * 1.3f, carHeight * 1.1f),
    cornerRadius = CornerRadius(18f, 18f)
  )

  // Headlight Light Beams shining forward on asphalt
  val beamPath = Path().apply {
    moveTo(playerScreenX - carWidth * 0.35f, playerScreenY - carHeight * 0.45f)
    lineTo(playerScreenX - carWidth * 1.2f, playerScreenY - carHeight * 3.5f)
    lineTo(playerScreenX + carWidth * 1.2f, playerScreenY - carHeight * 3.5f)
    lineTo(playerScreenX + carWidth * 0.35f, playerScreenY - carHeight * 0.45f)
    close()
  }
  drawPath(
    path = beamPath,
    brush = Brush.verticalGradient(
      colors = listOf(Color(0x35FFFFFF), Color(0x00FFFFFF)),
      startY = playerScreenY - carHeight * 3.5f,
      endY = playerScreenY - carHeight * 0.45f
    )
  )

  // Draw Car Body with rotation and scale for jumping / drifting
  rotate(degrees = engine.driftAngle, pivot = Offset(playerScreenX, playerScreenY)) {
    scale(scale = jumpScale, pivot = Offset(playerScreenX, playerScreenY)) {
      val carBodyColor = Color(engine.customBodyColor)

      // Tires (4 corners)
      val tireW = 8f
      val tireH = 18f
      val tireColor = Color(0xFF1E232A)
      // Front Left
      drawRoundRect(tireColor, Offset(playerScreenX - carWidth * 0.52f, playerScreenY - carHeight * 0.42f), Size(tireW, tireH), CornerRadius(3f, 3f))
      // Front Right
      drawRoundRect(tireColor, Offset(playerScreenX + carWidth * 0.52f - tireW, playerScreenY - carHeight * 0.42f), Size(tireW, tireH), CornerRadius(3f, 3f))
      // Rear Left
      drawRoundRect(tireColor, Offset(playerScreenX - carWidth * 0.52f, playerScreenY + carHeight * 0.22f), Size(tireW, tireH), CornerRadius(3f, 3f))
      // Rear Right
      drawRoundRect(tireColor, Offset(playerScreenX + carWidth * 0.52f - tireW, playerScreenY + carHeight * 0.22f), Size(tireW, tireH), CornerRadius(3f, 3f))

      // Main Chassis Body
      drawRoundRect(
        color = carBodyColor,
        topLeft = Offset(playerScreenX - carWidth * 0.45f, playerScreenY - carHeight * 0.48f),
        size = Size(carWidth * 0.9f, carHeight * 0.96f),
        cornerRadius = CornerRadius(14f, 14f)
      )

      // Racing Stripes down center
      drawRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(playerScreenX - 3f, playerScreenY - carHeight * 0.48f),
        size = Size(6f, carHeight * 0.96f)
      )

      // Windshield & Cockpit Glass (Dark tinted)
      val glassColor = Color(0xFF101924)
      drawRoundRect(
        color = glassColor,
        topLeft = Offset(playerScreenX - carWidth * 0.32f, playerScreenY - carHeight * 0.22f),
        size = Size(carWidth * 0.64f, carHeight * 0.42f),
        cornerRadius = CornerRadius(8f, 8f)
      )

      // Rear Spoiler Wing
      drawRoundRect(
        color = Color(0xFF151515),
        topLeft = Offset(playerScreenX - carWidth * 0.48f, playerScreenY + carHeight * 0.38f),
        size = Size(carWidth * 0.96f, 10f),
        cornerRadius = CornerRadius(4f, 4f)
      )

      // Headlights (Twin bright white/cyan LEDs)
      val ledColor = Color(0xFFE0F7FA)
      drawCircle(ledColor, 4.5f, Offset(playerScreenX - carWidth * 0.3f, playerScreenY - carHeight * 0.45f))
      drawCircle(ledColor, 4.5f, Offset(playerScreenX + carWidth * 0.3f, playerScreenY - carHeight * 0.45f))

      // Taillights (Red LED bar)
      val tailColor = if (engine.brakeInput > 0f) Color(0xFFFF1744) else Color(0xFFB71C1C)
      drawRoundRect(
        color = tailColor,
        topLeft = Offset(playerScreenX - carWidth * 0.38f, playerScreenY + carHeight * 0.44f),
        size = Size(carWidth * 0.76f, 5f),
        cornerRadius = CornerRadius(2f, 2f)
      )

      // Nitro Exhaust Flames when boosting
      if (engine.isNitroActive) {
        val flameLen = 22f + (sin(engine.playerY * 20f) * 6f)
        // Left exhaust flame
        drawOval(
          brush = Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF6D00))),
          topLeft = Offset(playerScreenX - 14f, playerScreenY + carHeight * 0.48f),
          size = Size(8f, flameLen)
        )
        // Right exhaust flame
        drawOval(
          brush = Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF6D00))),
          topLeft = Offset(playerScreenX + 6f, playerScreenY + carHeight * 0.48f),
          size = Size(8f, flameLen)
        )
      }

      // Energy Shield Bubble if active
      if (engine.shieldTimer > 0f) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(Color(0x0000E5FF), Color(0x6600E5FF)),
            center = Offset(playerScreenX, playerScreenY),
            radius = carHeight * 0.8f
          ),
          radius = carHeight * 0.8f,
          center = Offset(playerScreenX, playerScreenY)
        )
      }
    }
  }
}

private fun DrawScope.drawTrafficVehicle(
  traffic: TrafficVehicle,
  engine: RacingGameEngine,
  roadLeft: Float,
  roadRight: Float,
  canvasH: Float
) {
  val roadCenterX = (roadLeft + roadRight) * 0.5f
  val roadWidth = roadRight - roadLeft
  val playerScreenY = canvasH * 0.75f

  // Calculate screen Y relative to player
  val relDistY = traffic.y - engine.playerY
  val screenY = playerScreenY - (relDistY * 6f)

  // Only render if visible on screen
  if (screenY < -100f || screenY > canvasH + 100f) return

  val screenX = roadCenterX + (traffic.x * roadWidth * 0.5f)

  val carW = if (traffic.carType == 2) 52f else 42f // Trucks are wider
  val carH = if (traffic.carType == 2) 95f else 72f // Trucks are longer

  // Shadow
  drawRoundRect(
    color = Color.Black.copy(alpha = 0.4f),
    topLeft = Offset(screenX - carW * 0.5f, screenY - carH * 0.5f + 4f),
    size = Size(carW, carH),
    cornerRadius = CornerRadius(10f, 10f)
  )

  // Car Body
  val bodyColor = Color(traffic.colorHex)
  drawRoundRect(
    color = bodyColor,
    topLeft = Offset(screenX - carW * 0.46f, screenY - carH * 0.48f),
    size = Size(carW * 0.92f, carH * 0.96f),
    cornerRadius = CornerRadius(12f, 12f)
  )

  // Windshield & Windows
  drawRoundRect(
    color = Color(0xFF161F2E),
    topLeft = Offset(screenX - carW * 0.32f, screenY - carH * 0.22f),
    size = Size(carW * 0.64f, carH * 0.38f),
    cornerRadius = CornerRadius(6f, 6f)
  )

  // Headlights (facing forward / up)
  drawCircle(Color(0xFFFFF9C4), 4f, Offset(screenX - carW * 0.3f, screenY - carH * 0.44f))
  drawCircle(Color(0xFFFFF9C4), 4f, Offset(screenX + carW * 0.3f, screenY - carH * 0.44f))

  // Taillights
  drawRoundRect(
    color = Color(0xFFD32F2F),
    topLeft = Offset(screenX - carW * 0.35f, screenY + carH * 0.44f),
    size = Size(carW * 0.7f, 4f),
    cornerRadius = CornerRadius(2f, 2f)
  )

  // Police Siren Lights
  if (traffic.isPolice) {
    val phase = (traffic.sirenPhase % 2f) < 1f
    val redColor = if (phase) Color(0xFFFF1744) else Color(0x33FF1744)
    val blueColor = if (!phase) Color(0xFF2979FF) else Color(0x332979FF)
    drawCircle(redColor, 6f, Offset(screenX - 10f, screenY))
    drawCircle(blueColor, 6f, Offset(screenX + 10f, screenY))
  }

  // Rival Racer Rank Badge
  if (traffic.isRivalRacer) {
    drawCircle(Color(0xFFFFD600), 10f, Offset(screenX, screenY))
  }
}

private fun DrawScope.drawCollectible(
  col: Collectible,
  engine: RacingGameEngine,
  roadLeft: Float,
  roadRight: Float,
  canvasH: Float
) {
  val roadCenterX = (roadLeft + roadRight) * 0.5f
  val roadWidth = roadRight - roadLeft
  val playerScreenY = canvasH * 0.75f

  val relDistY = col.y - engine.playerY
  val screenY = playerScreenY - (relDistY * 6f)

  if (screenY < -60f || screenY > canvasH + 60f) return

  val screenX = roadCenterX + (col.x * roadWidth * 0.5f)

  when (col.type) {
    CollectibleType.COIN -> {
      // Golden Coin with inner circle
      val pulse = 14f + (sin(engine.playerY * 0.5f) * 2f)
      drawCircle(Color(0xFFFFD600), pulse, Offset(screenX, screenY))
      drawCircle(Color(0xFFFFA000), pulse * 0.65f, Offset(screenX, screenY))
    }
    CollectibleType.NITRO_CANISTER -> {
      // Blue Nitro tank
      drawRoundRect(
        color = Color(0xFF00E5FF),
        topLeft = Offset(screenX - 12f, screenY - 18f),
        size = Size(24f, 36f),
        cornerRadius = CornerRadius(6f, 6f)
      )
      drawRect(Color.White, Offset(screenX - 6f, screenY - 24f), Size(12f, 6f))
    }
    CollectibleType.SHIELD -> {
      // Cyan Shield orb
      drawCircle(Color(0xFF00E5FF), 18f, Offset(screenX, screenY))
      drawCircle(Color.White.copy(alpha = 0.7f), 10f, Offset(screenX, screenY))
    }
    CollectibleType.REPAIR_WRENCH -> {
      // Emerald wrench / plus sign
      drawCircle(Color(0xFF00E676), 18f, Offset(screenX, screenY))
      drawRect(Color.White, Offset(screenX - 4f, screenY - 10f), Size(8f, 20f))
      drawRect(Color.White, Offset(screenX - 10f, screenY - 4f), Size(20f, 8f))
    }
    CollectibleType.SPEED_RAMP -> {
      // Speed launch pad with yellow/orange chevron arrows
      val rampW = 55f
      val rampH = 32f
      drawRoundRect(
        color = Color(0xFFFF6D00),
        topLeft = Offset(screenX - rampW * 0.5f, screenY - rampH * 0.5f),
        size = Size(rampW, rampH),
        cornerRadius = CornerRadius(6f, 6f)
      )
      // Chevron arrow pointing up
      val arrow = Path().apply {
        moveTo(screenX, screenY - 10f)
        lineTo(screenX + 16f, screenY + 8f)
        lineTo(screenX, screenY + 2f)
        lineTo(screenX - 16f, screenY + 8f)
        close()
      }
      drawPath(arrow, Color(0xFFFFD600))
    }
    CollectibleType.DOUBLE_CASH -> {
      drawCircle(Color(0xFFFFD600), 18f, Offset(screenX, screenY))
      drawCircle(Color(0xFFFF5722), 9f, Offset(screenX, screenY))
    }
    CollectibleType.MAGNET -> {
      drawCircle(Color(0xFFD500F9), 18f, Offset(screenX, screenY))
      drawCircle(Color.White, 9f, Offset(screenX, screenY))
    }
  }
}

private fun DrawScope.drawParticles(
  engine: RacingGameEngine,
  width: Float,
  height: Float,
  roadLeft: Float,
  roadRight: Float
) {
  val roadCenterX = (roadLeft + roadRight) * 0.5f
  val roadWidth = roadRight - roadLeft
  val playerScreenY = height * 0.75f

  for (p in engine.particles) {
    val px = if (p.type == ParticleType.RAIN || p.type == ParticleType.DUST) {
      (p.x * width * 0.5f) + (width * 0.5f)
    } else {
      roadCenterX + (p.x * roadWidth * 0.5f)
    }

    val py = if (p.type == ParticleType.RAIN || p.type == ParticleType.DUST) {
      (p.y * height)
    } else {
      playerScreenY + (p.y * 50f)
    }

    val color = Color(p.color).copy(alpha = p.alpha)

    when (p.type) {
      ParticleType.RAIN -> {
        drawLine(color, Offset(px, py), Offset(px - 4f, py + 18f), strokeWidth = 2.5f)
      }
      else -> {
        drawCircle(color, p.radius, Offset(px, py))
      }
    }
  }
}

private fun DrawScope.drawSpeedLines(
  speed: Float,
  isNitro: Boolean,
  width: Float,
  height: Float,
  playerY: Float
) {
  val count = if (isNitro) 24 else 12
  val lineColor = if (isNitro) Color(0x6600E5FF) else Color(0x33FFFFFF)
  for (i in 0 until count) {
    val side = if (i % 2 == 0) 0f else width
    val offset = (playerY * 12f + i * 140f) % height
    val length = 80f + (speed * 0.4f)
    val startX = if (side == 0f) side + (i * 3f) else side - (i * 3f)
    val endX = if (side == 0f) startX + 25f else startX - 25f
    drawLine(
      color = lineColor,
      start = Offset(startX, offset),
      end = Offset(endX, offset + length),
      strokeWidth = 3f
    )
  }
}

private fun DrawScope.drawFloatingMessages(
  engine: RacingGameEngine,
  roadLeft: Float,
  roadRight: Float,
  canvasH: Float
) {
  val roadCenterX = (roadLeft + roadRight) * 0.5f
  val roadWidth = roadRight - roadLeft
  val paint = android.graphics.Paint().apply {
    textAlign = android.graphics.Paint.Align.CENTER
    textSize = 42f
    isFakeBoldText = true
  }

  for (msg in engine.floatingMessages) {
    val sx = roadCenterX + (msg.x * roadWidth * 0.5f)
    val sy = canvasH * msg.y
    val alpha = (msg.life / msg.maxLife).coerceIn(0f, 1f)

    drawContext.canvas.nativeCanvas.apply {
      // Glow shadow
      paint.color = android.graphics.Color.BLACK
      paint.alpha = (alpha * 200).toInt()
      drawText(msg.text, sx + 2f, sy + 2f, paint)

      // Foreground colored text
      paint.color = msg.color.toInt()
      paint.alpha = (alpha * 255).toInt()
      drawText(msg.text, sx, sy, paint)
    }
  }
}
