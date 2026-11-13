package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ConvoyRole
import com.example.model.ConvoyVehicle
import com.example.model.PoiCategory
import com.example.model.RoutePoint
import com.example.model.VehicleStatus
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.TertiaryAmber
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ConvoyMapView(
    vehicles: List<ConvoyVehicle>,
    waypoints: List<RoutePoint>,
    currentProgress: Float,
    isDriving: Boolean,
    modifier: Modifier = Modifier,
    onWaypointClick: (RoutePoint) -> Unit = {}
) {
    // Interactive pan and zoom
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Pulsing animation for formation / active GPS tracking
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val routeGlowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "routeGlow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 3.5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .testTag("convoy_map_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Map Background Grid & Topographic lines
            drawMapGrid(canvasW, canvasH, scale, offsetX, offsetY)

            // 2. Generate Curving Road Waypoint positions
            val routeCoords = calculateRouteCoordinates(canvasW, canvasH, waypoints.size)

            // Draw Road Base (asphalt)
            val roadPath = Path().apply {
                if (routeCoords.isNotEmpty()) {
                    val p0 = transformPoint(routeCoords.first(), scale, offsetX, offsetY, canvasW, canvasH)
                    moveTo(p0.x, p0.y)
                    for (i in 1 until routeCoords.size) {
                        val prev = transformPoint(routeCoords[i - 1], scale, offsetX, offsetY, canvasW, canvasH)
                        val curr = transformPoint(routeCoords[i], scale, offsetX, offsetY, canvasW, canvasH)
                        val midX = (prev.x + curr.x) / 2
                        val midY = (prev.y + curr.y) / 2
                        quadraticTo(prev.x, prev.y, midX, midY)
                    }
                    val last = transformPoint(routeCoords.last(), scale, offsetX, offsetY, canvasW, canvasH)
                    lineTo(last.x, last.y)
                }
            }

            // Road border / shoulders
            drawPath(
                path = roadPath,
                color = Slate700,
                style = Stroke(
                    width = 44f * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Road surface (dark asphalt)
            drawPath(
                path = roadPath,
                color = Slate900,
                style = Stroke(
                    width = 38f * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Road Center line (dashed yellow highway lines)
            drawPath(
                path = roadPath,
                color = Color(0xFFFBBF24).copy(alpha = 0.6f),
                style = Stroke(
                    width = 3f * scale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * scale, 14f * scale), 0f),
                    cap = StrokeCap.Round
                )
            )

            // Active GPS Convoy Route Glow
            drawPath(
                path = roadPath,
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryNeon.copy(alpha = 0.85f), SecondaryTeal.copy(alpha = 0.85f))
                ),
                style = Stroke(
                    width = 8f * scale,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(30f * scale, 15f * scale),
                        -routeGlowOffset * scale
                    ),
                    cap = StrokeCap.Round
                )
            )

            // 3. Draw Waypoints / POI Pins
            waypoints.forEachIndexed { index, point ->
                if (index < routeCoords.size) {
                    val pt = transformPoint(routeCoords[index], scale, offsetX, offsetY, canvasW, canvasH)
                    drawWaypointPin(pt, point, scale)
                }
            }

            // 4. Calculate Vehicle Positions on the Path
            val vehiclePoints = mutableListOf<Offset>()
            vehicles.forEach { vehicle ->
                // Progress along the route
                val vProgress = (currentProgress - (vehicle.distanceBehindLeadMeters / 15000f)).coerceIn(0.02f, 0.98f)
                val pos = interpolateAlongRoute(routeCoords, vProgress)
                val transformedPos = transformPoint(pos, scale, offsetX, offsetY, canvasW, canvasH)
                vehiclePoints.add(transformedPos)
            }

            // Draw Convoy Formation Links between vehicles
            for (i in 0 until vehiclePoints.size - 1) {
                val start = vehiclePoints[i]
                val end = vehiclePoints[i + 1]
                val vNext = vehicles.getOrNull(i + 1)
                val lineColor = when (vNext?.status) {
                    VehicleStatus.FALLEN_BEHIND -> TertiaryAmber
                    VehicleStatus.EMERGENCY -> ErrorRed
                    else -> SecondaryTeal.copy(alpha = pulseAlpha)
                }

                drawLine(
                    color = lineColor,
                    start = start,
                    end = end,
                    strokeWidth = 4f * scale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * scale, 8f * scale), 0f),
                    cap = StrokeCap.Round
                )
            }

            // 5. Draw Convoy Vehicles
            vehicles.forEachIndexed { index, vehicle ->
                if (index < vehiclePoints.size) {
                    val pt = vehiclePoints[index]
                    drawConvoyVehicleIcon(pt, vehicle, scale, pulseAlpha)
                }
            }
        }

        // Overlay Navigation Controls (Re-center, Zoom In, Zoom Out)
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            shape = CircleShape,
            color = Slate800.copy(alpha = 0.9f),
            shadowElevation = 6.dp
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(4.dp)
            ) {
                IconButton(
                    onClick = { scale = (scale * 1.25f).coerceAtMost(3.5f) },
                    modifier = Modifier.testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { scale = (scale / 1.25f).coerceAtLeast(0.6f) },
                    modifier = Modifier.testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = {
                        scale = 1.0f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier.testTag("recenter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Re-center Map",
                        tint = PrimaryNeon
                    )
                }
            }
        }

        // Compass Rose Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(44.dp),
            shape = CircleShape,
            color = Slate850.copy(alpha = 0.85f),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "North Compass",
                    tint = PrimaryNeon,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun transformPoint(
    point: Offset,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    canvasW: Float,
    canvasH: Float
): Offset {
    val cx = canvasW / 2f
    val cy = canvasH / 2f
    return Offset(
        x = (point.x - cx) * scale + cx + offsetX,
        y = (point.y - cy) * scale + cy + offsetY
    )
}

private fun calculateRouteCoordinates(w: Float, h: Float, count: Int): List<Offset> {
    val points = mutableListOf<Offset>()
    val steps = (count + 2).coerceAtLeast(6)
    val marginX = w * 0.15f
    val availableW = w - (marginX * 2)

    for (i in 0 until steps) {
        val t = i.toFloat() / (steps - 1)
        val x = marginX + (t * availableW)
        // Realistic mountain pass S-curves
        val wave1 = sin(t * Math.PI.toFloat() * 2.8f) * (h * 0.22f)
        val wave2 = cos(t * Math.PI.toFloat() * 1.5f) * (h * 0.10f)
        val y = (h * 0.50f) + wave1 + wave2
        points.add(Offset(x, y))
    }
    return points
}

private fun interpolateAlongRoute(route: List<Offset>, progress: Float): Offset {
    if (route.isEmpty()) return Offset.Zero
    if (route.size == 1) return route.first()

    val totalSegments = route.size - 1
    val scaledProgress = progress * totalSegments
    val segmentIndex = scaledProgress.toInt().coerceIn(0, totalSegments - 1)
    val segmentFraction = scaledProgress - segmentIndex

    val p1 = route[segmentIndex]
    val p2 = route[segmentIndex + 1]

    return Offset(
        x = p1.x + (p2.x - p1.x) * segmentFraction,
        y = p1.y + (p2.y - p1.y) * segmentFraction
    )
}

private fun DrawScope.drawMapGrid(w: Float, h: Float, scale: Float, offX: Float, offY: Float) {
    val gridSize = 60f * scale
    val startX = (offX % gridSize)
    val startY = (offY % gridSize)

    var x = startX
    while (x < w) {
        drawLine(
            color = Color(0xFF1E293B).copy(alpha = 0.35f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1f
        )
        x += gridSize
    }

    var y = startY
    while (y < h) {
        drawLine(
            color = Color(0xFF1E293B).copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f
        )
        y += gridSize
    }
}

private fun DrawScope.drawWaypointPin(point: Offset, wp: RoutePoint, scale: Float) {
    val pinColor = when (wp.category) {
        PoiCategory.VIEWPOINT -> Color(0xFF10B981)
        PoiCategory.CULTURE_HISTORY -> Color(0xFF8B5CF6)
        PoiCategory.HAZARD_CURVE -> Color(0xFFEF4444)
        PoiCategory.REST_STOP -> Color(0xFF0EA5E9)
        PoiCategory.LOCAL_FOOD -> Color(0xFFF59E0B)
        PoiCategory.FUEL_STATION -> Color(0xFF06B6D4)
    }

    // Outer glow
    drawCircle(
        color = pinColor.copy(alpha = 0.25f),
        radius = 18f * scale,
        center = point
    )

    // Pin circle
    drawCircle(
        color = pinColor,
        radius = 10f * scale,
        center = point
    )
    drawCircle(
        color = Color.White,
        radius = 4f * scale,
        center = point
    )

    // Label text
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = (11f * scale).coerceIn(18f, 32f)
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
    }

    drawContext.canvas.nativeCanvas.drawText(
        wp.name,
        point.x - 20f,
        point.y - (16f * scale),
        paint
    )
}

private fun DrawScope.drawConvoyVehicleIcon(
    point: Offset,
    vehicle: ConvoyVehicle,
    scale: Float,
    pulseAlpha: Float
) {
    val roleColor = when (vehicle.role) {
        ConvoyRole.LEAD -> SecondaryTeal
        ConvoyRole.MEMBER -> PrimaryNeon
        ConvoyRole.SWEEPER -> TertiaryAmber
    }

    val statusColor = when (vehicle.status) {
        VehicleStatus.FALLEN_BEHIND -> TertiaryAmber
        VehicleStatus.EMERGENCY -> ErrorRed
        else -> roleColor
    }

    // Outer radar halo for lead or alert cars
    if (vehicle.role == ConvoyRole.LEAD || vehicle.status != VehicleStatus.IN_FORMATION) {
        drawCircle(
            color = statusColor.copy(alpha = pulseAlpha * 0.4f),
            radius = 28f * scale,
            center = point
        )
    }

    // Vehicle Marker Background
    drawCircle(
        color = Slate900,
        radius = 16f * scale,
        center = point
    )

    drawCircle(
        color = statusColor,
        radius = 13f * scale,
        center = point
    )

    // Vehicle Core Dot
    drawCircle(
        color = Color.White,
        radius = 5f * scale,
        center = point
    )

    // Callsign Label badge
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = (10f * scale).coerceIn(16f, 28f)
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(3f, 0f, 2f, android.graphics.Color.BLACK)
    }

    val label = "${vehicle.callSign} (${vehicle.speedKmh} กม./ชม.)"
    drawContext.canvas.nativeCanvas.drawText(
        label,
        point.x - 30f,
        point.y + (28f * scale),
        textPaint
    )
}
