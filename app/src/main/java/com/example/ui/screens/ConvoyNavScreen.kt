package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiPersona
import com.example.ui.components.AiNarratorCard
import com.example.ui.components.ConvoyBottomDriveControls
import com.example.ui.components.ConvoyMapView
import com.example.ui.components.ConvoyTopHudBar
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.viewmodel.ConvoyUiState
import com.example.viewmodel.ConvoyViewModel

@Composable
fun ConvoyNavScreen(
    uiState: ConvoyUiState,
    isSpeaking: Boolean,
    speechRate: Float,
    viewModel: ConvoyViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("convoy_nav_screen")
    ) {
        // 1. Interactive Canvas Map View
        ConvoyMapView(
            vehicles = uiState.vehicles,
            waypoints = uiState.currentTrip.waypoints,
            currentProgress = uiState.currentProgress,
            isDriving = uiState.isDriving,
            modifier = Modifier.fillMaxSize(),
            onWaypointClick = { wp ->
                viewModel.narrateWaypoint(wp)
            }
        )

        // 2. Top HUD Bar (Speed, Next Landmark, Convoy Count, Quick Radio)
        ConvoyTopHudBar(
            currentSpeedKmh = uiState.currentSpeedKmh,
            nextPoint = uiState.nextPoint,
            distanceToNextKm = uiState.distanceToNextKm,
            vehicles = uiState.vehicles,
            modifier = Modifier.align(Alignment.TopCenter),
            onRadioClick = { viewModel.setShowRadioDialog(true) }
        )

        // 3. Bottom HUD Elements: AI Narrator Card & Drive Simulation Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Floating AI Voice Narrator Card
            AiNarratorCard(
                currentStoryTitle = uiState.currentStoryTitle,
                currentStoryText = uiState.currentStoryText,
                isSpeaking = isSpeaking,
                isLoadingAi = uiState.isLoadingAi,
                activePersona = uiState.activePersona,
                speechRate = speechRate,
                onPlayClick = { viewModel.speakNarration(uiState.currentStoryText) },
                onStopClick = { viewModel.stopSpeaking() },
                onReplayClick = { viewModel.speakNarration(uiState.currentStoryText) },
                onCycleSpeedRate = { viewModel.cycleSpeechRate() },
                onAskAiClick = { viewModel.setShowAskAiDialog(true) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Drive Controls
            ConvoyBottomDriveControls(
                isDriving = uiState.isDriving,
                onToggleDrive = { viewModel.toggleDriving() },
                onSkipNextLandmark = { viewModel.skipToNextLandmark() }
            )
        }

        // Dialog: Ask AI Driving Voice Assistant
        if (uiState.showAskAiDialog) {
            AskAiDialog(
                onDismiss = { viewModel.setShowAskAiDialog(false) },
                onAsk = { question -> viewModel.askDriverQuestion(question) },
                onSelectPersona = { persona -> viewModel.setPersona(persona) },
                currentPersona = uiState.activePersona
            )
        }

        // Dialog: Convoy Walkie-Talkie Radio Dispatch
        if (uiState.showRadioDialog) {
            ConvoyRadioDialog(
                onDismiss = { viewModel.setShowRadioDialog(false) },
                onBroadcast = { sender, message, isAlert ->
                    viewModel.addAnnouncement(sender, message, isAlert)
                    viewModel.setShowRadioDialog(false)
                }
            )
        }
    }
}

@Composable
private fun AskAiDialog(
    onDismiss: () -> Unit,
    onAsk: (String) -> Unit,
    onSelectPersona: (AiPersona) -> Unit,
    currentPersona: AiPersona
) {
    var queryText by remember { mutableStateOf("") }
    val presetQuestions = listOf(
        "เล่าประวัติศาสตร์และตำนานแถวนี้ให้ฟังหน่อย",
        "สภาพถนนและโค้งข้างหน้า ต้องระวังอะไรเป็นพิเศษ",
        "แนะนำจุดชมวิวและมุมถ่ายรูปสวยที่สุดข้างหน้า",
        "ของกินและของฝากขึ้นชื่อประจำถิ่นมีอะไรบ้าง",
        "จุดพักรถและปั๊มน้ำมันที่ใกล้ที่สุดอยู่ตรงไหน"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate850,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = PrimaryNeon,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ถาม AI ไกด์นำทาง & บรรยาย",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "สไตล์เสียง AI:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Persona selector pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiPersona.values().forEach { p ->
                        val selected = p == currentPersona
                        Surface(
                            onClick = { onSelectPersona(p) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) PrimaryNeon.copy(alpha = 0.2f) else Slate800,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) PrimaryNeon else Slate700
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p.name.split("_").first().lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) PrimaryNeon else Slate400,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "เลือกคำถามด่วนสำหรับคนขับ:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(6.dp))

                presetQuestions.take(3).forEach { q ->
                    Surface(
                        onClick = { onAsk(q) },
                        shape = RoundedCornerShape(10.dp),
                        color = Slate800,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Text(
                            text = "• $q",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = { Text("หรือพิมพ์คำถามเฉพาะเส้นทาง...", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryNeon,
                        unfocusedBorderColor = Slate700,
                        cursorColor = PrimaryNeon
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ask_ai_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (queryText.isNotBlank()) onAsk(queryText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                modifier = Modifier.testTag("submit_ai_question_button")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Slate900)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ถามและให้อ่านเสียง", color = Slate900, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ปิด", color = Slate400)
            }
        }
    )
}

@Composable
private fun ConvoyRadioDialog(
    onDismiss: () -> Unit,
    onBroadcast: (sender: String, message: String, isAlert: Boolean) -> Unit
) {
    val quickCalls = listOf(
        Triple("01 คันนำ", "ขบวนเตรียมชะลอความเร็ว มีโค้งชันด้านหน้า", true),
        Triple("01 คันนำ", "เตรียมแวะปั๊มน้ำมันและจุดพักรถข้างหน้า", false),
        Triple("04 ปิดท้าย", "คันปิดท้ายตามทันแล้ว ขบวนรวมกลุ่มสมบูรณ์", false),
        Triple("03 สมาชิก", "ขอชะลอเล็กน้อย มีรถบรรทุกแทรกขบวน", true),
        Triple("01 คันนำ", "เร่งความเร็วขบวนขึ้นเป็น 80-90 กม./ชม.", false)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate850,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = SecondaryTeal,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "วิทยุสื่อสารขบวนคาราวาน (Radio)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "แตะเพื่อกระจายเสียงเตือนในขบวน (TTS จะอ่านทันที):",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(10.dp))

                quickCalls.forEach { (sender, msg, isAlert) ->
                    Surface(
                        onClick = { onBroadcast(sender, msg, isAlert) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAlert) Color(0xFFEF4444).copy(alpha = 0.15f) else Slate800,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAlert) Color(0xFFEF4444).copy(alpha = 0.5f) else Slate700
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAlert) Icons.Default.Warning else Icons.Default.Radio,
                                contentDescription = null,
                                tint = if (isAlert) Color(0xFFEF4444) else SecondaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = sender,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlert) Color(0xFFFCA5A5) else SecondaryTeal
                                )
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ปิด", color = Slate400)
            }
        }
    )
}
