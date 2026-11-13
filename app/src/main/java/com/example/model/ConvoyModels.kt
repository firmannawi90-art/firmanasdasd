package com.example.model

enum class ConvoyRole(val titleTh: String, val badgeColorHex: Long) {
    LEAD("คันนำ (Lead)", 0xFF10B981),        // Emerald Green
    MEMBER("ร่วมขบวน (Member)", 0xFF3B82F6),   // Blue
    SWEEPER("ปิดท้าย (Sweeper)", 0xFFF59E0B)   // Amber
}

enum class VehicleStatus(val labelTh: String, val colorHex: Long) {
    IN_FORMATION("เกาะกลุ่มปกติ", 0xFF10B981),
    FALLEN_BEHIND("ระยะห่างมากเกินไป", 0xFFF59E0B),
    PULLING_OVER("กำลังจอดชะลอ", 0xFF6366F1),
    EMERGENCY("ขอความช่วยเหลือ", 0xFFEF4444)
}

data class ConvoyVehicle(
    val id: String,
    val callSign: String,
    val driverName: String,
    val model: String,
    val licensePlate: String,
    val role: ConvoyRole,
    val status: VehicleStatus = VehicleStatus.IN_FORMATION,
    val speedKmh: Int = 80,
    val distanceBehindLeadMeters: Int = 0,
    val fuelPercent: Int = 85,
    val tirePressureOk: Boolean = true,
    val positionProgress: Float = 0f // 0.0 to 1.0 along route
)

enum class PoiCategory(val labelTh: String, val iconName: String) {
    VIEWPOINT("จุดชมวิวธรรมชาติ", "landscape"),
    CULTURE_HISTORY("ประวัติศาสตร์ & วัฒนธรรม", "temple"),
    HAZARD_CURVE("จุดระวัง & โค้งอันตราย", "warning"),
    REST_STOP("จุดพักรถ & รวมพล", "coffee"),
    LOCAL_FOOD("ของอร่อยท้องถิ่น", "restaurant"),
    FUEL_STATION("ปั๊มน้ำมัน & จุดบริการ", "local_gas_station")
}

data class RoutePoint(
    val id: String,
    val name: String,
    val kilometer: Double,
    val category: PoiCategory,
    val description: String,
    val landmarkStoryPrompt: String,
    val elevationMeters: Int = 300,
    val progress: Float = 0f // 0.0 to 1.0
)

data class ConvoyTrip(
    val id: String,
    val title: String,
    val origin: String,
    val destination: String,
    val totalDistanceKm: Double,
    val estimatedDurationHours: Double,
    val targetSpeedKmh: Int,
    val currentProgress: Float = 0.15f,
    val waypoints: List<RoutePoint> = emptyList()
)

data class ConvoyAnnouncement(
    val id: String,
    val senderCallSign: String,
    val message: String,
    val timestamp: String,
    val isAlert: Boolean = false
)

enum class AiPersona(val titleTh: String, val descriptionTh: String) {
    CULTURAL_GUIDE("ไกด์สายประวัติศาสตร์ & ธรรมชาติ", "เล่าเรื่องราว ตำนาน และเสน่ห์ของสถานที่ที่กำลังผ่าน"),
    ROAD_CAPTAIN("โคไพลอตความปลอดภัยการขับขี่", "เตือนโค้งลาดชัน สภาพถนน และระยะห่างในขบวนคาราวาน"),
    STORYTELLER("เพื่อนร่วมทางสายบันเทิง", "เกร็ดความรู้สนุกๆ มุกตลก และแนะนำของกินเด็ดข้างทาง")
}

data class CommentaryItem(
    val id: String,
    val title: String,
    val text: String,
    val timestamp: String,
    val category: PoiCategory,
    val audioDurationSec: Int = 12
)
