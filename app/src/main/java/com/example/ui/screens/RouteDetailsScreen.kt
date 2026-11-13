package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TempleBuddhist
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PoiCategory
import com.example.model.RoutePoint
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.TertiaryAmber
import com.example.viewmodel.ConvoyUiState
import com.example.viewmodel.ConvoyViewModel
import com.example.viewmodel.NavScreen

@Composable
fun RouteDetailsScreen(
    uiState: ConvoyUiState,
    viewModel: ConvoyViewModel,
    modifier: Modifier = Modifier
) {
    val trip = uiState.currentTrip

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp)
            .testTag("route_details_screen")
    ) {
        item {
            // Route Overview Header
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate850),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = trip.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${trip.origin} ➔ ${trip.destination}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TripStatItem(label = "ระยะทางทั้งหมด", value = "${trip.totalDistanceKm} กม.", color = Color.White)
                        TripStatItem(label = "เวลาเดินทางโดยประมาณ", value = "${trip.estimatedDurationHours} ชม.", color = SecondaryTeal)
                        TripStatItem(label = "ความเร็วขบวนเป้าหมาย", value = "${trip.targetSpeedKmh} กม./ชม.", color = TertiaryAmber)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Road characteristic badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate800,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = TertiaryAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "เส้นทางภูเขาสูงชัน 1,864 โค้ง กรุณาขับตามสัญญาณวิทยุคันนำและเว้นระยะห่างปลอดภัย",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "จุดสำคัญและจุดพักระหว่างเส้นทาง (${trip.waypoints.size} จุด):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(trip.waypoints, key = { it.id }) { point ->
            WaypointItemCard(
                point = point,
                isNextPoint = uiState.nextPoint?.id == point.id,
                onNarrateNow = {
                    viewModel.narrateWaypoint(point)
                    viewModel.setScreen(NavScreen.MAP)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TripStatItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate400)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun WaypointItemCard(
    point: RoutePoint,
    isNextPoint: Boolean,
    onNarrateNow: () -> Unit
) {
    val (icon, categoryColor) = when (point.category) {
        PoiCategory.VIEWPOINT -> Pair(Icons.Default.Landscape, Color(0xFF10B981))
        PoiCategory.CULTURE_HISTORY -> Pair(Icons.Default.TempleBuddhist, Color(0xFF8B5CF6))
        PoiCategory.HAZARD_CURVE -> Pair(Icons.Default.Warning, Color(0xFFEF4444))
        PoiCategory.REST_STOP -> Pair(Icons.Default.Coffee, Color(0xFF0EA5E9))
        PoiCategory.LOCAL_FOOD -> Pair(Icons.Default.Restaurant, Color(0xFFF59E0B))
        PoiCategory.FUEL_STATION -> Pair(Icons.Default.LocalGasStation, Color(0xFF06B6D4))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNextPoint) Slate800 else Slate850
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isNextPoint) 1.5.dp else 1.dp,
            if (isNextPoint) PrimaryNeon else Slate700
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = point.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isNextPoint) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryNeon
                        ) {
                            Text(
                                text = "จุดถัดไป",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "กม. ที่ %.1f • สูง %d ม. • %s".format(
                        point.kilometer,
                        point.elevationMeters,
                        point.category.labelTh
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = point.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // One tap AI audio narration
            Surface(
                onClick = onNarrateNow,
                shape = CircleShape,
                color = SecondaryTeal.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryTeal),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Narrate with AI",
                        tint = SecondaryTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
