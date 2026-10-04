package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RaceRepository
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink

@Composable
fun StatsScreen(
  repository: RaceRepository,
  onBackToMenu: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBackToMenu() }

  val totalKm = remember { repository.getTotalKmDriven() }
  val maxSpeed = remember { repository.getMaxSpeed() }
  val nearMisses = remember { repository.getTotalNearMisses() }
  val racesWon = remember { repository.getTotalRacesWon() }
  val driftScore = remember { repository.getTotalDriftScore() }
  val achievements = remember { repository.getAllAchievements() }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onBackToMenu,
            modifier = Modifier
              .size(42.dp)
              .background(DarkSurface, CircleShape)
              .border(1.dp, Color(0xFF2D3748), CircleShape)
              .testTag("stats_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "CAREER & TROPHIES",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Lifetime Records Grid Card
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(16.dp))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "LIFETIME TELEMETRY",
              color = NeonCyan,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              RecordTile("Total Distance", String.format("%.1f km", totalKm), NeonGold)
              RecordTile("Top Speed", "${maxSpeed.toInt()} km/h", NeonOrange)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              RecordTile("Close Calls", "$nearMisses", NeonPink)
              RecordTile("Grand Prix Wins", "$racesWon", NeonGreen)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              RecordTile("Drift Points", "$driftScore pts", Color(0xFFD500F9))
            }
          }
        }
        Spacer(modifier = Modifier.height(20.dp))
      }

      // Achievements Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "TROPHY ROOM",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
          val unlockedCount = achievements.count { it.isUnlocked }
          Text(
            text = "$unlockedCount / ${achievements.size}",
            color = NeonGold,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // Achievement Items
      items(achievements) { ach ->
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (ach.isUnlocked) DarkSurfaceVariant else DarkSurface
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
              width = 1.dp,
              color = if (ach.isUnlocked) NeonGold.copy(alpha = 0.5f) else Color(0xFF21262D),
              shape = RoundedCornerShape(14.dp)
            )
            .testTag("achievement_${ach.id}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .background(
                  if (ach.isUnlocked) NeonGold.copy(alpha = 0.2f) else Color(0xFF161B22),
                  CircleShape
                )
                .border(
                  1.dp,
                  if (ach.isUnlocked) NeonGold else Color(0xFF2D3748),
                  CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (ach.isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                contentDescription = null,
                tint = if (ach.isUnlocked) NeonGold else Color(0xFF484F58),
                modifier = Modifier.size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = ach.title,
                color = if (ach.isUnlocked) Color.White else Color(0xFF8B949E),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Text(
                text = ach.description,
                color = Color(0xFF8B949E),
                fontSize = 12.sp
              )
            }

            Box(
              modifier = Modifier
                .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "+$ ${ach.rewardCash}",
                color = if (ach.isUnlocked) NeonGreen else Color(0xFF8B949E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RecordTile(label: String, value: String, valueColor: Color) {
  Column {
    Text(text = label, color = Color(0xFF8B949E), fontSize = 11.sp)
    Text(
      text = value,
      color = valueColor,
      fontWeight = FontWeight.Black,
      fontSize = 16.sp,
      fontFamily = FontFamily.Monospace
    )
  }
}
