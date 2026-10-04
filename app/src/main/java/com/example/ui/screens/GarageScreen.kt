package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RaceRepository
import com.example.model.CarCatalog
import com.example.model.CarDefinition
import com.example.model.CarUpgrades
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import kotlin.math.sin

@Composable
fun GarageScreen(
  repository: RaceRepository,
  onBackToMenu: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBackToMenu() }

  var playerCash by remember { mutableIntStateOf(repository.getCash()) }
  var equippedCarId by remember { mutableStateOf(repository.getSelectedCarId()) }
  var selectedCarId by remember { mutableStateOf(equippedCarId) }
  var unlockedCars by remember { mutableStateOf(repository.getUnlockedCars()) }

  val carDef = remember(selectedCarId) { CarCatalog.getCar(selectedCarId) }
  var upgrades by remember(selectedCarId) { mutableStateOf(repository.getCarUpgrades(selectedCarId)) }
  var currentBodyColor by remember(selectedCarId) { mutableStateOf(repository.getCarBodyColor(selectedCarId)) }
  var currentNeonColor by remember(selectedCarId) { mutableStateOf(repository.getCarNeonColor(selectedCarId)) }

  // Rotating Turntable animation
  val infiniteTransition = rememberInfiniteTransition(label = "turntable")
  val rotationDeg by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(12000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  val isUnlocked = unlockedCars.contains(selectedCarId)
  val isEquipped = equippedCarId == selectedCarId

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(bottom = 80.dp)
    ) {
      // -------------------------------------------------------------
      // HEADER BAR
      // -------------------------------------------------------------
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onBackToMenu,
              modifier = Modifier
                .size(42.dp)
                .background(DarkSurface, CircleShape)
                .border(1.dp, Color(0xFF2D3748), CircleShape)
                .testTag("garage_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "GARAGE",
              color = Color.White,
              fontSize = 22.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp
            )
          }

          // Cash pill
          Box(
            modifier = Modifier
              .background(DarkSurface, RoundedCornerShape(14.dp))
              .border(1.dp, NeonGold.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
              .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = "$ $playerCash",
              color = NeonGold,
              fontWeight = FontWeight.Black,
              fontSize = 15.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      // -------------------------------------------------------------
      // 360 TURNTABLE SHOWCASE
      // -------------------------------------------------------------
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          contentAlignment = Alignment.Center
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f

            // Neon Turntable platform ring
            drawOval(
              brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1D2430), Color(0xFF090D12)),
                center = Offset(cx, cy),
                radius = 140f
              ),
              topLeft = Offset(cx - 140f, cy - 60f),
              size = Size(280f, 120f)
            )

            // Neon Glow Ring
            drawOval(
              color = Color(currentNeonColor).copy(alpha = 0.7f),
              topLeft = Offset(cx - 135f, cy - 56f),
              size = Size(270f, 112f),
              style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )

            // Vehicle on turntable
            val carW = 50f
            val carH = 88f
            val wobbleY = (sin(rotationDeg * 0.05f) * 2f)

            rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
              // Shadow
              drawRoundRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(cx - carW * 0.5f, cy - carH * 0.5f + 6f),
                size = Size(carW, carH),
                cornerRadius = CornerRadius(14f, 14f)
              )

              // Neon Underglow
              drawRoundRect(
                color = Color(currentNeonColor).copy(alpha = 0.8f),
                topLeft = Offset(cx - carW * 0.65f, cy - carH * 0.55f),
                size = Size(carW * 1.3f, carH * 1.1f),
                cornerRadius = CornerRadius(20f, 20f)
              )

              // Car Body
              drawRoundRect(
                color = Color(currentBodyColor),
                topLeft = Offset(cx - carW * 0.46f, cy - carH * 0.48f + wobbleY),
                size = Size(carW * 0.92f, carH * 0.96f),
                cornerRadius = CornerRadius(14f, 14f)
              )

              // Center Racing Stripe
              drawRect(
                color = Color.White.copy(alpha = 0.9f),
                topLeft = Offset(cx - 3f, cy - carH * 0.48f + wobbleY),
                size = Size(6f, carH * 0.96f)
              )

              // Cockpit Glass
              drawRoundRect(
                color = Color(0xFF111827),
                topLeft = Offset(cx - carW * 0.32f, cy - carH * 0.22f + wobbleY),
                size = Size(carW * 0.64f, carH * 0.42f),
                cornerRadius = CornerRadius(8f, 8f)
              )

              // Headlights
              drawCircle(Color(0xFFE0F7FA), 5f, Offset(cx - carW * 0.3f, cy - carH * 0.44f + wobbleY))
              drawCircle(Color(0xFFE0F7FA), 5f, Offset(cx + carW * 0.3f, cy - carH * 0.44f + wobbleY))
            }
          }
        }
      }

      // -------------------------------------------------------------
      // CAR SELECTOR ROW
      // -------------------------------------------------------------
      item {
        LazyRow(
          modifier = Modifier.padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
          items(CarCatalog.ALL_CARS) { car ->
            val isCarSelected = car.id == selectedCarId
            val isCarUnlocked = unlockedCars.contains(car.id)
            val isCarEquipped = car.id == equippedCarId

            Card(
              onClick = { selectedCarId = car.id },
              colors = CardDefaults.cardColors(
                containerColor = if (isCarSelected) DarkSurfaceVariant else DarkSurface
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .width(135.dp)
                .border(
                  width = if (isCarSelected) 2.dp else 1.dp,
                  color = if (isCarSelected) NeonCyan else Color(0xFF21262D),
                  shape = RoundedCornerShape(14.dp)
                )
                .testTag("car_card_${car.id}")
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                // Color swatch
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .background(Color(car.defaultBodyColor), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  if (!isCarUnlocked) {
                    Icon(
                      imageVector = Icons.Default.Lock,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(12.dp)
                    )
                  } else if (isCarEquipped) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                  text = car.name,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  maxLines = 1
                )

                Text(
                  text = if (!isCarUnlocked) "$ ${car.price}" else if (isCarEquipped) "ACTIVE" else "OWNED",
                  color = if (!isCarUnlocked) NeonGold else if (isCarEquipped) NeonGreen else NeonCyan,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // CAR INFO & EQUIP / PURCHASE ACTIONS
      // -------------------------------------------------------------
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(16.dp))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = carDef.name,
                  color = Color.White,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Black
                )
                Text(
                  text = carDef.tagLine,
                  color = NeonCyan,
                  fontSize = 12.sp
                )
              }

              if (isUnlocked) {
                Button(
                  onClick = {
                    equippedCarId = selectedCarId
                    repository.setSelectedCarId(selectedCarId)
                  },
                  enabled = !isEquipped,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEquipped) NeonGreen else NeonCyan,
                    disabledContainerColor = NeonGreen.copy(alpha = 0.2f)
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("equip_car_button")
                ) {
                  Text(
                    text = if (isEquipped) "EQUIPPED" else "EQUIP",
                    color = if (isEquipped) NeonGreen else DarkBackground,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                  )
                }
              } else {
                Button(
                  onClick = {
                    if (playerCash >= carDef.price) {
                      playerCash = repository.addCash(-carDef.price)
                      repository.unlockCar(selectedCarId)
                      unlockedCars = repository.getUnlockedCars()
                      equippedCarId = selectedCarId
                      repository.setSelectedCarId(selectedCarId)
                      repository.checkAndUnlockAchievement("ach_collector")
                    }
                  },
                  enabled = playerCash >= carDef.price,
                  colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("buy_car_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "BUY $ ${carDef.price}",
                    color = DarkBackground,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // PERFORMANCE UPGRADE STATION
      // -------------------------------------------------------------
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          Text(
            text = "PERFORMANCE TUNING",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(10.dp))

          // Top Speed Upgrade
          UpgradeItem(
            title = "Top Speed",
            baseStat = carDef.baseSpeed,
            level = upgrades.speedLevel,
            playerCash = playerCash,
            onUpgrade = {
              val cost = upgrades.getUpgradeCost(upgrades.speedLevel)
              if (playerCash >= cost && upgrades.speedLevel < 5) {
                playerCash = repository.addCash(-cost)
                upgrades = upgrades.copy(speedLevel = upgrades.speedLevel + 1)
                repository.saveCarUpgrades(selectedCarId, upgrades)
              }
            }
          )

          // Acceleration Upgrade
          UpgradeItem(
            title = "Acceleration",
            baseStat = carDef.baseAccel,
            level = upgrades.accelLevel,
            playerCash = playerCash,
            onUpgrade = {
              val cost = upgrades.getUpgradeCost(upgrades.accelLevel)
              if (playerCash >= cost && upgrades.accelLevel < 5) {
                playerCash = repository.addCash(-cost)
                upgrades = upgrades.copy(accelLevel = upgrades.accelLevel + 1)
                repository.saveCarUpgrades(selectedCarId, upgrades)
              }
            }
          )

          // Handling Upgrade
          UpgradeItem(
            title = "Handling & Drift",
            baseStat = carDef.baseHandling,
            level = upgrades.handlingLevel,
            playerCash = playerCash,
            onUpgrade = {
              val cost = upgrades.getUpgradeCost(upgrades.handlingLevel)
              if (playerCash >= cost && upgrades.handlingLevel < 5) {
                playerCash = repository.addCash(-cost)
                upgrades = upgrades.copy(handlingLevel = upgrades.handlingLevel + 1)
                repository.saveCarUpgrades(selectedCarId, upgrades)
              }
            }
          )

          // Nitro Upgrade
          UpgradeItem(
            title = "Nitro Capacity",
            baseStat = carDef.baseNitro,
            level = upgrades.nitroLevel,
            playerCash = playerCash,
            onUpgrade = {
              val cost = upgrades.getUpgradeCost(upgrades.nitroLevel)
              if (playerCash >= cost && upgrades.nitroLevel < 5) {
                playerCash = repository.addCash(-cost)
                upgrades = upgrades.copy(nitroLevel = upgrades.nitroLevel + 1)
                repository.saveCarUpgrades(selectedCarId, upgrades)
              }
            }
          )

          // Armor Upgrade
          UpgradeItem(
            title = "Armor & Durability",
            baseStat = carDef.baseArmor,
            level = upgrades.armorLevel,
            playerCash = playerCash,
            onUpgrade = {
              val cost = upgrades.getUpgradeCost(upgrades.armorLevel)
              if (playerCash >= cost && upgrades.armorLevel < 5) {
                playerCash = repository.addCash(-cost)
                upgrades = upgrades.copy(armorLevel = upgrades.armorLevel + 1)
                repository.saveCarUpgrades(selectedCarId, upgrades)
              }
            }
          )
        }
      }

      // -------------------------------------------------------------
      // CUSTOM PAINT & NEON COLOR PALETTE
      // -------------------------------------------------------------
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(16.dp))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "BODY PAINT",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val paintColors = listOf(
              0xFFFF1744, 0xFF00E5FF, 0xFFFFD600, 0xFF00E676,
              0xFFD500F9, 0xFFFF6D00, 0xFFECEFF1, 0xFF212121
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              paintColors.forEach { colorHex ->
                val isSelected = currentBodyColor == colorHex
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .background(Color(colorHex), CircleShape)
                    .border(
                      width = if (isSelected) 2.dp else 1.dp,
                      color = if (isSelected) Color.White else Color.Transparent,
                      shape = CircleShape
                    )
                    .clickable {
                      currentBodyColor = colorHex
                      repository.setCarBodyColor(selectedCarId, colorHex)
                    },
                  contentAlignment = Alignment.Center
                ) {
                  if (isSelected) {
                    Box(
                      modifier = Modifier
                        .size(10.dp)
                        .background(Color.White, CircleShape)
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "NEON UNDERGLOW",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val neonColors = listOf(
              0xFF00E5FF, 0xFFFF1744, 0xFF00E676, 0xFFFFD600,
              0xFFD500F9, 0xFFFF5722, 0xFF2979FF
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              neonColors.forEach { colorHex ->
                val isSelected = currentNeonColor == colorHex
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .background(Color(colorHex), CircleShape)
                    .border(
                      width = if (isSelected) 2.dp else 1.dp,
                      color = if (isSelected) Color.White else Color.Transparent,
                      shape = CircleShape
                    )
                    .clickable {
                      currentNeonColor = colorHex
                      repository.setCarNeonColor(selectedCarId, colorHex)
                    },
                  contentAlignment = Alignment.Center
                ) {
                  if (isSelected) {
                    Box(
                      modifier = Modifier
                        .size(10.dp)
                        .background(Color.White, CircleShape)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun UpgradeItem(
  title: String,
  baseStat: Int,
  level: Int,
  playerCash: Int,
  onUpgrade: () -> Unit
) {
  val isMax = level >= 5
  val cost = when (level) {
    1 -> 500
    2 -> 1200
    3 -> 2500
    4 -> 5000
    else -> 0
  }
  val canAfford = playerCash >= cost && !isMax

  Card(
    colors = CardDefaults.cardColors(containerColor = DarkSurface),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .border(1.dp, Color(0xFF21262D), RoundedCornerShape(12.dp))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Lv $level/5",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Level pips (5 steps)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          for (i in 1..5) {
            Box(
              modifier = Modifier
                .width(24.dp)
                .height(6.dp)
                .background(
                  color = if (i <= level) NeonCyan else Color(0xFF21262D),
                  shape = RoundedCornerShape(3.dp)
                )
            )
          }
        }
      }

      // Upgrade Action Button
      Button(
        onClick = onUpgrade,
        enabled = canAfford,
        colors = ButtonDefaults.buttonColors(
          containerColor = NeonGold,
          disabledContainerColor = Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("upgrade_${title.lowercase().replace(" ", "_")}")
      ) {
        Text(
          text = if (isMax) "MAX" else "$ $cost",
          color = if (isMax) Color(0xFF8B949E) else if (canAfford) DarkBackground else Color(0xFF8B949E),
          fontWeight = FontWeight.Black,
          fontSize = 12.sp
        )
      }
    }
  }
}
