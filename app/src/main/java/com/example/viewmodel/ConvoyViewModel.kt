package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiNarratorEngine
import com.example.audio.TextToSpeechManager
import com.example.model.AiPersona
import com.example.model.CommentaryItem
import com.example.model.ConvoyAnnouncement
import com.example.model.ConvoyRole
import com.example.model.ConvoyTrip
import com.example.model.ConvoyVehicle
import com.example.model.PoiCategory
import com.example.model.RoutePoint
import com.example.model.VehicleStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "ConvoyViewModel"

enum class NavScreen {
    MAP,
    CONVOY_ROSTER,
    AI_STORIES,
    ROUTE_DETAILS
}

data class ConvoyUiState(
    val currentScreen: NavScreen = NavScreen.MAP,
    val currentTrip: ConvoyTrip,
    val vehicles: List<ConvoyVehicle> = emptyList(),
    val currentProgress: Float = 0.08f,
    val isDriving: Boolean = false,
    val currentSpeedKmh: Int = 78,
    val nextPoint: RoutePoint? = null,
    val distanceToNextKm: Double = 3.2,
    // AI Narrator State
    val currentStoryTitle: String = "",
    val currentStoryText: String = "",
    val isLoadingAi: Boolean = false,
    val activePersona: AiPersona = AiPersona.CULTURAL_GUIDE,
    val commentaryHistory: List<CommentaryItem> = emptyList(),
    val announcements: List<ConvoyAnnouncement> = emptyList(),
    val showAskAiDialog: Boolean = false,
    val showRadioDialog: Boolean = false,
    val errorMessage: String? = null
)

class ConvoyViewModel(context: Context) : ViewModel() {

    private val ttsManager = TextToSpeechManager(context)
    private val aiNarrator = AiNarratorEngine()

    val isSpeaking = ttsManager.isSpeaking
    val speechRate = ttsManager.speechRate

    private var simulationJob: Job? = null
    private var lastNarratedPointId: String? = null

    private val initialTrip = createDefaultTrip()
    private val initialVehicles = createDefaultVehicles()

    private val _uiState = MutableStateFlow(
        ConvoyUiState(
            currentTrip = initialTrip,
            vehicles = initialVehicles,
            currentProgress = 0.08f,
            nextPoint = initialTrip.waypoints.firstOrNull(),
            currentStoryTitle = initialTrip.waypoints.firstOrNull()?.name ?: "จุดเริ่มต้นการเดินทาง",
            currentStoryText = "ยินดีต้อนรับสู่ขบวนคาราวาน AI ระบบพร้อมนำทางและบรรยายประวัติศาสตร์ วิวทิวทัศน์ ตลอดจนความปลอดภัยขณะขับรถตลอดเส้นทางครับ"
        )
    )
    val uiState: StateFlow<ConvoyUiState> = _uiState.asStateFlow()

    init {
        // Trigger welcome audio greeting
        viewModelScope.launch {
            delay(1200)
            speakNarration(_uiState.value.currentStoryText)
        }
    }

    fun setScreen(screen: NavScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun toggleDriving() {
        val willDrive = !_uiState.value.isDriving
        _uiState.update { it.copy(isDriving = willDrive) }

        if (willDrive) {
            startSimulation()
            addAnnouncement("01 นำขบวน", "ขบวนคาราวานเริ่มออกเดินทางแล้ว รักษาระยะห่าง 50-80 เมตรครับ")
        } else {
            stopSimulation()
            addAnnouncement("01 นำขบวน", "พักการเดินทางชั่วคราว จอดพักรถในจุดที่ปลอดภัย")
        }
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (isActive && _uiState.value.isDriving) {
                delay(1000)
                advanceRouteProgress()
            }
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    private fun advanceRouteProgress() {
        val currentProg = _uiState.value.currentProgress
        val newProgress = if (currentProg >= 0.98f) 0.05f else currentProg + 0.008f
        val trip = _uiState.value.currentTrip

        // Determine next waypoint
        val totalWaypoints = trip.waypoints
        val currentKm = newProgress * trip.totalDistanceKm
        val nextWp = totalWaypoints.firstOrNull { it.kilometer > currentKm } ?: totalWaypoints.lastOrNull()
        val distToNext = if (nextWp != null) (nextWp.kilometer - currentKm).coerceAtLeast(0.1) else 0.5

        // Check if we just crossed or arrived at a waypoint to auto-narrate
        val nearestWp = totalWaypoints.minByOrNull { kotlin.math.abs(it.kilometer - currentKm) }
        if (nearestWp != null && kotlin.math.abs(nearestWp.kilometer - currentKm) < 1.8) {
            if (lastNarratedPointId != nearestWp.id) {
                lastNarratedPointId = nearestWp.id
                narrateWaypoint(nearestWp)
            }
        }

        // Slight realistic speed variation
        val speedFluctuation = (75..88).random()

        _uiState.update {
            it.copy(
                currentProgress = newProgress,
                currentSpeedKmh = speedFluctuation,
                nextPoint = nextWp,
                distanceToNextKm = distToNext
            )
        }
    }

    fun skipToNextLandmark() {
        val trip = _uiState.value.currentTrip
        val currentKm = _uiState.value.currentProgress * trip.totalDistanceKm
        val nextWp = trip.waypoints.firstOrNull { it.kilometer > currentKm + 0.5 }
            ?: trip.waypoints.firstOrNull()

        if (nextWp != null) {
            val targetProgress = (nextWp.kilometer / trip.totalDistanceKm).toFloat().coerceIn(0.02f, 0.96f)
            _uiState.update {
                it.copy(
                    currentProgress = targetProgress,
                    nextPoint = nextWp,
                    distanceToNextKm = 0.5
                )
            }
            narrateWaypoint(nextWp)
        }
    }

    fun narrateWaypoint(point: RoutePoint) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingAi = true,
                    currentStoryTitle = point.name
                )
            }

            val result = aiNarrator.generateLandmarkNarration(
                point = point,
                trip = _uiState.value.currentTrip,
                persona = _uiState.value.activePersona,
                convoySpeedKmh = _uiState.value.currentSpeedKmh
            )

            val text = result.getOrDefault(point.description)
            val commentary = CommentaryItem(
                id = UUID.randomUUID().toString(),
                title = point.name,
                text = text,
                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                category = point.category
            )

            _uiState.update {
                it.copy(
                    isLoadingAi = false,
                    currentStoryText = text,
                    commentaryHistory = listOf(commentary) + it.commentaryHistory
                )
            }

            // Speak out loud automatically via TTS!
            speakNarration(text)
        }
    }

    fun askDriverQuestion(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAi = true, showAskAiDialog = false) }

            val result = aiNarrator.answerDriverQuestion(
                question = question,
                currentPoint = _uiState.value.nextPoint,
                trip = _uiState.value.currentTrip
            )

            val text = result.getOrElse { "ขออภัยครับ ขณะนี้มีคลื่นสัญญาณจำกัด กรุณาขับขี่ด้วยความระมัดระวัง" }
            val commentary = CommentaryItem(
                id = UUID.randomUUID().toString(),
                title = "คำถามจากคณะ: $question",
                text = text,
                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                category = PoiCategory.CULTURE_HISTORY
            )

            _uiState.update {
                it.copy(
                    isLoadingAi = false,
                    currentStoryTitle = "ตอบข้อสงสัยผู้ขับขี่",
                    currentStoryText = text,
                    commentaryHistory = listOf(commentary) + it.commentaryHistory
                )
            }

            speakNarration(text)
        }
    }

    fun speakNarration(text: String) {
        ttsManager.speak(text)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun cycleSpeechRate() {
        val current = ttsManager.speechRate.value
        val next = when {
            current < 0.95f -> 1.0f
            current < 1.15f -> 1.2f
            else -> 0.8f
        }
        ttsManager.setSpeechRate(next)
    }

    fun setPersona(persona: AiPersona) {
        _uiState.update { it.copy(activePersona = persona) }
        val msg = "เปลี่ยนบุคลิกภาพไกด์เป็น: ${persona.titleTh}"
        addAnnouncement("ระบบ AI", msg)
        speakNarration(msg)
    }

    fun addAnnouncement(sender: String, message: String, isAlert: Boolean = false) {
        val announcement = ConvoyAnnouncement(
            id = UUID.randomUUID().toString(),
            senderCallSign = sender,
            message = message,
            timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            isAlert = isAlert
        )
        _uiState.update {
            it.copy(announcements = listOf(announcement) + it.announcements)
        }
        if (isAlert) {
            ttsManager.speak("แจ้งเตือนขบวน: $message")
        }
    }

    fun updateVehicleStatus(vehicleId: String, newStatus: VehicleStatus) {
        _uiState.update { state ->
            val updated = state.vehicles.map {
                if (it.id == vehicleId) it.copy(status = newStatus) else it
            }
            state.copy(vehicles = updated)
        }
        val target = _uiState.value.vehicles.firstOrNull { it.id == vehicleId }
        if (target != null && newStatus != VehicleStatus.IN_FORMATION) {
            addAnnouncement(target.callSign, "สถานะเปลี่ยนเป็น: ${newStatus.labelTh}", isAlert = true)
        }
    }

    fun setShowAskAiDialog(show: Boolean) {
        _uiState.update { it.copy(showAskAiDialog = show) }
    }

    fun setShowRadioDialog(show: Boolean) {
        _uiState.update { it.copy(showRadioDialog = show) }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        ttsManager.release()
    }

    private fun createDefaultTrip(): ConvoyTrip {
        return ConvoyTrip(
            id = "trip_mae_hong_son",
            title = "คาราวาน 1,864 โค้งสู่ปาย & แม่ฮ่องสอน",
            origin = "เชียงใหม่ (ประตูช้างเผือก)",
            destination = "แม่ฮ่องสอน (พระธาตุดอยกองมู)",
            totalDistanceKm = 245.0,
            estimatedDurationHours = 5.5,
            targetSpeedKmh = 75,
            waypoints = listOf(
                RoutePoint(
                    id = "wp_1",
                    name = "จุดพักรถ ปตท. แม่ริม",
                    kilometer = 15.0,
                    category = PoiCategory.REST_STOP,
                    description = "จุดรวมพลและเช็กความพร้อมยานพาหนะ เติมน้ำมัน และเซ็ตวิทยุสื่อสารขบวน",
                    landmarkStoryPrompt = "จุดพักรถประตูสู่อำเภอแม่ริม จุดนัดพบยอดนิยมของคาราวานสายเหนือ",
                    elevationMeters = 310
                ),
                RoutePoint(
                    id = "wp_2",
                    name = "น้ำตกหมอกฟ้า อุทยานฯ ดอยสุเทพ-ปุย",
                    kilometer = 45.0,
                    category = PoiCategory.VIEWPOINT,
                    description = "น้ำตกสูงตระหง่านกลางม่านหมอก สองข้างทางเริ่มเข้าสู่แนวป่าเขาเขียวขจี",
                    landmarkStoryPrompt = "น้ำตกหมอกฟ้า ความงามธรรมชาติกับม่านละอองน้ำดุจสายหมอก",
                    elevationMeters = 720
                ),
                RoutePoint(
                    id = "wp_3",
                    name = "โค้งพับผ้า กม. 62 (จุดระวังทางลาดชัน)",
                    kilometer = 62.0,
                    category = PoiCategory.HAZARD_CURVE,
                    description = "แนวโค้งสลับฟันปลาต่อเนื่อง 12 โค้ง ให้ใช้เกียร์ต่ำและเว้นระยะห่าง 100 เมตร",
                    landmarkStoryPrompt = "โค้งพับผ้า จุดทดสอบฝีมือคาราวาน ต้องขับช้าและไม่แซงในทางโค้ง",
                    elevationMeters = 1080
                ),
                RoutePoint(
                    id = "wp_4",
                    name = "จุดชมวิวดอยกิ่วลม (ยอดดอยสูง)",
                    kilometer = 88.0,
                    category = PoiCategory.VIEWPOINT,
                    description = "จุดชมทิวทัศน์ 360 องศาสูงสุดในเส้นทางปาย อากาศเย็นสบายตลอดปี มีร้านกาแฟดอย",
                    landmarkStoryPrompt = "ดอยกิ่วลม จุดสูงสุดที่ลมพัดผ่านตลอดปี ชมทะเลหมอกและทิวเขาแดนลาว",
                    elevationMeters = 1431
                ),
                RoutePoint(
                    id = "wp_5",
                    name = "สะพานประวัติศาสตร์ท่าปาย",
                    kilometer = 112.0,
                    category = PoiCategory.CULTURE_HISTORY,
                    description = "สะพานเหล็กโบราณสร้างในสมัยสงครามโลกครั้งที่ 2 สัญลักษณ์การเข้าสู่อำเภอปาย",
                    landmarkStoryPrompt = "สะพานประวัติศาสตร์ท่าปาย เรื่องเล่าเส้นทางลำเลียงสมัยสงครามมหาเอเชียบูรพา",
                    elevationMeters = 480
                ),
                RoutePoint(
                    id = "wp_6",
                    name = "จุดแวะชิม ขนมจีนยูนนาน & ชาอู่หลง ปาย",
                    kilometer = 125.0,
                    category = PoiCategory.LOCAL_FOOD,
                    description = "ของอร่อยขึ้นชื่อ ชุมชนวัฒนธรรมจีนยูนนาน หมู่บ้านสันติชล",
                    landmarkStoryPrompt = "อาหารจีนยูนนาน ขาหมูหมั่นโถว และวิถีชีวิตดั้งเดิมที่สันติชล",
                    elevationMeters = 520
                ),
                RoutePoint(
                    id = "wp_7",
                    name = "พระธาตุดอยกองมู แม่ฮ่องสอน",
                    kilometer = 245.0,
                    category = PoiCategory.CULTURE_HISTORY,
                    description = "พระธาตุคู่บ้านคู่เมืองแม่ฮ่องสอน ศิลปะไทยใหญ่ จุดชมวิวเมืองสามหมอกแบบพาโนรามา",
                    landmarkStoryPrompt = "วัดพระธาตุดอยกองมู ปลายทางแห่งศรัทธาและรางวัลของขบวนคาราวาน 1864 โค้ง",
                    elevationMeters = 950
                )
            )
        )
    }

    private fun createDefaultVehicles(): List<ConvoyVehicle> {
        return listOf(
            ConvoyVehicle(
                id = "v_1",
                callSign = "01 นำขบวน (Alpha)",
                driverName = "พี่เอก สหภาพ",
                model = "Toyota Fortuner 2.8 4WD",
                licensePlate = "กข-8899 เชียงใหม่",
                role = ConvoyRole.LEAD,
                status = VehicleStatus.IN_FORMATION,
                speedKmh = 80,
                distanceBehindLeadMeters = 0,
                fuelPercent = 90
            ),
            ConvoyVehicle(
                id = "v_2",
                callSign = "02 รองนำ (Beta)",
                driverName = "หมอนนท์",
                model = "Ford Ranger Raptor",
                licensePlate = "ฮฮ-1234 กทม.",
                role = ConvoyRole.MEMBER,
                status = VehicleStatus.IN_FORMATION,
                speedKmh = 79,
                distanceBehindLeadMeters = 65,
                fuelPercent = 82
            ),
            ConvoyVehicle(
                id = "v_3",
                callSign = "03 คันกลาง (Charlie)",
                driverName = "คุณแป้ง & ก๊วน",
                model = "Honda CR-V e:HEV",
                licensePlate = "งง-5678 กทม.",
                role = ConvoyRole.MEMBER,
                status = VehicleStatus.IN_FORMATION,
                speedKmh = 77,
                distanceBehindLeadMeters = 140,
                fuelPercent = 75
            ),
            ConvoyVehicle(
                id = "v_4",
                callSign = "04 ปิดท้าย (Sweeper)",
                driverName = "ช่างเก่ง ดูแลขบวน",
                model = "Isuzu D-Max V-Cross",
                licensePlate = "ขข-9900 เชียงใหม่",
                role = ConvoyRole.SWEEPER,
                status = VehicleStatus.IN_FORMATION,
                speedKmh = 78,
                distanceBehindLeadMeters = 210,
                fuelPercent = 88
            )
        )
    }
}

class ConvoyViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ConvoyViewModel(context.applicationContext) as T
    }
}
