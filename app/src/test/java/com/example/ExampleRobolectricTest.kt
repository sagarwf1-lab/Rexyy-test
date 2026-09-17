package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.wakeword.WakeWordDetector
import com.example.features.reminders.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals("REXYY", appName)
    }

    @Test
    fun `wake word detector recognizes wake phrases and extracts commands`() {
        val match1 = WakeWordDetector.checkWakeWord("Hello REXYY YouTube kholo")
        assertTrue(match1.isWakeWordDetected)
        assertEquals("YouTube kholo", match1.extractedCommand)

        val match2 = WakeWordDetector.checkWakeWord("Hello REXYY")
        assertTrue(match2.isWakeWordDetected)
        assertEquals("", match2.extractedCommand)

        val match3 = WakeWordDetector.checkWakeWord("Hey REXYY volume 50 percent karo")
        assertTrue(match3.isWakeWordDetected)
        assertEquals("volume 50 percent karo", match3.extractedCommand)
    }

    @Test
    fun `reminder natural language time parser parses tomorrow and hours`() {
        val (title, epochMs) = ReminderScheduler.parseNaturalLanguageTimeToEpoch("Kal 8 baje project meet yaad dilana")
        assertTrue(epochMs > System.currentTimeMillis())
        assertTrue(title.isNotBlank())
    }
}
