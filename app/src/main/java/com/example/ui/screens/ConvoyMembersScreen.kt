package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.model.ConvoyRole
import com.example.model.ConvoyVehicle
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
import com.example.viewmodel.ConvoyUiState
import com.example.viewmodel.ConvoyViewModel

@Composable
fun ConvoyMembersScreen(
    uiState: ConvoyUiState,
    viewModel: ConvoyViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp)
            .testTag("convoy_members_screen")
    ) {
        item {
            // Header Stats Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate850),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ขบวนคาราวานเดินทาง (${uiState.vehicles.size} คัน)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "เส้นทาง: ${uiState.currentTrip.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FormationMetricItem(
                            label = "คันนำขบวน",
                            value = uiState.vehicles.firstOrNull { it.role == ConvoyRole.LEAD }?.callSign?.split(" ")?.firstOrNull() ?: "-",
                            color = SecondaryTeal
                        )
                        FormationMetricItem(
                            label = "ระยะห่างเฉลี่ย",
                            value = "75 เมตร",
                            color = PrimaryNeon
                        )
                        FormationMetricItem(
                            label = "คันปิดท้าย",
                            value = "เกาะกลุ่มดี",
                            color = TertiaryAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.addAnnouncement(
                                sender = "คันนำขบวน",
                                message = "ขอให้ทุกคันตรวจสอบระยะห่างในขบวน 50-80 เมตรและเตรียมรวมพลที่จุดพักรถ",
                                isAlert = true
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("regroup_convoy_button")
                    ) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = Slate900)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ส่งเสียงประกาศจัดระยะขบวน (Regroup)",
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "รายชื่อรถในคณะเดินทาง:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(uiState.vehicles, key = { it.id }) { vehicle ->
            VehicleCardItem(
                vehicle = vehicle,
                onStatusChange = { newStatus ->
                    viewModel.updateVehicleStatus(vehicle.id, newStatus)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FormationMetricItem(label: String, value: String, color: Color) {
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
private fun VehicleCardItem(
    vehicle: ConvoyVehicle,
    onStatusChange: (VehicleStatus) -> Unit
) {
    val roleColor = when (vehicle.role) {
        ConvoyRole.LEAD -> SecondaryTeal
        ConvoyRole.MEMBER -> PrimaryNeon
        ConvoyRole.SWEEPER -> TertiaryAmber
    }

    val statusColor = when (vehicle.status) {
        VehicleStatus.IN_FORMATION -> SecondaryTeal
        VehicleStatus.FALLEN_BEHIND -> TertiaryAmber
        VehicleStatus.PULLING_OVER -> Color(0xFF818CF8)
        VehicleStatus.EMERGENCY -> ErrorRed
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate850),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vehicle_card_${vehicle.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Role & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(roleColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = roleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = vehicle.callSign,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${vehicle.driverName} • ${vehicle.model}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = vehicle.status.labelTh,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate900)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ความเร็ว", style = MaterialTheme.typography.labelSmall, color = Slate400, fontSize = 10.sp)
                    Text(text = "${vehicle.speedKmh} กม./ชม.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ระยะห่างจากคันนำ", style = MaterialTheme.typography.labelSmall, color = Slate400, fontSize = 10.sp)
                    Text(text = "${vehicle.distanceBehindLeadMeters} ม.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PrimaryNeon)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ระดับน้ำมัน", style = MaterialTheme.typography.labelSmall, color = Slate400, fontSize = 10.sp)
                    Text(text = "${vehicle.fuelPercent}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (vehicle.fuelPercent > 30) SecondaryTeal else ErrorRed)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Quick Toggles for Convoy Simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (vehicle.status != VehicleStatus.IN_FORMATION) {
                    OutlinedButton(
                        onClick = { onStatusChange(VehicleStatus.IN_FORMATION) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("กลับเข้าขบวน", fontSize = 11.sp, color = SecondaryTeal)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onStatusChange(VehicleStatus.FALLEN_BEHIND) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("จำลองระยะห่าง", fontSize = 11.sp, color = TertiaryAmber)
                    }
                }
            }
        }
    }
}
