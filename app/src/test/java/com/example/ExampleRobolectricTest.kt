package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ConvoyRole
import com.example.model.ConvoyVehicle
import com.example.model.VehicleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ConvoyNav AI", appName)
    }

    @Test
    fun `convoy vehicle formation initialization`() {
        val leadVehicle = ConvoyVehicle(
            id = "lead_1",
            callSign = "01 นำขบวน (Alpha)",
            driverName = "สมชาย",
            model = "Toyota Fortuner",
            licensePlate = "กข-1234",
            role = ConvoyRole.LEAD,
            status = VehicleStatus.IN_FORMATION,
            speedKmh = 80
        )
        assertNotNull(leadVehicle)
        assertEquals(ConvoyRole.LEAD, leadVehicle.role)
        assertEquals(VehicleStatus.IN_FORMATION, leadVehicle.status)
        assertEquals(80, leadVehicle.speedKmh)
    }
}
