package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Achievement
import com.example.model.AchievementList
import com.example.model.CarCatalog
import com.example.model.CarUpgrades
import com.example.model.GameMode

class RaceRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("apex_racer_save_data", Context.MODE_PRIVATE)

  // Cash
  fun getCash(): Int = prefs.getInt("player_cash", 1200)

  fun addCash(amount: Int): Int {
    val newCash = (getCash() + amount).coerceAtLeast(0)
    prefs.edit().putInt("player_cash", newCash).apply()
    return newCash
  }

  // Selected Car
  fun getSelectedCarId(): String = prefs.getString("selected_car_id", "car_interceptor") ?: "car_interceptor"

  fun setSelectedCarId(carId: String) {
    prefs.edit().putString("selected_car_id", carId).apply()
  }

  // Unlocked Cars
  fun getUnlockedCars(): Set<String> {
    val saved = prefs.getStringSet("unlocked_cars", null)
    return saved ?: setOf("car_interceptor")
  }

  fun unlockCar(carId: String): Boolean {
    val current = getUnlockedCars().toMutableSet()
    val added = current.add(carId)
    if (added) {
      prefs.edit().putStringSet("unlocked_cars", current).apply()
    }
    return added
  }

  // Car Upgrades
  fun getCarUpgrades(carId: String): CarUpgrades {
    val speed = prefs.getInt("${carId}_upg_speed", 1)
    val accel = prefs.getInt("${carId}_upg_accel", 1)
    val handling = prefs.getInt("${carId}_upg_handling", 1)
    val nitro = prefs.getInt("${carId}_upg_nitro", 1)
    val armor = prefs.getInt("${carId}_upg_armor", 1)
    return CarUpgrades(speed, accel, handling, nitro, armor)
  }

  fun saveCarUpgrades(carId: String, upgrades: CarUpgrades) {
    prefs.edit()
      .putInt("${carId}_upg_speed", upgrades.speedLevel)
      .putInt("${carId}_upg_accel", upgrades.accelLevel)
      .putInt("${carId}_upg_handling", upgrades.handlingLevel)
      .putInt("${carId}_upg_nitro", upgrades.nitroLevel)
      .putInt("${carId}_upg_armor", upgrades.armorLevel)
      .apply()
  }

  // Custom colors
  fun getCarBodyColor(carId: String): Long {
    val defaultColor = CarCatalog.getCar(carId).defaultBodyColor
    return prefs.getLong("${carId}_custom_body", defaultColor)
  }

  fun setCarBodyColor(carId: String, color: Long) {
    prefs.edit().putLong("${carId}_custom_body", color).apply()
  }

  fun getCarNeonColor(carId: String): Long {
    val defaultColor = CarCatalog.getCar(carId).defaultNeonColor
    return prefs.getLong("${carId}_custom_neon", defaultColor)
  }

  fun setCarNeonColor(carId: String, color: Long) {
    prefs.edit().putLong("${carId}_custom_neon", color).apply()
  }

  // High Scores
  fun getHighScore(mode: GameMode): Int {
    return prefs.getInt("high_score_${mode.name}", 0)
  }

  fun updateHighScore(mode: GameMode, score: Int): Boolean {
    val current = getHighScore(mode)
    if (score > current) {
      prefs.edit().putInt("high_score_${mode.name}", score).apply()
      return true
    }
    return false
  }

  // Career Stats
  fun getTotalKmDriven(): Float = prefs.getFloat("stat_total_km", 0f)
  fun addKmDriven(km: Float) {
    prefs.edit().putFloat("stat_total_km", getTotalKmDriven() + km).apply()
  }

  fun getMaxSpeed(): Float = prefs.getFloat("stat_max_speed", 0f)
  fun recordMaxSpeed(speed: Float) {
    if (speed > getMaxSpeed()) {
      prefs.edit().putFloat("stat_max_speed", speed).apply()
    }
  }

  fun getTotalNearMisses(): Int = prefs.getInt("stat_near_misses", 0)
  fun addNearMisses(count: Int) {
    prefs.edit().putInt("stat_near_misses", getTotalNearMisses() + count).apply()
  }

  fun getTotalRacesWon(): Int = prefs.getInt("stat_races_won", 0)
  fun incrementRacesWon() {
    prefs.edit().putInt("stat_races_won", getTotalRacesWon() + 1).apply()
  }

  fun getTotalDriftScore(): Int = prefs.getInt("stat_drift_score", 0)
  fun addDriftScore(score: Int) {
    prefs.edit().putInt("stat_drift_score", getTotalDriftScore() + score).apply()
  }

  // Settings
  fun isSoundEnabled(): Boolean = prefs.getBoolean("setting_sound_enabled", true)
  fun setSoundEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("setting_sound_enabled", enabled).apply()
  }

  fun isHapticsEnabled(): Boolean = prefs.getBoolean("setting_haptics_enabled", true)
  fun setHapticsEnabled(enabled: Boolean) {
    prefs.edit().putBoolean("setting_haptics_enabled", enabled).apply()
  }

  fun isTouchDragControls(): Boolean = prefs.getBoolean("setting_touch_drag", false)
  fun setTouchDragControls(enabled: Boolean) {
    prefs.edit().putBoolean("setting_touch_drag", enabled).apply()
  }

  // Achievements
  fun getUnlockedAchievements(): Set<String> {
    return prefs.getStringSet("unlocked_achievements", emptySet()) ?: emptySet()
  }

  fun checkAndUnlockAchievement(achievementId: String): Boolean {
    val current = getUnlockedAchievements().toMutableSet()
    if (!current.contains(achievementId)) {
      current.add(achievementId)
      prefs.edit().putStringSet("unlocked_achievements", current).apply()
      val ach = AchievementList.DEFAULTS.find { it.id == achievementId }
      if (ach != null) {
        addCash(ach.rewardCash)
      }
      return true
    }
    return false
  }

  fun getAllAchievements(): List<Achievement> {
    val unlocked = getUnlockedAchievements()
    return AchievementList.DEFAULTS.map {
      it.copy(isUnlocked = unlocked.contains(it.id))
    }
  }
}
