package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.RaceRepository
import com.example.model.CarCatalog
import com.example.model.GameMode
import com.example.model.TrackTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink

@Composable
fun MainMenuScreen(
  repository: RaceRepository,
  selectedMode: GameMode,
  selectedTrack: TrackTheme,
  onModeSelected: (GameMode) -> Unit,
  onTrackSelected: (TrackTheme) -> Unit,
  onStartRace: () -> Unit,
  onGoToGarage: () -> Unit,
  onGoToStats: () -> Unit,
  onToggleSound: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val cash = remember { repository.getCash() }
  val selectedCarId = remember { repository.getSelectedCarId() }
  val currentCar = remember { CarCatalog.getCar(selectedCarId) }
  val highScore = remember(selectedMode) { repository.getHighScore(selectedMode) }

  var showSettingsDialog by remember { mutableStateOf(false) }
  var isSoundEnabled by remember { mutableStateOf(repository.isSoundEnabled()) }
  var isHapticsEnabled by remember { mutableStateOf(repository.isHapticsEnabled()) }
  var isTouchDrag by remember { mutableStateOf(repository.isTouchDragControls()) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(bottom = 90.dp)
    ) {
      // -------------------------------------------------------------
      // HERO BANNER & LOGO
      // -------------------------------------------------------------
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_race_hero),
            contentDescription = "Apex Racer Hero",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )

          // Gradient overlay for readability
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                brush = Brush.verticalGradient(
                  colors = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.8f), DarkBackground),
                  startY = 40f
                )
              )
          )

          // Top Row: Player Cash & Settings Icon
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Cash Pill
            Box(
              modifier = Modifier
                .background(DarkSurface.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .border(1.dp, NeonGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = "$ $cash",
                color = NeonGold,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
              )
            }

            // Settings Button
            IconButton(
              onClick = { showSettingsDialog = true },
              modifier = Modifier
                .size(42.dp)
                .background(DarkSurface.copy(alpha = 0.9f), CircleShape)
                .border(1.dp, Color(0xFF2D3748), CircleShape)
                .testTag("settings_button")
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          // Main Title & Tagline
          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(horizontal = 20.dp, vertical = 8.dp)
          ) {
            Text(
              text = "APEX RACER",
              fontSize = 36.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 2.sp,
              color = Color.White
            )
            Text(
              text = "HIGH-OCTANE RETRO ARCADE RACING",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = NeonCyan
            )
          }
        }
      }

      // -------------------------------------------------------------
      // EQUIPPED CAR SUMMARY & GARAGE SHORTCUT
      // -------------------------------------------------------------
      item {
        Card(
          onClick = onGoToGarage,
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(16.dp))
            .testTag("garage_shortcut_card")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "ACTIVE MACHINE",
                color = Color(0xFF8B949E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = currentCar.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = currentCar.tagLine,
                color = NeonCyan,
                fontSize = 12.sp
              )
            }

            Box(
              modifier = Modifier
                .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Build,
                  contentDescription = null,
                  tint = NeonCyan,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "GARAGE",
                  color = NeonCyan,
                  fontWeight = FontWeight.Black,
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // GAME MODE SELECTOR
      // -------------------------------------------------------------
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          Text(
            text = "SELECT MODE",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(10.dp))

          GameMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            Card(
              onClick = { onModeSelected(mode) },
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = if (isSelected) NeonCyan else Color(0xFF21262D),
                  shape = RoundedCornerShape(14.dp)
                )
                .testTag("mode_${mode.name.lowercase()}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = mode.displayName,
                    color = if (isSelected) NeonCyan else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                  )
                  Text(
                    text = mode.description,
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                  )
                }

                if (isSelected) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .background(NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.PlayArrow,
                      contentDescription = null,
                      tint = DarkBackground,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // TRACK THEME SELECTOR
      // -------------------------------------------------------------
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          Text(
            text = "CIRCUIT TRACK",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(10.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(TrackTheme.entries) { track ->
              val isTrackSelected = track == selectedTrack
              Card(
                onClick = { onTrackSelected(track) },
                colors = CardDefaults.cardColors(
                  containerColor = if (isTrackSelected) DarkSurfaceVariant else DarkSurface
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .width(160.dp)
                  .border(
                    width = if (isTrackSelected) 2.dp else 1.dp,
                    color = if (isTrackSelected) NeonOrange else Color(0xFF21262D),
                    shape = RoundedCornerShape(14.dp)
                  )
                  .testTag("track_${track.name.lowercase()}")
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  // Track color indicator line
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .background(Color(track.barrierColor), RoundedCornerShape(3.dp))
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = track.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  Text(
                    text = if (track.hasRain) "Rain / Slick" else if (track.hasDust) "Dust Storm" else "Clear Night",
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp
                  )
                }
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // BEST SCORE FOR CURRENT MODE
      // -------------------------------------------------------------
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.Center
        ) {
          Text(
            text = "RECORD SCORE: $highScore PTS",
            color = NeonGold,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    // -------------------------------------------------------------
    // BOTTOM STICKY ACTION BAR
    // -------------------------------------------------------------
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, DarkBackground, DarkBackground),
            startY = 0f
          )
        )
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Trophies / Stats Button
        IconButton(
          onClick = onGoToStats,
          modifier = Modifier
            .size(54.dp)
            .background(DarkSurface, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(14.dp))
            .testTag("stats_nav_button")
        ) {
          Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "Achievements",
            tint = NeonGold,
            modifier = Modifier.size(24.dp)
          )
        }

        // START RACE BUTTON
        Button(
          onClick = onStartRace,
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .weight(1f)
            .height(54.dp)
            .testTag("start_race_button")
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = DarkBackground,
            modifier = Modifier.size(26.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "IGNITION / RACE",
            color = DarkBackground,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            letterSpacing = 1.sp
          )
        }
      }
    }

    // -------------------------------------------------------------
    // SETTINGS DIALOG
    // -------------------------------------------------------------
    if (showSettingsDialog) {
      Dialog(onDismissRequest = { showSettingsDialog = false }) {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(20.dp))
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "AUDIO & CONTROLS",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sound Toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                  contentDescription = null,
                  tint = NeonCyan,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sound FX & Engine", color = Color.White, fontSize = 14.sp)
              }
              Switch(
                checked = isSoundEnabled,
                onCheckedChange = {
                  isSoundEnabled = it
                  repository.setSoundEnabled(it)
                  onToggleSound(it)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Haptics Toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Vibration,
                  contentDescription = null,
                  tint = NeonOrange,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Vibration Rumble", color = Color.White, fontSize = 14.sp)
              }
              Switch(
                checked = isHapticsEnabled,
                onCheckedChange = {
                  isHapticsEnabled = it
                  repository.setHapticsEnabled(it)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonOrange)
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Control Scheme: Buttons vs Drag
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (isTouchDrag) Icons.Default.TouchApp else Icons.Default.Gamepad,
                  contentDescription = null,
                  tint = NeonGreen,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text("Touch Drag Steer", color = Color.White, fontSize = 14.sp)
                  Text("Drag car directly", color = Color(0xFF8B949E), fontSize = 11.sp)
                }
              }
              Switch(
                checked = isTouchDrag,
                onCheckedChange = {
                  isTouchDrag = it
                  repository.setTouchDragControls(it)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
              )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = { showSettingsDialog = false },
              colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
              modifier = Modifier.fillMaxWidth().testTag("close_settings_button")
            ) {
              Text("DONE", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
