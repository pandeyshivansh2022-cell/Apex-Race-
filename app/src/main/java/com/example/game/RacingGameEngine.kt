package com.example.game

import com.example.audio.AudioEngine
import com.example.model.CarCatalog
import com.example.model.CarDefinition
import com.example.model.CarUpgrades
import com.example.model.Collectible
import com.example.model.CollectibleType
import com.example.model.FloatingMessage
import com.example.model.GameMode
import com.example.model.Particle
import com.example.model.ParticleType
import com.example.model.TrackTheme
import com.example.model.TrafficVehicle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class RacingGameEngine(
  val mode: GameMode,
  val theme: TrackTheme,
  val carDef: CarDefinition,
  val upgrades: CarUpgrades,
  val customBodyColor: Long,
  val customNeonColor: Long,
  val audioEngine: AudioEngine,
  val onHapticPulse: (strength: Int) -> Unit = {}
) {

  // Road geometry
  val roadHalfWidth = 0.85f // player bounds -roadHalfWidth to +roadHalfWidth
  val numLanes = 4
  val lanePositions = floatArrayOf(-0.55f, -0.18f, 0.18f, 0.55f)

  // Player state
  var playerX = 0f                // -1f to 1f normalized
  var playerY = 0f                // continuous world distance in meters
  var playerSpeed = 0f            // km/h
  var targetSpeed = 0f
  var steerInput = 0f             // -1f (left) to +1f (right)
  var throttleInput = 0f          // 0f to 1f
  var brakeInput = 0f             // 0f to 1f
  var isDrifting = false
  var driftAngle = 0f             // degrees
  var driftScore = 0
  var driftTimer = 0f

  // Health and Nitro
  val maxHealth = 100f + (upgrades.armorLevel - 1) * 20f
  var health = maxHealth
  val maxNitro = 100f
  var nitroFuel = 80f
  var isNitroActive = false

  // Transmission & RPM
  var currentGear = 1
  var currentRpm = 0.2f // 0f to 1f

  // Air Time
  var airTimeHeight = 0f // > 0 when launched over speed ramp
  var airTimeVelocity = 0f

  // Powerup timers
  var shieldTimer = 0f
  var magnetTimer = 0f
  var doubleCashTimer = 0f

  // Stats for this run
  var score = 0
  var cashEarned = 0
  var nearMissCount = 0
  var comboMultiplier = 1
  var comboResetTimer = 0f
  var topSpeedAchieved = 0f

  // Grand Prix specific
  val totalLaps = 3
  val lapLengthMeters = 2500f
  var currentLap = 1
  var playerRaceRank = 8 // starts at 8th place
  var totalRaceDistance = totalLaps * lapLengthMeters

  // Police pursuit specific
  var heatLevel = 1f
  var policeAlertTimer = 0f

  // Game status
  var isCountdown = true
  var countdownTimer = 3.8f // 3..2..1..GO!
  var isGameOver = false
  var isVictory = false
  var gameOverReason = ""

  // Camera and Screen shake
  var cameraShake = 0f
  var roadCurvature = 0f

  // Entities
  val trafficList = mutableListOf<TrafficVehicle>()
  val collectibles = mutableListOf<Collectible>()
  val particles = mutableListOf<Particle>()
  val floatingMessages = mutableListOf<FloatingMessage>()

  private var nextEntityId = 1L
  private var spawnDistanceCounter = 0f
  private var nextTrafficSpawnDistance = 60f
  private var nextCollectibleSpawnDistance = 40f
  private val random = Random(System.currentTimeMillis())

  init {
    if (mode == GameMode.CAREER_GRAND_PRIX) {
      initGrandPrixRivals()
    }
  }

  private fun initGrandPrixRivals() {
    val rivalColors = listOf(0xFF2979FF, 0xFFFFAB00, 0xFF00E676, 0xFFFF1744, 0xFFE040FB, 0xFF00B0FF, 0xFFFF6D00)
    for (i in 0 until 7) {
      val rank = 7 - i // rank 1 to 7 ahead of player
      val startDist = 80f + i * 90f
      val laneIdx = i % numLanes
      val rival = TrafficVehicle(
        id = nextEntityId++,
        x = lanePositions[laneIdx],
        y = startDist,
        speed = 130f + i * 8f,
        targetX = lanePositions[laneIdx],
        carType = 1,
        colorHex = rivalColors[i % rivalColors.size],
        isRivalRacer = true,
        rivalRank = rank
      )
      trafficList.add(rival)
    }
  }

  fun update(dt: Float) {
    if (isGameOver) {
      updateParticlesOnly(dt)
      return
    }

    // Handle Countdown
    if (isCountdown) {
      val prevInt = countdownTimer.toInt()
      countdownTimer -= dt
      val currInt = countdownTimer.toInt()
      if (prevInt != currInt && currInt in 1..3) {
        audioEngine.playCountdownLow()
        onHapticPulse(1)
      } else if (countdownTimer <= 0f) {
        isCountdown = false
        audioEngine.playCountdownHigh()
        onHapticPulse(3)
        addFloatingMessage("GO!!", 0f, 0.4f, 0xFF00E676)
      }
      updateParticlesOnly(dt)
      return
    }

    // Screen shake decay
    if (cameraShake > 0f) {
      cameraShake = (cameraShake - dt * 25f).coerceAtLeast(0f)
    }

    // Calculate Road Curvature based on player world distance
    val curveCycle = playerY * 0.0018f
    roadCurvature = sin(curveCycle) * 0.42f + cos(curveCycle * 2.3f) * 0.18f

    // Process Power-up Timers
    if (shieldTimer > 0f) shieldTimer = (shieldTimer - dt).coerceAtLeast(0f)
    if (magnetTimer > 0f) magnetTimer = (magnetTimer - dt).coerceAtLeast(0f)
    if (doubleCashTimer > 0f) doubleCashTimer = (doubleCashTimer - dt).coerceAtLeast(0f)

    // Combo Timer Decay
    if (comboResetTimer > 0f) {
      comboResetTimer -= dt
      if (comboResetTimer <= 0f) {
        comboMultiplier = 1
      }
    }

    // Vehicle Physics: Speed & Acceleration
    val maxBaseSpeed = 150f + carDef.baseSpeed * 0.7f + upgrades.speedLevel * 14f
    val nitroSpeedBoost = if (isNitroActive) 45f + upgrades.nitroLevel * 8f else 0f
    val topSpeed = maxBaseSpeed + nitroSpeedBoost

    val accelRate = (40f + carDef.baseAccel * 0.5f + upgrades.accelLevel * 10f) * (if (isNitroActive) 2.2f else 1.0f)
    val brakeRate = 120f

    if (isNitroActive) {
      nitroFuel = (nitroFuel - dt * 25f).coerceAtLeast(0f)
      if (nitroFuel <= 0f) {
        isNitroActive = false
      }
      cameraShake = 4f
      emitNitroParticles()
    } else {
      // Slow passive nitro recharge
      nitroFuel = (nitroFuel + dt * 3.5f).coerceAtMost(maxNitro)
    }

    // Speed acceleration / deceleration
    if (throttleInput > 0f) {
      playerSpeed = (playerSpeed + accelRate * throttleInput * dt).coerceAtMost(topSpeed)
    } else if (brakeInput > 0f) {
      playerSpeed = (playerSpeed - brakeRate * brakeInput * dt).coerceAtLeast(0f)
    } else {
      // Natural rolling resistance
      playerSpeed = (playerSpeed - 18f * dt).coerceAtLeast(0f)
    }

    if (playerSpeed > topSpeedAchieved) {
      topSpeedAchieved = playerSpeed
    }

    // Transmission, Gears, and RPM calculation
    val speedRatio = (playerSpeed / topSpeed).coerceIn(0f, 1f)
    currentGear = when {
      playerSpeed < 45f -> 1
      playerSpeed < 90f -> 2
      playerSpeed < 140f -> 3
      playerSpeed < 195f -> 4
      playerSpeed < 255f -> 5
      else -> 6
    }
    // Gear RPM saw curve
    val gearSpeedRange = 50f
    val inGearSpeed = (playerSpeed % gearSpeedRange) / gearSpeedRange
    currentRpm = (0.25f + inGearSpeed * 0.75f).coerceIn(0.2f, 1.0f)
    audioEngine.updateEngineRPM(currentRpm, throttleInput > 0f || isNitroActive)

    // Steering & Handling
    val handling = (carDef.baseHandling * 0.01f + upgrades.handlingLevel * 0.12f).coerceIn(0.6f, 1.5f)
    val steerSpeed = 2.4f * handling
    // Drifting logic
    if (isDrifting && playerSpeed > 60f) {
      driftAngle = (steerInput * 32f).coerceIn(-35f, 35f)
      driftTimer += dt
      val pointsGained = (playerSpeed * 0.5f * dt * comboMultiplier).toInt()
      driftScore += pointsGained
      score += pointsGained
      emitDriftParticles()
    } else {
      driftAngle = (driftAngle * (1f - dt * 6f)).coerceIn(-35f, 35f)
      driftTimer = 0f
    }

    // Lateral player movement with centrifugal curve drift
    val centrifugal = -roadCurvature * (playerSpeed / 180f) * 0.45f
    val lateralDelta = (steerInput * steerSpeed + centrifugal) * dt
    playerX = (playerX + lateralDelta).coerceIn(-1.05f, 1.05f)

    // Air time logic
    if (airTimeHeight > 0f || airTimeVelocity > 0f) {
      airTimeVelocity -= 22f * dt
      airTimeHeight = (airTimeHeight + airTimeVelocity * dt).coerceAtLeast(0f)
      if (airTimeHeight == 0f) {
        airTimeVelocity = 0f
        cameraShake = 5f
        onHapticPulse(2)
        emitSmokeBurst(playerX, 0f, 10)
      }
    }

    // Check Guardrail Scrape
    if (abs(playerX) >= roadHalfWidth && airTimeHeight <= 0.05f) {
      // Scrape friction & spark particles
      playerSpeed = (playerSpeed - 45f * dt).coerceAtLeast(30f)
      takeDamage(8f * dt)
      cameraShake = 2.5f
      onHapticPulse(1)
      val side = if (playerX > 0) roadHalfWidth else -roadHalfWidth
      emitSparks(side, 0f, 4)
    }

    // Update distance
    val distanceStepMeters = (playerSpeed / 3.6f) * dt
    playerY += distanceStepMeters
    score += (distanceStepMeters * 0.8f * comboMultiplier).toInt()

    // Mode-specific distance checks (Grand Prix Laps & Victory)
    if (mode == GameMode.CAREER_GRAND_PRIX) {
      val lapProgress = playerY % lapLengthMeters
      val completedLaps = (playerY / lapLengthMeters).toInt() + 1
      if (completedLaps > currentLap && completedLaps <= totalLaps) {
        currentLap = completedLaps
        addFloatingMessage("LAP $currentLap / $totalLaps!", 0f, 0.4f, 0xFFFFD600)
        audioEngine.playPowerup()
        onHapticPulse(2)
      } else if (playerY >= totalRaceDistance) {
        isVictory = true
        isGameOver = true
        gameOverReason = if (playerRaceRank == 1) "1st PLACE CHAMPION!" else "${playerRaceRank}th Place Finish"
        audioEngine.playPowerup()
        return
      }
    }

    // Spawning Traffic & Collectibles
    spawnDistanceCounter += distanceStepMeters
    if (spawnDistanceCounter >= nextTrafficSpawnDistance) {
      spawnDistanceCounter = 0f
      nextTrafficSpawnDistance = random.nextFloat() * 45f + 35f
      spawnTraffic()
      if (random.nextFloat() < 0.65f) {
        spawnCollectible()
      }
    }

    // Update Traffic Vehicles
    val iterator = trafficList.iterator()
    while (iterator.hasNext()) {
      val traffic = iterator.next()
      // Traffic moves along road at its own speed
      val trafficDistStep = (traffic.speed / 3.6f) * dt
      traffic.y += trafficDistStep

      // Lane shifting behavior for sports / dynamic cars
      if (traffic.carType == 1 && random.nextFloat() < 0.015f) {
        val randomLane = lanePositions[random.nextInt(lanePositions.size)]
        traffic.targetX = randomLane
      }
      traffic.x += (traffic.targetX - traffic.x) * 1.5f * dt

      // Police chase AI logic
      if (traffic.isPolice) {
        traffic.sirenPhase += dt * 8f
        // Police actively tries to steer towards player
        traffic.targetX = playerX.coerceIn(-0.65f, 0.65f)
        traffic.speed = (traffic.speed + 15f * dt).coerceAtMost(225f)
      }

      // Check Near-Miss (reward player for closely overtaking at speed)
      val relY = traffic.y - playerY
      if (!traffic.passedPlayer && relY < 0f && relY > -30f) {
        traffic.passedPlayer = true
        val latDist = abs(traffic.x - playerX)
        if (latDist < 0.32f && playerSpeed > 100f && airTimeHeight <= 0.1f) {
          // Near miss triggered!
          nearMissCount++
          comboMultiplier = (comboMultiplier + 1).coerceAtMost(5)
          comboResetTimer = 4.5f
          val bonusCash = 25 * comboMultiplier * (if (doubleCashTimer > 0f) 2 else 1)
          cashEarned += bonusCash
          score += 150 * comboMultiplier
          audioEngine.playNearMiss()
          onHapticPulse(1)
          addFloatingMessage("CLOSE CALL! +$bonusCash ($comboMultiplier x)", playerX, 0.35f, 0xFF00E5FF)
        }
      }

      // Collision Detection with Player
      if (airTimeHeight <= 0.15f) {
        val relDistY = abs(relY)
        val relDistX = abs(traffic.x - playerX)
        if (relDistY < 18f && relDistX < 0.26f) {
          // Collision!
          handleCollisionWithTraffic(traffic)
        }
      }

      // Cleanup traffic far behind
      if (traffic.y < playerY - 70f) {
        iterator.remove()
      }
    }

    // Update Rank in Grand Prix
    if (mode == GameMode.CAREER_GRAND_PRIX) {
      val rivalsAhead = trafficList.count { it.isRivalRacer && it.y > playerY }
      playerRaceRank = rivalsAhead + 1
    }

    // Update Collectibles
    val colIter = collectibles.iterator()
    while (colIter.hasNext()) {
      val col = colIter.next()
      val relY = col.y - playerY

      // Magnet power-up effect (pull towards player)
      if (magnetTimer > 0f && col.type == CollectibleType.COIN && relY > 0f && relY < 80f) {
        val dx = playerX - col.x
        col.x += dx * 4.5f * dt
      }

      // Check pickup
      val distX = abs(col.x - playerX)
      val distY = abs(relY)
      if (distY < 12f && distX < 0.28f && !col.isCollected) {
        col.isCollected = true
        handleCollectItem(col)
        colIter.remove()
        continue
      }

      if (col.y < playerY - 40f) {
        colIter.remove()
      }
    }

    // Update Floating Messages
    val msgIter = floatingMessages.iterator()
    while (msgIter.hasNext()) {
      val msg = msgIter.next()
      msg.life -= dt
      msg.y -= 0.12f * dt // drift upwards on screen
      if (msg.life <= 0f) {
        msgIter.remove()
      }
    }

    // Update Weather & Particles
    updateParticles(dt)
  }

  private fun handleCollisionWithTraffic(traffic: TrafficVehicle) {
    if (shieldTimer > 0f) {
      // Shield absorbs collision
      shieldTimer = 0f
      cameraShake = 7f
      audioEngine.playCrash()
      onHapticPulse(3)
      addFloatingMessage("SHIELD ABSORBED!", playerX, 0.4f, 0xFF00E5FF)
      traffic.y += 40f // bump away
      emitSparks(playerX, 0f, 15)
      return
    }

    // Calculate impact severity
    val speedDelta = (playerSpeed - traffic.speed).coerceAtLeast(30f)
    val damage = (speedDelta * 0.35f / (1f + (upgrades.armorLevel - 1) * 0.2f)).coerceIn(15f, 65f)
    takeDamage(damage)

    // Slow player down
    playerSpeed = (playerSpeed * 0.4f).coerceAtLeast(25f)
    cameraShake = 9f
    onHapticPulse(3)
    audioEngine.playCrash()
    comboMultiplier = 1

    emitSparks(playerX, 0f, 20)
    emitSmokeBurst(playerX, 0f, 12)

    // Push vehicle aside
    if (playerX < traffic.x) {
      traffic.x += 0.25f
      playerX -= 0.15f
    } else {
      traffic.x -= 0.25f
      playerX += 0.15f
    }
  }

  private fun takeDamage(amount: Float) {
    health = (health - amount).coerceAtLeast(0f)
    if (health <= 0f && !isGameOver) {
      isGameOver = true
      isVictory = false
      gameOverReason = "VEHICLE TOTALED!"
      audioEngine.playCrash()
      onHapticPulse(3)
      emitExplosion(playerX, 0f)
    }
  }

  private fun handleCollectItem(col: Collectible) {
    when (col.type) {
      CollectibleType.COIN -> {
        val mult = if (doubleCashTimer > 0f) 2 else 1
        val cash = col.value * mult
        cashEarned += cash
        score += 50 * mult
        audioEngine.playCoin()
        onHapticPulse(1)
        addFloatingMessage("+$cash$", col.x, 0.35f, 0xFFFFD600)
      }
      CollectibleType.NITRO_CANISTER -> {
        nitroFuel = (nitroFuel + 45f).coerceAtMost(maxNitro)
        audioEngine.playPowerup()
        onHapticPulse(1)
        addFloatingMessage("NITRO +45%!", col.x, 0.35f, 0xFF00E5FF)
      }
      CollectibleType.SHIELD -> {
        shieldTimer = 10f
        audioEngine.playPowerup()
        onHapticPulse(2)
        addFloatingMessage("SHIELD ACTIVE!", col.x, 0.35f, 0xFF00E5FF)
      }
      CollectibleType.REPAIR_WRENCH -> {
        health = (health + 40f).coerceAtMost(maxHealth)
        audioEngine.playPowerup()
        onHapticPulse(1)
        addFloatingMessage("REPAIR +40 HP", col.x, 0.35f, 0xFF00E676)
      }
      CollectibleType.DOUBLE_CASH -> {
        doubleCashTimer = 12f
        audioEngine.playPowerup()
        onHapticPulse(2)
        addFloatingMessage("2X CASH BOOST!", col.x, 0.35f, 0xFFFFD600)
      }
      CollectibleType.MAGNET -> {
        magnetTimer = 12f
        audioEngine.playPowerup()
        onHapticPulse(2)
        addFloatingMessage("COIN MAGNET!", col.x, 0.35f, 0xFFE040FB)
      }
      CollectibleType.SPEED_RAMP -> {
        airTimeVelocity = 14f
        playerSpeed = (playerSpeed * 1.2f).coerceAtMost(320f)
        score += 200
        cashEarned += 50
        cameraShake = 6f
        audioEngine.playNitro()
        onHapticPulse(3)
        addFloatingMessage("RAMP JUMP! +50$", col.x, 0.35f, 0xFFFF6D00)
      }
    }
  }

  fun triggerNitro() {
    if (nitroFuel > 15f && !isNitroActive && !isCountdown && !isGameOver) {
      isNitroActive = true
      audioEngine.playNitro()
      onHapticPulse(2)
      addFloatingMessage("NITRO ENGAGED!", playerX, 0.35f, 0xFF00E5FF)
    }
  }

  private fun spawnTraffic() {
    val lane = lanePositions[random.nextInt(lanePositions.size)]
    val carType = random.nextInt(4) // 0: sedan, 1: sport, 2: truck, 3: suv
    val speed = when (carType) {
      2 -> random.nextFloat() * 25f + 70f // truck: 70-95 km/h
      1 -> random.nextFloat() * 40f + 140f // sports: 140-180 km/h
      else -> random.nextFloat() * 30f + 105f // sedan/suv: 105-135 km/h
    }
    val colors = listOf(0xFFB0BEC5, 0xFFE53935, 0xFF1E88E5, 0xFFFDD835, 0xFF43A047, 0xFFFB8C00, 0xFF8E24AA)

    val isPolice = mode == GameMode.POLICE_PURSUIT && random.nextFloat() < 0.35f

    val traffic = TrafficVehicle(
      id = nextEntityId++,
      x = lane,
      y = playerY + 130f + random.nextFloat() * 40f,
      speed = if (isPolice) 185f else speed,
      targetX = lane,
      carType = carType,
      colorHex = if (isPolice) 0xFF0D47A1 else colors[random.nextInt(colors.size)],
      isPolice = isPolice
    )
    trafficList.add(traffic)
  }

  private fun spawnCollectible() {
    val lane = lanePositions[random.nextInt(lanePositions.size)]
    val roll = random.nextFloat()
    val type = when {
      roll < 0.45f -> CollectibleType.COIN
      roll < 0.65f -> CollectibleType.NITRO_CANISTER
      roll < 0.78f -> CollectibleType.SPEED_RAMP
      roll < 0.86f -> CollectibleType.REPAIR_WRENCH
      roll < 0.92f -> CollectibleType.SHIELD
      roll < 0.96f -> CollectibleType.MAGNET
      else -> CollectibleType.DOUBLE_CASH
    }

    val item = Collectible(
      id = nextEntityId++,
      type = type,
      x = lane + (random.nextFloat() - 0.5f) * 0.15f,
      y = playerY + 110f + random.nextFloat() * 30f,
      value = if (type == CollectibleType.COIN) 15 else 0
    )
    collectibles.add(item)
  }

  private fun addFloatingMessage(text: String, x: Float, y: Float, color: Long) {
    floatingMessages.add(FloatingMessage(nextEntityId++, text, x, y, color, 1.2f, 1.2f))
  }

  private fun emitNitroParticles() {
    val spread = (random.nextFloat() - 0.5f) * 0.08f
    particles.add(
      Particle(
        x = playerX - 0.06f + spread,
        y = 0f,
        vx = (random.nextFloat() - 0.5f) * 0.2f,
        vy = random.nextFloat() * 1.5f + 1.2f,
        color = if (random.nextBoolean()) 0xFF00E5FF else 0xFFFF6D00,
        radius = random.nextFloat() * 12f + 8f,
        life = 0.35f,
        maxLife = 0.35f,
        type = ParticleType.NITRO_FLAME
      )
    )
    particles.add(
      Particle(
        x = playerX + 0.06f + spread,
        y = 0f,
        vx = (random.nextFloat() - 0.5f) * 0.2f,
        vy = random.nextFloat() * 1.5f + 1.2f,
        color = if (random.nextBoolean()) 0xFF00E5FF else 0xFFFF6D00,
        radius = random.nextFloat() * 12f + 8f,
        life = 0.35f,
        maxLife = 0.35f,
        type = ParticleType.NITRO_FLAME
      )
    )
  }

  private fun emitDriftParticles() {
    val wheelOffset = if (driftAngle > 0) -0.1f else 0.1f
    particles.add(
      Particle(
        x = playerX + wheelOffset,
        y = 0f,
        vx = (random.nextFloat() - 0.5f) * 0.4f,
        vy = random.nextFloat() * 0.8f + 0.4f,
        color = 0xAAFFFFFF,
        radius = random.nextFloat() * 10f + 6f,
        life = 0.45f,
        maxLife = 0.45f,
        type = ParticleType.SMOKE
      )
    )
  }

  private fun emitSparks(x: Float, y: Float, count: Int) {
    for (i in 0 until count) {
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = (random.nextFloat() - 0.5f) * 2.2f,
          vy = random.nextFloat() * 2f - 0.5f,
          color = if (random.nextBoolean()) 0xFFFFD600 else 0xFFFF6D00,
          radius = random.nextFloat() * 5f + 2f,
          life = 0.3f,
          maxLife = 0.3f,
          type = ParticleType.SPARK
        )
      )
    }
  }

  private fun emitSmokeBurst(x: Float, y: Float, count: Int) {
    for (i in 0 until count) {
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = (random.nextFloat() - 0.5f) * 1.2f,
          vy = (random.nextFloat() - 0.5f) * 1.2f,
          color = 0x88AAAAAA,
          radius = random.nextFloat() * 14f + 8f,
          life = 0.6f,
          maxLife = 0.6f,
          type = ParticleType.SMOKE
        )
      )
    }
  }

  private fun emitExplosion(x: Float, y: Float) {
    emitSparks(x, y, 40)
    emitSmokeBurst(x, y, 25)
    for (i in 0 until 20) {
      particles.add(
        Particle(
          x = x,
          y = y,
          vx = (random.nextFloat() - 0.5f) * 3f,
          vy = (random.nextFloat() - 0.5f) * 3f,
          color = 0xFFFF1744,
          radius = random.nextFloat() * 18f + 10f,
          life = 0.8f,
          maxLife = 0.8f,
          type = ParticleType.NITRO_FLAME
        )
      )
    }
  }

  private fun updateParticles(dt: Float) {
    // Rain or dust atmospheric particles
    if (theme.hasRain && particles.size < 70) {
      particles.add(
        Particle(
          x = (random.nextFloat() - 0.5f) * 2.2f,
          y = 1f,
          vx = -0.3f,
          vy = 3.5f,
          color = 0x8890CAF9,
          radius = 2.5f,
          life = 0.5f,
          maxLife = 0.5f,
          type = ParticleType.RAIN
        )
      )
    } else if (theme.hasDust && particles.size < 40) {
      particles.add(
        Particle(
          x = (random.nextFloat() - 0.5f) * 2.2f,
          y = 1f,
          vx = (random.nextFloat() - 0.5f) * 0.8f,
          vy = random.nextFloat() * 1.2f + 0.8f,
          color = 0x55BCAAA4,
          radius = random.nextFloat() * 8f + 4f,
          life = 1f,
          maxLife = 1f,
          type = ParticleType.DUST
        )
      )
    }

    updateParticlesOnly(dt)
  }

  private fun updateParticlesOnly(dt: Float) {
    val iter = particles.iterator()
    while (iter.hasNext()) {
      val p = iter.next()
      p.life -= dt
      p.x += p.vx * dt
      p.y += p.vy * dt
      p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
      if (p.life <= 0f) {
        iter.remove()
      }
    }
  }
}
