package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConvoyVehicle
import com.example.model.RoutePoint
import com.example.model.VehicleStatus
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.TertiaryAmber

@Composable
fun ConvoyTopHudBar(
    currentSpeedKmh: Int,
    nextPoint: RoutePoint?,
    distanceToNextKm: Double,
    vehicles: List<ConvoyVehicle>,
    modifier: Modifier = Modifier,
    onRadioClick: () -> Unit
) {
    val fallenBehindCount = vehicles.count { it.status == VehicleStatus.FALLEN_BEHIND }
    val totalCount = vehicles.size

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
        color = Slate850.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Speedometer Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate900)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currentSpeedKmh",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryNeon,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = "กม./ชม.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Speed limit badge
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(2.dp, ErrorRed, CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "90",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 11.sp
                    )
                }
            }

            // Middle: Next Landmark / Turn Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TurnRight,
                        contentDescription = "Next Turn",
                        tint = PrimaryNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "อีก %.1f กม.".format(distanceToNextKm),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = nextPoint?.name ?: "กำลังมุ่งหน้าสู่ปลายทาง",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    maxLines = 1
                )
            }

            // Right: Convoy Formation Status & Quick Radio Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (fallenBehindCount > 0) TertiaryAmber.copy(alpha = 0.2f) else SecondaryTeal.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (fallenBehindCount > 0) TertiaryAmber else SecondaryTeal
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Convoy Count",
                            tint = if (fallenBehindCount > 0) TertiaryAmber else SecondaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalCount คัน",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Walkie-Talkie Quick Radio Alert
                IconButton(
                    onClick = onRadioClick,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1))
                        .testTag("quick_radio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "Convoy Radio Alert",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConvoyBottomDriveControls(
    isDriving: Boolean,
    onToggleDrive: () -> Unit,
    onSkipNextLandmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
        color = Slate850.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onToggleDrive,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDriving) Color(0xFFEF4444) else SecondaryTeal
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("toggle_drive_button")
            ) {
                Icon(
                    imageVector = if (isDriving) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isDriving) Color.White else Slate900
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDriving) "พักการเดินทาง" else "เริ่มเดินทางขบวน (Start)",
                    fontWeight = FontWeight.Bold,
                    color = if (isDriving) Color.White else Slate900,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            FilledTonalButton(
                onClick = onSkipNextLandmark,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("skip_next_landmark_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Next Landmark",
                    tint = PrimaryNeon
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "จุดต่อไป",
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryNeon
                )
            }
        }
    }
}
