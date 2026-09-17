package com.example.core.security

import android.os.Build
import java.io.File

enum class RootState {
    NOT_ROOTED,
    ROOT_DETECTED,
    ROOT_PERMISSION_DENIED,
    ROOT_PERMISSION_GRANTED,
    UNKNOWN
}

object RootDetector {

    private val SU_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/magisk/.core/bin/su"
    )

    fun detectRoot(): RootState {
        return try {
            val hasTestKeys = Build.TAGS != null && Build.TAGS.contains("test-keys")
            val hasSuBinary = checkSuBinary()
            val hasWhichSu = checkWhichSu()

            if (hasSuBinary || hasWhichSu || hasTestKeys) {
                RootState.ROOT_DETECTED
            } else {
                RootState.NOT_ROOTED
            }
        } catch (_: Exception) {
            RootState.UNKNOWN
        }
    }

    private fun checkSuBinary(): Boolean {
        for (path in SU_PATHS) {
            try {
                if (File(path).exists()) return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    private fun checkWhichSu(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            try {
                process = Runtime.getRuntime().exec(arrayOf("which", "su"))
                val exitCode = process.waitFor()
                exitCode == 0
            } catch (_: Exception) {
                false
            }
        } finally {
            process?.destroy()
        }
    }
}
