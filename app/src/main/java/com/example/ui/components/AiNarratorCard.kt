package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiPersona
import com.example.ui.theme.AudioWaveActive
import com.example.ui.theme.AudioWaveInactive
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900

@Composable
fun AiNarratorCard(
    currentStoryTitle: String,
    currentStoryText: String,
    isSpeaking: Boolean,
    isLoadingAi: Boolean,
    activePersona: AiPersona,
    speechRate: Float,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onReplayClick: () -> Unit,
    onCycleSpeedRate: () -> Unit,
    onAskAiClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isSpeaking) {
                        listOf(PrimaryNeon, SecondaryTeal)
                    } else {
                        listOf(Slate700, Slate800)
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("ai_narrator_card"),
        color = Slate850.copy(alpha = 0.96f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: AI Badge & Persona & Waveform
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF8B5CF6), PrimaryNeon))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Icon",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI ไกด์นำทาง & บรรยายสด",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = activePersona.titleTh,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Animated Soundwave Indicator
                if (isSpeaking) {
                    AudioSoundwaveAnimation()
                } else if (isLoadingAi) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = PrimaryNeon
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Landmark Story Title
            if (currentStoryTitle.isNotBlank()) {
                Text(
                    text = currentStoryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryNeon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Narration Text Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Slate900.copy(alpha = 0.8f)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isLoadingAi) {
                            "AI กำลังประมวลผลเรื่องราวและข้อควรระวังสำหรับเส้นทางนี้..."
                        } else if (currentStoryText.isNotBlank()) {
                            currentStoryText
                        } else {
                            "ขบวนกำลังเคลื่อนที่ ระบบ AI จะบรรยายประวัติศาสตร์ ทัศนียภาพ และจุดเลี้ยวสำคัญให้โดยอัตโนมัติ"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Player Controls & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play / Stop / Repeat Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSpeaking) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = onStopClick,
                                modifier = Modifier.testTag("ai_stop_tts_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Voice Narration",
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = SecondaryTeal,
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = onPlayClick,
                                modifier = Modifier.testTag("ai_play_tts_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Play Voice Narration",
                                    tint = Slate900
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onReplayClick,
                        modifier = Modifier.testTag("ai_replay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Replay Narration",
                            tint = Slate400
                        )
                    }

                    // Speech Rate selector (0.8x, 1.0x, 1.2x)
                    Surface(
                        onClick = onCycleSpeedRate,
                        shape = RoundedCornerShape(8.dp),
                        color = Slate800,
                        modifier = Modifier.testTag("speech_rate_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speech Rate",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%.1fx".format(speechRate),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Ask AI Button
                Surface(
                    onClick = onAskAiClick,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                    modifier = Modifier.testTag("ask_ai_voice_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Ask AI",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ถาม AI เพิ่มเติม",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC7D2FE)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioSoundwaveAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val h1 by transition.animateFloat(
        initialValue = 6f, targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 18f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 8f, targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by transition.animateFloat(
        initialValue = 22f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h4"
    )
    val h5 by transition.animateFloat(
        initialValue = 10f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h5"
    )

    Row(
        modifier = modifier.height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        SoundBar(height = h1.dp)
        SoundBar(height = h2.dp)
        SoundBar(height = h3.dp)
        SoundBar(height = h4.dp)
        SoundBar(height = h5.dp)
    }
}

@Composable
private fun SoundBar(height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(3.5.dp)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(AudioWaveActive)
    )
}
