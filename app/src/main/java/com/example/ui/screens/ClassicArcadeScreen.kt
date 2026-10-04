package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.data.RaceRepository
import com.example.model.GameMode
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class ClassicObstacle(
  val id: Long,
  val lane: Int, // 0, 1, 2
  var y: Float,  // 0 to canvas height
  val color: Color
)

@Composable
fun ClassicArcadeScreen(
  repository: RaceRepository,
  audioEngine: AudioEngine,
  onBackToMenu: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val vibrator = remember { context.getSystemService(Vibrator::class.java) }
  val hapticsEnabled = remember { repository.isHapticsEnabled() }
  val scope = rememberCoroutineScope()

  // Game state
  var score by remember { mutableFloatStateOf(0f) }
  var highScore by remember { mutableIntStateOf(repository.getHighScore(GameMode.ENDLESS_TRAFFIC)) }
  var gameRunning by remember { mutableStateOf(true) }
  var playerLane by remember { mutableIntStateOf(1) } // 0: Left, 1: Center, 2: Right
  val playerXAnim = remember { Animatable(1f) } // animated lane 0f, 1f, 2f

  // Nitro Boost feature state
  val maxNitro = 100f
  var nitroFuel by remember { mutableFloatStateOf(100f) }
  var isNitroActive by remember { mutableStateOf(false) }

  val obstacles = remember { mutableStateListOf<ClassicObstacle>() }
  var obstacleSpeed by remember { mutableFloatStateOf(4.5f) }
  var roadScroll by remember { mutableFloatStateOf(0f) }
  var spawnTimer by remember { mutableIntStateOf(0) }
  var nextObsId by remember { mutableLongStateOf(1L) }
  val focusRequester = remember { FocusRequester() }

  // Nitro animation transitions
  val infiniteTransition = rememberInfiniteTransition(label = "nitro_pulse")
  val nitroGlowPulse by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(350, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "nitro_glow"
  )

  fun triggerHaptic(strength: Int) {
    if (hapticsEnabled && vibrator != null && vibrator.hasVibrator()) {
      try {
        val duration = when (strength) {
          1 -> 25L
          2 -> 60L
          else -> 120L
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          vibrator.vibrate(duration)
        }
      } catch (ignored: Exception) {}
    }
  }

  fun moveLeft() {
    if (!gameRunning) return
    if (playerLane > 0) {
      playerLane--
      scope.launch {
        playerXAnim.animateTo(playerLane.toFloat(), tween(100))
      }
      triggerHaptic(1)
    }
  }

  fun moveRight() {
    if (!gameRunning) return
    if (playerLane < 2) {
      playerLane++
      scope.launch {
        playerXAnim.animateTo(playerLane.toFloat(), tween(100))
      }
      triggerHaptic(1)
    }
  }

  fun startNitro() {
    if (!gameRunning) return
    if (nitroFuel > 12f && !isNitroActive) {
      isNitroActive = true
      audioEngine.playNitro()
      triggerHaptic(2)
    }
  }

  fun stopNitro() {
    isNitroActive = false
  }

  fun restartGame() {
    obstacles.clear()
    score = 0f
    obstacleSpeed = 4.5f
    spawnTimer = 0
    playerLane = 1
    nitroFuel = 100f
    isNitroActive = false
    scope.launch {
      playerXAnim.snapTo(1f)
    }
    gameRunning = true
    audioEngine.startEngine()
  }

  fun endGame() {
    gameRunning = false
    isNitroActive = false
    audioEngine.playCrash()
    audioEngine.stopEngine()
    triggerHaptic(3)
    val finalScoreInt = score.toInt()
    if (finalScoreInt > highScore) {
      highScore = finalScoreInt
      repository.updateHighScore(GameMode.ENDLESS_TRAFFIC, finalScoreInt)
    }
    repository.addCash(finalScoreInt / 5)
  }

  BackHandler { onBackToMenu() }

  LaunchedEffect(Unit) {
    audioEngine.startEngine()
    focusRequester.requestFocus()
  }

  // 60fps Game Update Loop
  LaunchedEffect(gameRunning) {
    if (gameRunning) {
      val random = Random(System.currentTimeMillis())
      val colors = listOf(
        Color(0xFF00BFFF), // Cyan
        Color(0xFFFFA500), // Orange
        Color(0xFF8A2BE2), // BlueViolet
        Color(0xFFFFD700), // Gold
        Color(0xFFFF1E1E), // Red
        Color(0xFF00E676)  // Green
      )

      var lastTime = 0L
      while (gameRunning) {
        withFrameNanos { nowNanos ->
          if (lastTime != 0L) {
            val dt = ((nowNanos - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)

            // Nitro Fuel Depletion and Recharge
            if (isNitroActive) {
              nitroFuel = (nitroFuel - 32f * dt).coerceAtLeast(0f)
              if (nitroFuel <= 0f) {
                isNitroActive = false
              }
            } else {
              // Passive steady recharge over time
              nitroFuel = (nitroFuel + 14f * dt).coerceAtMost(maxNitro)
            }

            // Speed multiplier: 2.2x when Nitro boost is active!
            val speedMultiplier = if (isNitroActive) 2.2f else 1.0f
            val effectiveSpeed = obstacleSpeed * speedMultiplier

            // Spawn obstacles
            spawnTimer++
            roadScroll += effectiveSpeed * 60f * dt

            // Interval to spawn obstacle
            val spawnInterval = max(32, (65 - (score / 400)).toInt())
            if (spawnTimer >= spawnInterval) {
              val lane = random.nextInt(3)
              obstacles.add(
                ClassicObstacle(
                  id = nextObsId++,
                  lane = lane,
                  y = -100f,
                  color = colors[random.nextInt(colors.size)]
                )
              )
              spawnTimer = 0
            }

            // Gradually increase base speed as game progresses
            if (score.toInt() % 400 == 0 && score > 0) {
              obstacleSpeed = min(obstacleSpeed + 0.05f, 11f)
            }

            // Update obstacles
            val playerYPos = 570f // normalized relative to 700 canvas height

            val iter = obstacles.iterator()
            while (iter.hasNext()) {
              val obs = iter.next()
              obs.y += effectiveSpeed * 60f * dt

              // Approximate collision between animated player lane and obstacle lane
              val currentLaneX = playerXAnim.value
              val laneDiff = kotlin.math.abs(currentLaneX - obs.lane.toFloat())
              val isSameLane = laneDiff < 0.65f

              if (isSameLane) {
                val yDist = kotlin.math.abs(obs.y - playerYPos)
                if (yDist < 60f) {
                  endGame()
                  return@withFrameNanos
                }
              }

              // Remove passed obstacles
              if (obs.y > 800f) {
                iter.remove()
              }
            }

            // Increment score (faster while Nitro is active!)
            score += (40f * speedMultiplier) * dt
          }
          lastTime = nowNanos
        }
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF111111))
      .statusBarsPadding()
      .navigationBarsPadding()
      .onKeyEvent { keyEvent ->
        if (keyEvent.type == KeyEventType.KeyDown) {
          when (keyEvent.key) {
            Key.DirectionLeft -> {
              moveLeft()
              true
            }
            Key.DirectionRight -> {
              moveRight()
              true
            }
            Key.Spacebar, Key.DirectionUp, Key.N -> {
              startNitro()
              true
            }
            Key.R -> {
              restartGame()
              true
            }
            else -> false
          }
        } else if (keyEvent.type == KeyEventType.KeyUp) {
          if (keyEvent.key == Key.Spacebar || keyEvent.key == Key.DirectionUp || keyEvent.key == Key.N) {
            stopNitro()
            true
          } else {
            false
          }
        } else {
          false
        }
      }
      .focusRequester(focusRequester)
      .focusable(),
    contentAlignment = Alignment.Center
  ) {
    // Top Navigation & Return button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = {
          audioEngine.stopEngine()
          onBackToMenu()
        },
        modifier = Modifier
          .size(42.dp)
          .background(Color(0xFF222222), CircleShape)
          .border(1.dp, Color(0xFF444444), CircleShape)
          .testTag("classic_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = Color.White
        )
      }

      Text(
        text = "CLASSIC ARCADE",
        color = Color.White,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        letterSpacing = 1.sp
      )

      Box(
        modifier = Modifier
          .background(Color(0xFF222222), RoundedCornerShape(12.dp))
          .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = "BEST: $highScore",
          color = Color(0xFFFFD700),
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    // Centered Game Box Container (Styled matching .game-wrapper)
    Box(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .aspectRatio(400f / 700f)
        .shadow(24.dp, RoundedCornerShape(14.dp))
        .clip(RoundedCornerShape(14.dp))
        .border(4.dp, Color.White, RoundedCornerShape(14.dp))
        .background(Color(0xFF222222))
    ) {
      // -------------------------------------------------------------
      // CANVAS DRAWING (Road, Grass, Markings, Obstacles, Player, Nitro Flames)
      // -------------------------------------------------------------
      Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height

        val scaleX = canvasW / 400f
        val scaleY = canvasH / 700f

        val roadX = 60f * scaleX
        val roadW = 280f * scaleX
        val laneW = roadW / 3f

        // Screen shake when Nitro is active
        val shakeX = if (isNitroActive) (sin(roadScroll * 0.9f) * 2.5f * scaleX) else 0f
        val shakeY = if (isNitroActive) (sin(roadScroll * 1.3f) * 2.5f * scaleY) else 0f

        // 1. Grass Verges (#1f7a1f)
        drawRect(color = Color(0xFF1F7A1F), topLeft = Offset.Zero, size = Size(canvasW, canvasH))

        // 2. Asphalt Road (#444)
        drawRect(color = Color(0xFF444444), topLeft = Offset(roadX + shakeX, 0f), size = Size(roadW, canvasH))

        // 3. White Road Edges (5px)
        val edgeW = 5f * scaleX
        drawRect(color = Color.White, topLeft = Offset(roadX + shakeX, 0f), size = Size(edgeW, canvasH))
        drawRect(color = Color.White, topLeft = Offset(roadX + roadW - edgeW + shakeX, 0f), size = Size(edgeW, canvasH))

        // 4. Dashed Lane Lines (#ddd)
        val dashH = 30f * scaleY
        val gapH = 25f * scaleY
        val period = dashH + gapH
        val offset = ((roadScroll + shakeY) * scaleY) % period

        for (laneIdx in 1..2) {
          val lineX = roadX + laneW * laneIdx + shakeX
          var curY = -offset
          while (curY < canvasH) {
            drawLine(
              color = Color(0xFFDDDDDD),
              start = Offset(lineX, curY),
              end = Offset(lineX, curY + dashH),
              strokeWidth = 4f * scaleX
            )
            curY += period
          }
        }

        // 5. Draw Obstacle Cars
        val carW = 40f * scaleX
        val carH = 70f * scaleY

        for (obs in obstacles) {
          val obsLane = obs.lane
          val obsX = roadX + obsLane * laneW + (laneW / 2f) - (carW / 2f) + shakeX
          val obsY = obs.y * scaleY + shakeY

          if (obsY >= -carH && obsY <= canvasH + carH) {
            // Wheels
            drawRect(Color.Black, Offset(obsX - 6f * scaleX, obsY + 10f * scaleY), Size(6f * scaleX, 18f * scaleY))
            drawRect(Color.Black, Offset(obsX - 6f * scaleX, obsY + 42f * scaleY), Size(6f * scaleX, 18f * scaleY))
            drawRect(Color.Black, Offset(obsX + carW, obsY + 10f * scaleY), Size(6f * scaleX, 18f * scaleY))
            drawRect(Color.Black, Offset(obsX + carW, obsY + 42f * scaleY), Size(6f * scaleX, 18f * scaleY))

            // Body
            drawRect(obs.color, Offset(obsX, obsY), Size(carW, carH))

            // Windshield
            drawRect(Color.White, Offset(obsX + 8f * scaleX, obsY + 10f * scaleY), Size(carW - 16f * scaleX, 12f * scaleY))
          }
        }

        // 6. Draw Player Car
        val currentAnimLane = playerXAnim.value
        val playerX = roadX + currentAnimLane * laneW + (laneW / 2f) - (carW / 2f) + shakeX
        val playerY = 570f * scaleY + shakeY

        // Dual Nitro Flames behind player when Nitro is active!
        if (isNitroActive) {
          val flameLength = (22f + sin(roadScroll * 0.7f) * 8f) * scaleY
          val flameW = 8f * scaleX

          // Left flame
          drawOval(
            brush = Brush.verticalGradient(
              listOf(Color(0xFF00E5FF), Color(0xFFFF5722), Color.Transparent),
              startY = playerY + carH,
              endY = playerY + carH + flameLength
            ),
            topLeft = Offset(playerX + 5f * scaleX, playerY + carH),
            size = Size(flameW, flameLength)
          )

          // Right flame
          drawOval(
            brush = Brush.verticalGradient(
              listOf(Color(0xFF00E5FF), Color(0xFFFF5722), Color.Transparent),
              startY = playerY + carH,
              endY = playerY + carH + flameLength
            ),
            topLeft = Offset(playerX + carW - 13f * scaleX, playerY + carH),
            size = Size(flameW, flameLength)
          )

          // Cyan speed aura around player car
          drawRoundRect(
            color = Color(0x5500E5FF),
            topLeft = Offset(playerX - 4f * scaleX, playerY - 4f * scaleY),
            size = Size(carW + 8f * scaleX, carH + 8f * scaleY),
            cornerRadius = CornerRadius(8f * scaleX, 8f * scaleY)
          )
        }

        // Player Wheels
        drawRect(Color.Black, Offset(playerX - 6f * scaleX, playerY + 10f * scaleY), Size(6f * scaleX, 18f * scaleY))
        drawRect(Color.Black, Offset(playerX - 6f * scaleX, playerY + 42f * scaleY), Size(6f * scaleX, 18f * scaleY))
        drawRect(Color.Black, Offset(playerX + carW, playerY + 10f * scaleY), Size(6f * scaleX, 18f * scaleY))
        drawRect(Color.Black, Offset(playerX + carW, playerY + 42f * scaleY), Size(6f * scaleX, 18f * scaleY))

        // Player Body (#ff1e1e)
        drawRect(Color(0xFFFF1E1E), Offset(playerX, playerY), Size(carW, carH))

        // Player Windshield (#99d9ff)
        drawRect(Color(0xFF99D9FF), Offset(playerX + 8f * scaleX, playerY + 10f * scaleY), Size(carW - 16f * scaleX, 18f * scaleY))

        // Speed Lines across screen when Nitro is engaged
        if (isNitroActive) {
          for (i in 0..10) {
            val sx = roadX + (i * 28f * scaleX)
            val sy = ((roadScroll * 2f + i * 85f) * scaleY) % canvasH
            drawLine(
              color = Color(0x6600E5FF),
              start = Offset(sx, sy),
              end = Offset(sx, sy + 45f * scaleY),
              strokeWidth = 2.5f * scaleX
            )
          }
        }
      }

      // -------------------------------------------------------------
      // TOP HUD: SCORE & NITRO METER
      // -------------------------------------------------------------
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Score HUD
        Text(
          text = "Score: ${score.toInt()}",
          color = Color.White,
          fontSize = 19.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif
        )

        // NITRO METER UI (Depletes when used, recharges over time)
        val nitroRatio = (nitroFuel / maxNitro).coerceIn(0f, 1f)
        val isReady = nitroFuel > 12f

        Column(
          horizontalAlignment = Alignment.End,
          modifier = Modifier.testTag("nitro_meter_container")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.ElectricBolt,
              contentDescription = "Nitro",
              tint = if (isNitroActive) Color(0xFFFF5722) else if (isReady) Color(0xFF00E5FF) else Color(0xFF888888),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = if (isNitroActive) "NITRO BOOST!" else "NITRO",
              color = if (isNitroActive) Color(0xFFFF5722) else if (isReady) Color(0xFF00E5FF) else Color(0xFF888888),
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${(nitroRatio * 100).toInt()}%",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.height(3.dp))

          // Meter Bar
          Box(
            modifier = Modifier
              .width(95.dp)
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFF333333))
              .border(
                1.dp,
                if (isNitroActive) Color(0xFFFF5722) else Color(0xFF555555),
                RoundedCornerShape(4.dp)
              )
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(fraction = nitroRatio)
                .fillMaxSize()
                .background(
                  brush = Brush.horizontalGradient(
                    colors = if (isNitroActive) {
                      listOf(Color(0xFF00E5FF), Color(0xFFFF5722))
                    } else {
                      listOf(Color(0xFF0088FF), Color(0xFF00E5FF))
                    }
                  )
                )
            )
          }
        }
      }

      // -------------------------------------------------------------
      // GAME OVER MESSAGE OVERLAY (Matching .message #gameOverMsg)
      // -------------------------------------------------------------
      AnimatedVisibility(
        visible = !gameRunning,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.Center)
      ) {
        Column(
          modifier = Modifier
            .background(Color(0xEE111111), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFFF4D4D), RoundedCornerShape(16.dp))
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Game Over",
            color = Color(0xFFFF4D4D),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Your score: ${score.toInt()}",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(18.dp))

          Button(
            onClick = { restartGame() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("restart_game_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF111111))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Restart",
              color = Color(0xFF111111),
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // -------------------------------------------------------------
      // CONTROLS OVERLAY: [ ◀ ] [ ⚡ NITRO ] [ ▶ ]
      // -------------------------------------------------------------
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left Button
        Box(
          modifier = Modifier
            .size(width = 68.dp, height = 52.dp)
            .shadow(6.dp, RoundedCornerShape(10.dp))
            .background(Color.White, RoundedCornerShape(10.dp))
            .testTag("control_left_button")
            .pointerInput(Unit) {
              detectTapGestures(onTap = { moveLeft() })
            },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "◀",
            color = Color(0xFF111111),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Nitro Boost Button
        val canUseNitro = nitroFuel > 12f
        Box(
          modifier = Modifier
            .size(width = 82.dp, height = 52.dp)
            .shadow(6.dp, RoundedCornerShape(10.dp))
            .background(
              color = if (isNitroActive) Color(0xFFFF5722) else if (canUseNitro) Color(0xFF00E5FF) else Color(0xFF555555),
              shape = RoundedCornerShape(10.dp)
            )
            .border(
              width = 2.dp,
              color = if (isNitroActive) Color.White else if (canUseNitro) Color(0xFF80D8FF) else Color(0xFF444444),
              shape = RoundedCornerShape(10.dp)
            )
            .testTag("control_nitro_button")
            .pointerInput(canUseNitro) {
              detectTapGestures(
                onPress = {
                  if (canUseNitro) {
                    startNitro()
                    tryAwaitRelease()
                    stopNitro()
                  }
                }
              )
            },
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.ElectricBolt,
              contentDescription = "Nitro",
              tint = if (canUseNitro || isNitroActive) Color.White else Color(0xFF888888),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "NITRO",
              color = if (canUseNitro || isNitroActive) Color.White else Color(0xFF888888),
              fontSize = 13.sp,
              fontWeight = FontWeight.Black
            )
          }
        }

        // Right Button
        Box(
          modifier = Modifier
            .size(width = 68.dp, height = 52.dp)
            .shadow(6.dp, RoundedCornerShape(10.dp))
            .background(Color.White, RoundedCornerShape(10.dp))
            .testTag("control_right_button")
            .pointerInput(Unit) {
              detectTapGestures(onTap = { moveRight() })
            },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "▶",
            color = Color(0xFF111111),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
