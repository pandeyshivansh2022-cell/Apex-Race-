package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AudioEngine
import com.example.data.RaceRepository
import com.example.game.RacingGameEngine
import com.example.model.CarCatalog
import com.example.model.GameMode
import com.example.model.TrackTheme
import com.example.ui.components.GameCanvas
import com.example.ui.components.GameHud
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink

@Composable
fun RacingScreen(
  mode: GameMode,
  theme: TrackTheme,
  repository: RaceRepository,
  audioEngine: AudioEngine,
  onExitToMenu: () -> Unit,
  onGoToGarage: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val vibrator = remember { context.getSystemService(Vibrator::class.java) }
  val hapticsEnabled = remember { repository.isHapticsEnabled() }
  val touchDragMode = remember { repository.isTouchDragControls() }

  val carId = remember { repository.getSelectedCarId() }
  val carDef = remember { CarCatalog.getCar(carId) }
  val upgrades = remember { repository.getCarUpgrades(carId) }
  val bodyColor = remember { repository.getCarBodyColor(carId) }
  val neonColor = remember { repository.getCarNeonColor(carId) }

  // Game Engine instance
  var restartCounter by remember { mutableStateOf(0) }
  val engine = remember(restartCounter) {
    RacingGameEngine(
      mode = mode,
      theme = theme,
      carDef = carDef,
      upgrades = upgrades,
      customBodyColor = bodyColor,
      customNeonColor = neonColor,
      audioEngine = audioEngine,
      onHapticPulse = { strength ->
        if (hapticsEnabled && vibrator != null && vibrator.hasVibrator()) {
          try {
            val duration = when (strength) {
              1 -> 25L
              2 -> 70L
              else -> 180L
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              vibrator.vibrate(
                VibrationEffect.createOneShot(
                  duration,
                  if (strength == 3) VibrationEffect.DEFAULT_AMPLITUDE else 120
                )
              )
            } else {
              @Suppress("DEPRECATION")
              vibrator.vibrate(duration)
            }
          } catch (ignored: Exception) {}
        }
      }
    )
  }

  var isPaused by remember { mutableStateOf(false) }
  var hasSavedRunStats by remember { mutableStateOf(false) }
  var frameTicker by remember { mutableLongStateOf(0L) }

  // Intercept system Back button to pause
  BackHandler {
    if (!engine.isGameOver) {
      isPaused = !isPaused
    } else {
      onExitToMenu()
    }
  }

  // Engine audio loop lifecycle
  LaunchedEffect(restartCounter) {
    audioEngine.startEngine()
  }

  // 60 FPS Game Loop
  LaunchedEffect(isPaused, engine.isGameOver, restartCounter) {
    if (!isPaused && !engine.isGameOver) {
      var lastTime = 0L
      while (true) {
        withFrameNanos { nowNanos ->
          if (lastTime != 0L) {
            val dt = ((nowNanos - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            engine.update(dt)
            frameTicker = nowNanos
          }
          lastTime = nowNanos
        }
      }
    }
  }

  // When game finishes, commit stats and check achievements
  LaunchedEffect(engine.isGameOver) {
    if (engine.isGameOver && !hasSavedRunStats) {
      hasSavedRunStats = true
      audioEngine.stopEngine()

      // Add earned cash
      repository.addCash(engine.cashEarned)

      // High scores
      repository.updateHighScore(mode, engine.score)

      // Lifetime stats
      repository.addKmDriven(engine.playerY / 1000f)
      repository.recordMaxSpeed(engine.topSpeedAchieved)
      repository.addNearMisses(engine.nearMissCount)
      repository.addDriftScore(engine.driftScore)
      if (engine.isVictory && mode == GameMode.CAREER_GRAND_PRIX) {
        repository.incrementRacesWon()
      }

      // Check achievements
      repository.checkAndUnlockAchievement("ach_first_race")
      if (engine.topSpeedAchieved >= 200f) repository.checkAndUnlockAchievement("ach_speed_200")
      if (engine.topSpeedAchieved >= 280f) repository.checkAndUnlockAchievement("ach_speed_280")
      if (engine.nearMissCount >= 10) repository.checkAndUnlockAchievement("ach_near_miss_10")
      if (engine.driftScore >= 1500) repository.checkAndUnlockAchievement("ach_drift_king")
      if (engine.isVictory && mode == GameMode.CAREER_GRAND_PRIX) repository.checkAndUnlockAchievement("ach_podium_win")
      if (mode == GameMode.POLICE_PURSUIT && engine.playerY >= 2000f) repository.checkAndUnlockAchievement("ach_outrun_police")
    }
  }

  Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
    // Canvas
    GameCanvas(engine = engine)

    // HUD
    GameHud(
      engine = engine,
      isTouchDragMode = touchDragMode,
      onPauseClick = { isPaused = true }
    )

    // Countdown Overlay
    if (engine.isCountdown) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        val countText = when {
          engine.countdownTimer > 2.8f -> "3"
          engine.countdownTimer > 1.8f -> "2"
          engine.countdownTimer > 0.8f -> "1"
          else -> "GO!!"
        }
        val countColor = if (countText == "GO!!") NeonGreen else NeonOrange

        Text(
          text = countText,
          color = countColor,
          fontSize = 90.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          textAlign = TextAlign.Center
        )
      }
    }

    // Pause Dialog
    if (isPaused) {
      Dialog(onDismissRequest = { isPaused = false }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.padding(16.dp).border(1.dp, Color(0xFF2D3748), RoundedCornerShape(20.dp))
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "GAME PAUSED",
              color = Color.White,
              fontSize = 24.sp,
              fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = { isPaused = false },
              colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
              modifier = Modifier.fillMaxWidth().testTag("resume_button")
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground)
              Spacer(modifier = Modifier.width(8.dp))
              Text("RESUME", color = DarkBackground, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
              onClick = {
                isPaused = false
                hasSavedRunStats = false
                restartCounter++
              },
              modifier = Modifier.fillMaxWidth().testTag("restart_button")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonOrange)
              Spacer(modifier = Modifier.width(8.dp))
              Text("RESTART", color = NeonOrange, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
              onClick = {
                audioEngine.stopEngine()
                onExitToMenu()
              },
              modifier = Modifier.fillMaxWidth().testTag("quit_button")
            ) {
              Text("EXIT TO MENU", color = Color(0xFF8B949E), fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Game Over / Victory Modal
    AnimatedVisibility(
      visible = engine.isGameOver,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.Center)
    ) {
      Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth(0.9f)
          .padding(16.dp)
          .border(
            width = 2.dp,
            brush = Brush.verticalGradient(
              listOf(if (engine.isVictory) NeonGold else NeonPink, Color(0xFF2D3748))
            ),
            shape = RoundedCornerShape(24.dp)
          )
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = if (engine.isVictory) Icons.Default.EmojiEvents else Icons.Default.Speed,
            contentDescription = null,
            tint = if (engine.isVictory) NeonGold else NeonPink,
            modifier = Modifier.size(54.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = if (engine.isVictory) "VICTORY!" else "RACE FINISHED",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
          )

          Text(
            text = engine.gameOverReason,
            color = if (engine.isVictory) NeonGold else NeonPink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(18.dp))

          // Stats Breakdown Card
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xFF0D1117), RoundedCornerShape(14.dp))
              .padding(16.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              StatRow("Distance", "${engine.playerY.toInt()} m", NeonCyan)
              StatRow("Cash Earned", "+$ ${engine.cashEarned}", NeonGold)
              StatRow("Top Speed", "${engine.topSpeedAchieved.toInt()} km/h", Color.White)
              StatRow("Close Calls", "${engine.nearMissCount}", NeonOrange)
              if (engine.driftScore > 0) {
                StatRow("Drift Score", "${engine.driftScore} pts", Color(0xFFD500F9))
              }
              StatRow("Total Score", "${engine.score} PTS", NeonGreen)
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Play Again Button
          Button(
            onClick = {
              hasSavedRunStats = false
              restartCounter++
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.fillMaxWidth().testTag("play_again_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text("PLAY AGAIN", color = DarkBackground, fontWeight = FontWeight.Black)
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Garage Shortcut
            OutlinedButton(
              onClick = {
                audioEngine.stopEngine()
                onGoToGarage()
              },
              modifier = Modifier.weight(1f).testTag("result_garage_button")
            ) {
              Text("GARAGE", color = NeonGold, fontWeight = FontWeight.Bold)
            }

            // Menu Shortcut
            OutlinedButton(
              onClick = {
                audioEngine.stopEngine()
                onExitToMenu()
              },
              modifier = Modifier.weight(1f).testTag("result_menu_button")
            ) {
              Text("MENU", color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, color = Color(0xFF8B949E), fontSize = 13.sp)
    Text(
      text = value,
      color = valueColor,
      fontWeight = FontWeight.Bold,
      fontSize = 14.sp,
      fontFamily = FontFamily.Monospace
    )
  }
}
