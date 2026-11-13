package com.example.ai

import android.util.Log
import com.example.model.AiPersona
import com.example.model.ConvoyTrip
import com.example.model.RoutePoint
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "AiNarratorEngine"

class AiNarratorEngine {

    // Lazy initialization of Gemini model via Firebase AI Logic
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 350
            }
        )
    }

    /**
     * Generates a conversational, engaging driving commentary script (in Thai)
     * suitable for hands-free speech output (TTS) to the travel convoy.
     */
    suspend fun generateLandmarkNarration(
        point: RoutePoint,
        trip: ConvoyTrip,
        persona: AiPersona,
        convoySpeedKmh: Int
    ): Result<String> {
        val prompt = buildString {
            append("คุณคือผู้ช่วยนำทางและไกด์เสียงประจำขบวนคาราวานท่องเที่ยว (Convoy Roadtrip AI Guide)\n")
            append("บุคลิกภาพ: ${persona.titleTh} - ${persona.descriptionTh}\n")
            append("เส้นทางปัจจุบัน: ${trip.title} จาก ${trip.origin} มุ่งหน้า ${trip.destination}\n")
            append("จุดสำคัญที่กำลังผ่านหรือใกล้ถึง: ${point.name} (กิโลเมตรที่ ${point.kilometer})\n")
            append("หมวดหมู่: ${point.category.labelTh}, ระดับความสูง: ${point.elevationMeters} เมตรเหนือระดับน้ำทะเล\n")
            append("ข้อมูลจุดสังเกต: ${point.description}\n")
            append("ความเร็วขบวนคาราวานขณะนี้: $convoySpeedKmh กม./ชม.\n\n")
            append("คำแนะนำการสร้างเสียงบรรยาย:\n")
            append("1. บรรยายด้วยน้ำเสียงที่เป็นมิตร น่าฟัง กระชับ ชัดเจน เหมาะสำหรับอ่านออกเสียงด้วย Text-To-Speech ให้คนขับและคณะเดินทางฟัง\n")
            append("2. ความยาวประมาณ 2-4 ประโยค (ประมาณ 50-80 คำ) ไม่สั้นเกินไปและไม่เวิ่นเว้อ\n")
            append("3. สอดแทรกเกร็ดประวัติศาสตร์ ธรรมชาติ ข้อควรระวังในการขับขี่ขบวน หรือของอร่อยท้องถิ่นตามสไตล์บุคลิกภาพที่เลือก\n")
            append("4. ตอบเป็นข้อความธรรมดาสำหรับอ่านทันที ไม่ต้องใส่เครื่องหมาย Markdown เช่น ดอกจัน หนา เอียง หรือหัวข้อใดๆ")
        }

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                val cleanedText = cleanTextForTts(text)
                Result.success(cleanedText)
            } else {
                Result.success(getFallbackNarration(point, persona))
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Gemini narration timed out, using contextual fallback", e)
            Result.success(getFallbackNarration(point, persona))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed: ${e.message}, falling back to preset story", e)
            Result.success(getFallbackNarration(point, persona))
        }
    }

    /**
     * Answers a voice question asked by the driver or passengers in the convoy.
     */
    suspend fun answerDriverQuestion(
        question: String,
        currentPoint: RoutePoint?,
        trip: ConvoyTrip
    ): Result<String> {
        val prompt = buildString {
            append("คุณคือผู้ช่วย AI นำทางขบวนรถยนต์คาราวาน กำลังให้ข้อมูลแก่คนขับรถและคณะเดินทาง\n")
            append("เส้นทางปัจจุบัน: ${trip.title} (ใกล้ ${currentPoint?.name ?: trip.origin})\n")
            append("คำถามจากผู้ขับขี่: $question\n\n")
            append("ข้อกำหนด:\n")
            append("1. ตอบเป็นภาษาไทยที่สุภาพ กระชับ เข้าใจง่าย ภายใน 2-3 ประโยค สำหรับให้ระบบสังเคราะห์เสียงอ่านออกลำโพงรถยนต์ทันที\n")
            append("2. เน้นข้อมูลที่เป็นประโยชน์ ปลอดภัยต่อการขับขี่รถเป็นขบวน\n")
            append("3. ห้ามใช้ Markdown สัญลักษณ์ หรือ Bullet points ให้ใช้ประโยคพูดปกติเท่านั้น")
        }

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(cleanTextForTts(text))
            } else {
                Result.success("ขณะนี้กำลังเดินทางบนเส้นทาง ${trip.title} หากมีข้อสงสัยเกี่ยวกับจุดแวะพักหรือโค้งข้างหน้า สามารถสอบถามระบบได้ตลอดเวลาครับ")
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Answer generation failed", e)
            Result.failure(e)
        }
    }

    private fun cleanTextForTts(raw: String): String {
        return raw.replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace(">", "")
            .replace("- ", "")
            .replace("\n\n", " ")
            .replace("\n", " ")
            .trim()
    }

    private fun getFallbackNarration(point: RoutePoint, persona: AiPersona): String {
        return when (persona) {
            AiPersona.ROAD_CAPTAIN -> {
                "ขบวนคาราวานกำลังเข้าใกล้ ${point.name} ในระยะอีกไม่ไกล ขอให้คันนำรักษาระยะห่างจากคันหลังประมาณ 50 ถึง 100 เมตร และใช้เกียร์ต่ำในจุดลาดชันครับ"
            }
            AiPersona.STORYTELLER -> {
                "ใกล้ถึง ${point.name} แล้วครับทุกคน แถวนี้วิวสวยมาก อย่าลืมเตรียมกล้องและเปิดกระจกรับลมเย็นๆ พร้อมชิมของอร่อยประจำถิ่นกันนะครับ"
            }
            AiPersona.CULTURAL_GUIDE -> {
                "คณะเดินทางกำลังผ่าน ${point.name} ซึ่งมีความสำคัญและเรื่องเล่าประวัติศาสตร์อันยาวนานของเส้นทางสายนี้ บรรยากาศธรรมชาติสองข้างทางร่มรื่นและเป็นเอกลักษณ์มากครับ"
            }
        }
    }
}
