package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple

enum class GameMode(val displayName: String, val description: String) {
  CLASSIC_ARCADE("Classic 3-Lane", "Classic 3-lane arcade racer: switch lanes with ◀ ▶ buttons, dodge traffic, and set records!"),
  ENDLESS_TRAFFIC("Traffic Rush", "Dodge heavy highway traffic, hit close calls, and survive at high speeds"),
  CAREER_GRAND_PRIX("Grand Prix", "Compete against 7 fierce rivals across 3 intense laps for the podium trophy"),
  DRIFT_TRIAL("Tokyo Drift", "Carve through mountain hairpins, chaining huge drift combos before time runs out"),
  POLICE_PURSUIT("Police Pursuit", "High-heat pursuit! Evade police cruisers, dodge roadblocks, and stay uncaught")
}

enum class TrackTheme(
  val displayName: String,
  val roadColor: Long,
  val shoulderColor: Long,
  val barrierColor: Long,
  val lineColor: Long,
  val skyColorTop: Long,
  val skyColorBottom: Long,
  val hasRain: Boolean = false,
  val hasDust: Boolean = false
) {
  CYBER_HIGHWAY(
    displayName = "Cyber Highway",
    roadColor = 0xFF141923,
    shoulderColor = 0xFF0D1117,
    barrierColor = 0xFF00E5FF,
    lineColor = 0xFF00E5FF,
    skyColorTop = 0xFF080C14,
    skyColorBottom = 0xFF172033
  ),
  TOKYO_PASS(
    displayName = "Tokyo Midnight",
    roadColor = 0xFF1A1C23,
    shoulderColor = 0xFF2D1828,
    barrierColor = 0xFFFF1744,
    lineColor = 0xFFFFD600,
    skyColorTop = 0xFF120B1A,
    skyColorBottom = 0xFF351538
  ),
  DESERT_CANYON(
    displayName = "Desert Canyon",
    roadColor = 0xFF2B2520,
    shoulderColor = 0xFF3E2723,
    barrierColor = 0xFFFF6D00,
    lineColor = 0xFFFFFFFF,
    skyColorTop = 0xFF2E1906,
    skyColorBottom = 0xFF8D4F1E,
    hasDust = true
  ),
  THUNDERSTORM_COAST(
    displayName = "Thunderstorm Coast",
    roadColor = 0xFF111720,
    shoulderColor = 0xFF090E17,
    barrierColor = 0xFF448AFF,
    lineColor = 0xFF82B1FF,
    skyColorTop = 0xFF0A0F1A,
    skyColorBottom = 0xFF1C2C45,
    hasRain = true
  )
}

data class CarDefinition(
  val id: String,
  val name: String,
  val tagLine: String,
  val baseSpeed: Int,       // 1 - 100
  val baseAccel: Int,       // 1 - 100
  val baseHandling: Int,    // 1 - 100
  val baseNitro: Int,       // 1 - 100
  val baseArmor: Int,       // 1 - 100
  val price: Int,
  val defaultBodyColor: Long,
  val defaultNeonColor: Long,
  val isUnlockedByDefault: Boolean = false
)

object CarCatalog {
  val ALL_CARS = listOf(
    CarDefinition(
      id = "car_interceptor",
      name = "Interceptor GT",
      tagLine = "Balanced American muscle with raw torque",
      baseSpeed = 65,
      baseAccel = 70,
      baseHandling = 60,
      baseNitro = 65,
      baseArmor = 75,
      price = 0,
      defaultBodyColor = 0xFFFF1744, // Red
      defaultNeonColor = 0xFFFF5722,
      isUnlockedByDefault = true
    ),
    CarDefinition(
      id = "car_viper",
      name = "Viper Drift R",
      tagLine = "Agile Japanese twin-turbo drift specialist",
      baseSpeed = 75,
      baseAccel = 82,
      baseHandling = 90,
      baseNitro = 70,
      baseArmor = 55,
      price = 2500,
      defaultBodyColor = 0xFF00E5FF, // Cyan
      defaultNeonColor = 0xFF00E5FF
    ),
    CarDefinition(
      id = "car_spectre",
      name = "Spectre Superleggera",
      tagLine = "Italian aerodynamic masterpiece built for top speed",
      baseSpeed = 88,
      baseAccel = 80,
      baseHandling = 75,
      baseNitro = 80,
      baseArmor = 60,
      price = 6000,
      defaultBodyColor = 0xFFFFD600, // Gold Yellow
      defaultNeonColor = 0xFFFFAB00
    ),
    CarDefinition(
      id = "car_phantom",
      name = "Cyber TR-99",
      tagLine = "Futuristic prototype with monstrous nitro boost",
      baseSpeed = 84,
      baseAccel = 92,
      baseHandling = 85,
      baseNitro = 95,
      baseArmor = 70,
      price = 12000,
      defaultBodyColor = 0xFFD500F9, // Neon Purple
      defaultNeonColor = 0xFFD500F9
    ),
    CarDefinition(
      id = "car_apex",
      name = "Apex Hyperion",
      tagLine = "The pinnacle of racing engineering - unbeatable king",
      baseSpeed = 98,
      baseAccel = 96,
      baseHandling = 95,
      baseNitro = 95,
      baseArmor = 90,
      price = 25000,
      defaultBodyColor = 0xFF00E676, // Neon Emerald
      defaultNeonColor = 0xFF00E5FF
    )
  )

  fun getCar(id: String): CarDefinition =
    ALL_CARS.find { it.id == id } ?: ALL_CARS.first()
}

data class CarUpgrades(
  val speedLevel: Int = 1,      // 1 to 5
  val accelLevel: Int = 1,
  val handlingLevel: Int = 1,
  val nitroLevel: Int = 1,
  val armorLevel: Int = 1
) {
  fun getUpgradeCost(statLevel: Int): Int {
    return when (statLevel) {
      1 -> 500
      2 -> 1200
      3 -> 2500
      4 -> 5000
      else -> 0
    }
  }
}

enum class CollectibleType {
  COIN,
  NITRO_CANISTER,
  SHIELD,
  REPAIR_WRENCH,
  DOUBLE_CASH,
  MAGNET,
  SPEED_RAMP
}

data class Collectible(
  val id: Long,
  val type: CollectibleType,
  var x: Float, // -0.8f to 0.8f across road
  var y: Float, // distance down road
  var isCollected: Boolean = false,
  val value: Int = 10
)

data class TrafficVehicle(
  val id: Long,
  var x: Float,            // -0.7f to 0.7f
  var y: Float,            // world distance along road
  var speed: Float,        // km/h
  var targetX: Float,      // lane changing target
  val carType: Int,        // 0: sedan, 1: sports, 2: truck, 3: suv
  val colorHex: Long,
  val isPolice: Boolean = false,
  var sirenPhase: Float = 0f,
  var isRivalRacer: Boolean = false,
  var rivalRank: Int = 0,
  var passedPlayer: Boolean = false,
  var width: Float = 0.22f,
  var length: Float = 60f
)

data class Particle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  val color: Long,
  var alpha: Float = 1f,
  val radius: Float,
  var life: Float = 1f,
  val maxLife: Float = 1f,
  val type: ParticleType
)

enum class ParticleType {
  SMOKE,
  SPARK,
  NITRO_FLAME,
  DRIFT_MARK,
  RAIN,
  DUST,
  CONFETTI
}

data class FloatingMessage(
  val id: Long,
  val text: String,
  var x: Float,
  var y: Float,
  val color: Long,
  var life: Float = 1f,
  val maxLife: Float = 1f
)

data class Achievement(
  val id: String,
  val title: String,
  val description: String,
  val rewardCash: Int,
  val isUnlocked: Boolean = false
)

object AchievementList {
  val DEFAULTS = listOf(
    Achievement("ach_first_race", "First Ignition", "Complete your first race run", 200),
    Achievement("ach_speed_200", "Speed Demon", "Exceed 200 km/h in any race", 500),
    Achievement("ach_speed_280", "Sonic Boom", "Exceed 280 km/h using Nitro boost", 1200),
    Achievement("ach_near_miss_10", "Danger Zone", "Perform 10 Close Call near-misses in one run", 800),
    Achievement("ach_drift_king", "Drift Master", "Score over 1,500 drift points", 1000),
    Achievement("ach_podium_win", "Champion", "Win 1st Place in Grand Prix", 2500),
    Achievement("ach_outrun_police", "Ghost Rider", "Survive 2,000m in Police Pursuit mode", 2000),
    Achievement("ach_collector", "Garage Tycoon", "Unlock 3 different vehicles", 3000)
  )
}
