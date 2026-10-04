package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.RacingGameEngine
import com.example.model.GameMode
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink

@Composable
fun GameHud(
  engine: RacingGameEngine,
  onPauseClick: () -> Unit,
  isTouchDragMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  Box(modifier = modifier.fillMaxSize()) {
    // -------------------------------------------------------------
    // TOP BAR: Laps/Distance, Score, Cash, Combo, Powerups, Pause
    // -------------------------------------------------------------
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(horizontal = 16.dp, vertical = 36.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Distance or Grand Prix Lap
        Box(
          modifier = Modifier
            .background(DarkSurface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          if (engine.mode == GameMode.CAREER_GRAND_PRIX) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "LAP ${engine.currentLap}/${engine.totalLaps}",
                color = NeonGold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "POS: ${engine.playerRaceRank}/8",
                color = if (engine.playerRaceRank == 1) NeonGreen else NeonCyan,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
              )
            }
          } else {
            Text(
              text = "${engine.playerY.toInt()} m",
              color = NeonCyan,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        // Combo Multiplier Badge
        if (engine.comboMultiplier > 1) {
          Box(
            modifier = Modifier
              .scale(pulseScale)
              .background(
                brush = Brush.horizontalGradient(listOf(NeonOrange, NeonPink)),
                shape = RoundedCornerShape(14.dp)
              )
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = "${engine.comboMultiplier}x COMBO",
              color = Color.White,
              fontWeight = FontWeight.Black,
              fontSize = 13.sp
            )
          }
        }

        // Cash & Pause Button
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .background(DarkSurface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
              .border(1.dp, NeonGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "$ ${engine.cashEarned}",
              color = NeonGold,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(
            onClick = onPauseClick,
            modifier = Modifier
              .size(40.dp)
              .background(DarkSurface.copy(alpha = 0.85f), CircleShape)
              .border(1.dp, Color(0xFF2D3748), CircleShape)
              .testTag("pause_button")
          ) {
            Icon(
              imageVector = Icons.Default.Pause,
              contentDescription = "Pause Game",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // Active Powerups Row (Shield, Magnet, 2x Cash)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Start
      ) {
        if (engine.shieldTimer > 0f) {
          PowerUpBadge(
            label = "SHIELD",
            seconds = engine.shieldTimer.toInt(),
            color = NeonCyan,
            icon = Icons.Default.Shield
          )
        }
        if (engine.magnetTimer > 0f) {
          Spacer(modifier = Modifier.width(6.dp))
          PowerUpBadge(
            label = "MAGNET",
            seconds = engine.magnetTimer.toInt(),
            color = Color(0xFFD500F9),
            icon = Icons.Default.ElectricBolt
          )
        }
        if (engine.doubleCashTimer > 0f) {
          Spacer(modifier = Modifier.width(6.dp))
          PowerUpBadge(
            label = "2X CASH",
            seconds = engine.doubleCashTimer.toInt(),
            color = NeonGold,
            icon = Icons.Default.Speed
          )
        }
      }
    }

    // -------------------------------------------------------------
    // MID-LEVEL GAUGES: Speedometer & RPM, Nitro, Health
    // -------------------------------------------------------------
    Column(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 16.dp, top = 110.dp)
    ) {
      // Speedometer & Gear Box
      Box(
        modifier = Modifier
          .background(DarkSurface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
          .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Column {
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "${engine.playerSpeed.toInt()}",
              color = Color.White,
              fontWeight = FontWeight.Black,
              fontSize = 28.sp,
              fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "KM/H",
              color = NeonCyan,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
              modifier = Modifier
                .background(NeonOrange.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "GEAR ${engine.currentGear}",
                color = NeonOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // RPM Tachometer Bar
          LinearProgressIndicator(
            progress = { engine.currentRpm },
            modifier = Modifier
              .width(130.dp)
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = if (engine.currentRpm > 0.85f) NeonPink else if (engine.currentRpm > 0.65f) NeonGold else NeonGreen,
            trackColor = Color(0xFF21262D)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Health Bar
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "HP",
          color = Color(0xFF8B949E),
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        val hpRatio = (engine.health / engine.maxHealth).coerceIn(0f, 1f)
        LinearProgressIndicator(
          progress = { hpRatio },
          modifier = Modifier
            .width(80.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = if (hpRatio > 0.4f) NeonGreen else NeonPink,
          trackColor = Color(0xFF21262D)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Nitro Fuel Bar
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "NOS",
          color = NeonCyan,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
        val nosRatio = (engine.nitroFuel / engine.maxNitro).coerceIn(0f, 1f)
        LinearProgressIndicator(
          progress = { nosRatio },
          modifier = Modifier
            .width(80.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = NeonCyan,
          trackColor = Color(0xFF21262D)
        )
      }
    }

    // -------------------------------------------------------------
    // BOTTOM CONTROLS: Touch Steering, Pedals, Nitro, Drift
    // -------------------------------------------------------------
    if (isTouchDragMode) {
      // Touch Drag Steering Zone overlay
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectDragGestures(
              onDragStart = {},
              onDrag = { change, dragAmount ->
                change.consume()
                // Steer left or right with drag
                val normalizedDrag = dragAmount.x / 100f
                engine.steerInput = (engine.steerInput + normalizedDrag).coerceIn(-1f, 1f)
                engine.throttleInput = 1f
              },
              onDragEnd = {
                engine.steerInput = 0f
              }
            )
          }
      )
    }

    // Bottom On-Screen Buttons
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
      // Left: Steering Buttons
      Row(
        modifier = Modifier.align(Alignment.BottomStart),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Steer Left Button
        ControlButton(
          icon = Icons.AutoMirrored.Filled.ArrowBack,
          testTag = "steer_left_button",
          onPress = { engine.steerInput = -1f },
          onRelease = { if (engine.steerInput < 0f) engine.steerInput = 0f }
        )

        // Steer Right Button
        ControlButton(
          icon = Icons.AutoMirrored.Filled.ArrowForward,
          testTag = "steer_right_button",
          onPress = { engine.steerInput = 1f },
          onRelease = { if (engine.steerInput > 0f) engine.steerInput = 0f }
        )
      }

      // Center: Drift Button
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 6.dp)
      ) {
        DriftButton(
          isDrifting = engine.isDrifting,
          onPress = { engine.isDrifting = true },
          onRelease = { engine.isDrifting = false }
        )
      }

      // Right: Pedals & Nitro
      Row(
        modifier = Modifier.align(Alignment.BottomEnd),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
      ) {
        // Brake Pedal
        PedalButton(
          label = "BRAKE",
          color = NeonPink,
          testTag = "brake_button",
          onPress = { engine.brakeInput = 1f },
          onRelease = { engine.brakeInput = 0f }
        )

        // Nitro Button
        NitroButton(
          nitroFuel = engine.nitroFuel,
          isActive = engine.isNitroActive,
          onClick = { engine.triggerNitro() }
        )

        // Gas Pedal
        PedalButton(
          label = "GAS",
          color = NeonGreen,
          testTag = "gas_button",
          isPrimary = true,
          onPress = { engine.throttleInput = 1f },
          onRelease = { engine.throttleInput = 0f }
        )
      }
    }
  }
}

@Composable
private fun PowerUpBadge(
  label: String,
  seconds: Int,
  color: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector
) {
  Box(
    modifier = Modifier
      .background(DarkSurface.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
      .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(text = "$label: ${seconds}s", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun ControlButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  testTag: String,
  onPress: () -> Unit,
  onRelease: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(62.dp)
      .background(DarkSurface.copy(alpha = 0.8f), CircleShape)
      .border(2.dp, Color(0xFF2D3748), CircleShape)
      .testTag(testTag)
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            onPress()
            tryAwaitRelease()
            onRelease()
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = testTag,
      tint = Color.White,
      modifier = Modifier.size(32.dp)
    )
  }
}

@Composable
private fun DriftButton(
  isDrifting: Boolean,
  onPress: () -> Unit,
  onRelease: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(52.dp)
      .background(
        color = if (isDrifting) NeonOrange.copy(alpha = 0.8f) else DarkSurface.copy(alpha = 0.8f),
        shape = CircleShape
      )
      .border(2.dp, if (isDrifting) NeonOrange else Color(0xFF2D3748), CircleShape)
      .testTag("drift_button")
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            onPress()
            tryAwaitRelease()
            onRelease()
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "DRIFT",
      color = if (isDrifting) Color.White else NeonOrange,
      fontWeight = FontWeight.Black,
      fontSize = 11.sp
    )
  }
}

@Composable
private fun PedalButton(
  label: String,
  color: Color,
  testTag: String,
  isPrimary: Boolean = false,
  onPress: () -> Unit,
  onRelease: () -> Unit
) {
  val width = if (isPrimary) 62.dp else 52.dp
  val height = if (isPrimary) 76.dp else 64.dp

  Box(
    modifier = Modifier
      .size(width = width, height = height)
      .background(DarkSurface.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
      .border(2.dp, color.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
      .testTag(testTag)
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            onPress()
            tryAwaitRelease()
            onRelease()
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = color,
      fontWeight = FontWeight.Black,
      fontSize = if (isPrimary) 14.sp else 12.sp
    )
  }
}

@Composable
private fun NitroButton(
  nitroFuel: Float,
  isActive: Boolean,
  onClick: () -> Unit
) {
  val canUse = nitroFuel > 15f

  Box(
    modifier = Modifier
      .size(58.dp)
      .background(
        brush = if (isActive) {
          Brush.radialGradient(listOf(NeonCyan, Color(0xFF0091EA)))
        } else {
          Brush.radialGradient(listOf(DarkSurface, Color(0xFF131821)))
        },
        shape = CircleShape
      )
      .border(
        width = 2.dp,
        color = if (canUse) NeonCyan else Color(0xFF2D3748),
        shape = CircleShape
      )
      .testTag("nitro_button")
      .pointerInput(canUse) {
        if (canUse) {
          detectTapGestures(onTap = { onClick() })
        }
      },
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.ElectricBolt,
        contentDescription = "Nitro Boost",
        tint = if (canUse) NeonCyan else Color(0xFF484F58),
        modifier = Modifier.size(20.dp)
      )
      Text(
        text = "NOS",
        color = if (canUse) Color.White else Color(0xFF484F58),
        fontWeight = FontWeight.Black,
        fontSize = 10.sp
      )
    }
  }
}
