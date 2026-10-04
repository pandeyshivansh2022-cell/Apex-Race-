package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.audio.AudioEngine
import com.example.data.RaceRepository
import com.example.model.GameMode
import com.example.model.TrackTheme
import com.example.ui.screens.ClassicArcadeScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.RacingScreen
import com.example.ui.screens.StatsScreen

enum class AppScreen {
  MENU,
  RACING,
  CLASSIC_RACING,
  GARAGE,
  STATS
}

@Composable
fun ApexRacerApp(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val repository = remember { RaceRepository(context) }
  val audioEngine = remember { AudioEngine() }

  var currentScreen by remember { mutableStateOf(AppScreen.MENU) }
  var selectedMode by remember { mutableStateOf(GameMode.CLASSIC_ARCADE) }
  var selectedTrack by remember { mutableStateOf(TrackTheme.CYBER_HIGHWAY) }

  // Audio lifecycle observer
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
          audioEngine.stopEngine()
        }
        Lifecycle.Event.ON_DESTROY -> {
          audioEngine.release()
        }
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      audioEngine.release()
    }
  }

  Crossfade(targetState = currentScreen, label = "screen_crossfade", modifier = modifier.fillMaxSize()) { screen ->
    when (screen) {
      AppScreen.MENU -> {
        MainMenuScreen(
          repository = repository,
          selectedMode = selectedMode,
          selectedTrack = selectedTrack,
          onModeSelected = { selectedMode = it },
          onTrackSelected = { selectedTrack = it },
          onStartRace = {
            currentScreen = if (selectedMode == GameMode.CLASSIC_ARCADE) {
              AppScreen.CLASSIC_RACING
            } else {
              AppScreen.RACING
            }
          },
          onGoToGarage = { currentScreen = AppScreen.GARAGE },
          onGoToStats = { currentScreen = AppScreen.STATS },
          onToggleSound = { enabled -> audioEngine.setMuted(!enabled) }
        )
      }
      AppScreen.CLASSIC_RACING -> {
        ClassicArcadeScreen(
          repository = repository,
          audioEngine = audioEngine,
          onBackToMenu = { currentScreen = AppScreen.MENU }
        )
      }
      AppScreen.RACING -> {
        RacingScreen(
          mode = selectedMode,
          theme = selectedTrack,
          repository = repository,
          audioEngine = audioEngine,
          onExitToMenu = { currentScreen = AppScreen.MENU },
          onGoToGarage = { currentScreen = AppScreen.GARAGE }
        )
      }
      AppScreen.GARAGE -> {
        GarageScreen(
          repository = repository,
          onBackToMenu = { currentScreen = AppScreen.MENU }
        )
      }
      AppScreen.STATS -> {
        StatsScreen(
          repository = repository,
          onBackToMenu = { currentScreen = AppScreen.MENU }
        )
      }
    }
  }
}
