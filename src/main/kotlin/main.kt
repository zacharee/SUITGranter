import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.vectorXmlResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.awt.image.BufferedImage
import java.io.InputStream
import java.util.regex.Pattern
import javax.imageio.ImageIO

const val WSS = "android.permission.WRITE_SECURE_SETTINGS"
const val PUS = "android.permission.PACKAGE_USAGE_STATS"
const val DUMP = "android.permission.DUMP"

data class DeviceInfo(
    val deviceName: String,
    val deviceStatus: Status
) {
    enum class Status(val value: String) {
        UNAUTHORIZED("unauthorized"),
        OFFLINE("offline"),
        UNKNOWN("unknown"),
        DEVICE("device"),
        BLANK("");

        companion object {
            fun fromValue(value: String): Status {
                return when (value) {
                    UNAUTHORIZED.value -> UNAUTHORIZED
                    OFFLINE.value -> OFFLINE
                    UNKNOWN.value -> UNKNOWN
                    else -> DEVICE
                }
            }
        }
    }

    companion object {
        val EMPTY = DeviceInfo("No devices found", Status.BLANK)
    }

    fun isBlank() = deviceName == "No devices found" && deviceStatus == Status.BLANK
}

fun main() = Window(
    title = "SystemUI Tuner Permissions Granter",
    size = IntSize(640, 640),
    icon = ImageIO.read(Thread.currentThread().contextClassLoader.getResource("images/icon.png"))
) {
    val currentDevice = remember { mutableStateOf(DeviceInfo.EMPTY) }
    val availableDevices = remember { mutableStateListOf<DeviceInfo>() }
    val menuExpanded = remember { mutableStateOf(false) }

    val permissionStates = remember {
        mutableStateOf(
            LinkedHashMap<String, Boolean>()
        )
    }

    fun refreshInfo() {
        val newList = getDevices()

        if (!newList.containsAll(availableDevices) || !availableDevices.containsAll(newList)) {
            availableDevices.clear()
            availableDevices.addAll(getDevices())
        }

        if (currentDevice.value.isBlank()) {
            if (availableDevices.isNotEmpty()
                && currentDevice.value != availableDevices[0]
            ) {
                currentDevice.value = availableDevices[0]
            }
        } else if (availableDevices.isNotEmpty()) {
            if (!availableDevices.contains(currentDevice.value)) {
                currentDevice.value = availableDevices[0]
            }
        } else {
            currentDevice.value = DeviceInfo.EMPTY
        }

        if (!currentDevice.value.isBlank()) {
            val wss = checkPermissionGranted(currentDevice.value.deviceName, WSS)
            val pus = checkPermissionGranted(currentDevice.value.deviceName, PUS)
            val dump = checkPermissionGranted(currentDevice.value.deviceName, DUMP)

            permissionStates.value = LinkedHashMap<String, Boolean>().apply {
                putAll(listOf(WSS to wss, PUS to pus, DUMP to dump))
            }
        }
    }

    refreshInfo()

    DesktopMaterialTheme(
        colors = darkColors()
    ) {
        Surface(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            Column(Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp, 0.dp, 8.dp, 8.dp),
                    backgroundColor = Color(40, 40, 40)
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text("Current device", modifier = Modifier.align(Alignment.CenterVertically))
                        Spacer(Modifier.size(8.dp))
                        DropdownMenu(
                            toggle = {
                                Box(
                                    Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colors.secondary,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                        .height(48.dp)
                                        .align(Alignment.CenterVertically)
                                        .clickable {
                                            menuExpanded.value = !menuExpanded.value
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.align(Alignment.Center)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = currentDevice.value.deviceName
                                        )
                                        Icon(
                                            vectorXmlResource("images/chevron_down.xml"),
                                            tint = Color.White,
                                            modifier = Modifier.rotate(if (menuExpanded.value) 180f else 0f)
                                        )
                                    }
                                }
                            },
                            expanded = menuExpanded.value,
                            onDismissRequest = { menuExpanded.value = false }
                        ) {
                            @Composable
                            fun DeviceItem(info: DeviceInfo) {
                                DropdownMenuItem(onClick = {
                                    currentDevice.value = info
                                    menuExpanded.value = false
                                }) {
                                    Text(info.deviceName)
                                }
                            }

                            availableDevices.forEach {
                                DeviceItem(it)
                            }
                            if (availableDevices.isEmpty()) {
                                DeviceInfo.EMPTY
                            }
                        }
                        if (currentDevice.value.deviceStatus != DeviceInfo.Status.DEVICE) {
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = currentDevice.value.deviceStatus.toString(),
                                modifier = Modifier.align(Alignment.CenterVertically),
                                color = Color.Red
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                refreshInfo()
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text("Refresh")
                        }
                    }
                }

                if (currentDevice.value.deviceStatus == DeviceInfo.Status.DEVICE) {
                    LazyColumnFor(items = permissionStates.value.keys.toList()) {
                        Row(
                            Modifier.fillMaxWidth()
                                .height(48.dp)
                                .padding(8.dp)
                        ) {
                            Text(text = it, modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                onClick = {
                                    setPermission(
                                        currentDevice.value.deviceName,
                                        it,
                                        if (permissionStates.value[it]!!) "revoke" else "grant"
                                    )
                                    refreshInfo()
                                }
                            ) {
                                Text(if (permissionStates.value[it]!!) "Revoke" else "Grant")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getDevices(): List<DeviceInfo> {
    val list = ArrayList<DeviceInfo>()

    runCommand("adb devices").bufferedReader().forEachLine {
        if (!it.contains("List of", false) && it.isNotBlank()) {
            val split = it.split(Regex("\\s+"))

            list.add(DeviceInfo(split.first(), DeviceInfo.Status.fromValue(split.last())))
        }
    }

    return list
}

fun checkPermissionGranted(device: String, permission: String): Boolean {
    var granted = false
    val pattern = Pattern.compile("\\d{8} \\d{8}")

    runCommand("adb -s $device shell service call ${if (getSdkVersion(device) < 30) "package 20" else "permissionmgr 11"} s16 \"$permission\" s16 \"com.zacharee1.systemuituner\" i32 0")
        .bufferedReader().forEachLine {
            if (it.startsWith("Result")) {
                val matcher = pattern.matcher(it)

                if (matcher.find()) {
                    val group = matcher.group()
                    granted = group.replace(" ", "").toInt() == 0
                }
            }
        }

    return granted
}

fun setPermission(device: String, permission: String, action: String) {
    runCommand("adb -s $device shell pm $action com.zacharee1.systemuituner $permission")
}

fun getSdkVersion(device: String): Int {
    return runCommand("adb -s $device shell getprop ro.build.version.sdk").bufferedReader().use {
        it.readLine().toInt()
    }
}

fun runCommand(command: String): InputStream {
    val process = Runtime.getRuntime().exec(command)

    val error = process.errorStream
    if (error.available() > 0) return error

    error.close()

    return process.inputStream
}