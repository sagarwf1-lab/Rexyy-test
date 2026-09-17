package com.example.features.root

import com.example.core.security.RootDetector
import com.example.core.security.RootState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class CommandResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)

enum class RootAction {
    REBOOT_NORMAL,
    REBOOT_RECOVERY,
    REBOOT_BOOTLOADER,
    CLEAR_APP_CACHE,
    GET_SELINUX_STATUS,
    GET_BATTERY_CHARGE_COUNTER
}

class RootManager {

    private val _rootState = MutableStateFlow(RootState.UNKNOWN)
    val rootState: StateFlow<RootState> = _rootState.asStateFlow()

    init {
        refreshRootDetection()
    }

    fun refreshRootDetection() {
        val detected = RootDetector.detectRoot()
        _rootState.value = detected
    }

    suspend fun requestRootPermission(): Boolean = withContext(Dispatchers.IO) {
        val currentState = _rootState.value
        if (currentState == RootState.NOT_ROOTED) {
            return@withContext false
        }

        try {
            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream
            os.write("id\n".toByteArray())
            os.write("exit\n".toByteArray())
            os.flush()
            os.close()

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                _rootState.value = RootState.ROOT_PERMISSION_GRANTED
                true
            } else {
                _rootState.value = RootState.ROOT_PERMISSION_DENIED
                false
            }
        } catch (_: Exception) {
            _rootState.value = RootState.ROOT_PERMISSION_DENIED
            false
        }
    }

    fun isRootAvailable(): Boolean {
        return _rootState.value == RootState.ROOT_PERMISSION_GRANTED
    }

    suspend fun executeSafeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        if (!isRootAvailable()) {
            return@withContext CommandResult(
                success = false,
                stdout = "",
                stderr = "Ye action normal Android mode mein available nahi hai. Root access required hai.",
                exitCode = -1
            )
        }

        // Safety filter to prevent hazardous actions
        val sanitized = command.trim()
        val blacklistedTokens = listOf("rm -rf /", "rm -rf /*", "mkfs", "dd if=", "wipe", "> /dev/block")
        if (blacklistedTokens.any { sanitized.contains(it) }) {
            return@withContext CommandResult(
                success = false,
                stdout = "",
                stderr = "Dangerous command blocked for safety, Sagar Sir.",
                exitCode = -2
            )
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", sanitized))
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()
            CommandResult(
                success = exitCode == 0,
                stdout = stdout.trim(),
                stderr = stderr.trim(),
                exitCode = exitCode
            )
        } catch (e: Exception) {
            CommandResult(
                success = false,
                stdout = "",
                stderr = e.localizedMessage ?: "Unknown execution error",
                exitCode = -1
            )
        }
    }

    suspend fun reboot(): Boolean {
        val res = executeSafeCommand("reboot")
        return res.success
    }

    suspend fun rebootRecovery(): Boolean {
        val res = executeSafeCommand("reboot recovery")
        return res.success
    }

    suspend fun rebootBootloader(): Boolean {
        val res = executeSafeCommand("reboot bootloader")
        return res.success
    }

    suspend fun executeAllowedAction(action: RootAction): String {
        if (!isRootAvailable()) {
            return "Ye action normal Android mode mein available nahi hai."
        }
        return when (action) {
            RootAction.REBOOT_NORMAL -> {
                if (reboot()) "Rebooting device now, Sagar Sir." else "Reboot failed."
            }
            RootAction.REBOOT_RECOVERY -> {
                if (rebootRecovery()) "Rebooting to Recovery mode, Sagar Sir." else "Recovery reboot failed."
            }
            RootAction.REBOOT_BOOTLOADER -> {
                if (rebootBootloader()) "Rebooting to Bootloader, Sagar Sir." else "Bootloader reboot failed."
            }
            RootAction.GET_SELINUX_STATUS -> {
                val res = executeSafeCommand("getenforce")
                if (res.success) "SELinux status: ${res.stdout}" else "SELinux check failed."
            }
            RootAction.CLEAR_APP_CACHE -> {
                val res = executeSafeCommand("sync; echo 3 > /proc/sys/vm/drop_caches")
                if (res.success) "System memory cache drop successful, Sagar Sir." else "Cache operation failed."
            }
            RootAction.GET_BATTERY_CHARGE_COUNTER -> {
                val res = executeSafeCommand("dumpsys battery | grep -i level")
                if (res.success) "Battery status: ${res.stdout}" else "Failed reading dumpsys battery."
            }
        }
    }

    companion object {
        val instance by lazy { RootManager() }
    }
}
