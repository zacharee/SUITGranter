import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.transition
import androidx.compose.desktop.DesktopMaterialTheme
import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.vectorXmlResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
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
        val EMPTY = DeviceInfo(R.string.no_devices_found, Status.BLANK)
    }

    fun isBlank() = deviceName == R.string.no_devices_found && deviceStatus == Status.BLANK
}

fun main() = Window(
    title = R.string.app_name,
    size = IntSize(R.dimen.window_size, R.dimen.window_size),
    icon = ImageIO.read(Thread.currentThread().contextClassLoader.getResource(R.image.icon_png))
) {
    val scope = rememberCoroutineScope()
    val currentDevice = remember { mutableStateOf(DeviceInfo.EMPTY) }
    val availableDevices = remember { mutableStateListOf<DeviceInfo>() }
    val menuExpanded = remember { mutableStateOf(false) }
    val loading = remember { mutableStateOf(false) }

    var prevExpanded = false

    val permissionStates = remember {
        mutableStateOf(
            LinkedHashMap<String, Boolean>()
        )
    }

    suspend fun updateDeviceList() {
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
    }

    suspend fun refreshInfo() {
        loading.value = true

        updateDeviceList()

        if (!currentDevice.value.isBlank()) {
            val wss = checkPermissionGranted(currentDevice.value.deviceName, WSS)
            val pus = checkPermissionGranted(currentDevice.value.deviceName, PUS)
            val dump = checkPermissionGranted(currentDevice.value.deviceName, DUMP)

            permissionStates.value = LinkedHashMap<String, Boolean>().apply {
                putAll(listOf(WSS to wss, PUS to pus, DUMP to dump))
            }
        }

        loading.value = false
    }

    LaunchedEffect(currentDevice.value) {
        refreshInfo()
    }

    DesktopMaterialTheme(
        colors = darkColors()
    ) {
        Surface(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            Box {
                Column(Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp, 0.dp, R.dimen.corner_size_dp, R.dimen.corner_size_dp),
                        backgroundColor = R.color.color_card_background
                    ) {
                        Row(
                            Modifier.fillMaxWidth()
                                .padding(R.dimen.padding_dp)
                        ) {
                            Text(R.string.current_device, modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(Modifier.size(R.dimen.padding_dp))

                            val rotation = FloatPropKey()
                            val toggleTransition = transitionDefinition<Boolean> {
                                state(true) {
                                    this[rotation] = 180f
                                }
                                state(false) {
                                    this[rotation] = 0f
                                }
                            }

                            DropdownMenu(
                                toggle = {
                                    Box(
                                        Modifier.border(
                                            width = R.dimen.border_width_dp,
                                            color = MaterialTheme.colors.secondary,
                                            shape = RoundedCornerShape(R.dimen.corner_size_dp)
                                        )
                                            .height(R.dimen.menu_button_height_dp)
                                            .align(Alignment.CenterVertically)
                                            .clickable {
                                                menuExpanded.value = !menuExpanded.value
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.align(Alignment.Center)
                                                .padding(R.dimen.padding_dp)
                                        ) {
                                            Text(
                                                text = currentDevice.value.deviceName
                                            )
                                            val state = transition(toggleTransition,
                                                initState = if (prevExpanded != menuExpanded.value)
                                                    !menuExpanded.value else menuExpanded.value,
                                                toState = menuExpanded.value)
                                            prevExpanded = menuExpanded.value
                                            Icon(
                                                vectorXmlResource(R.image.chevron_down),
                                                tint = Color.White,
                                                modifier = Modifier.rotate(state[rotation])
                                            )
                                        }
                                    }
                                },
                                expanded = menuExpanded.value,
                                onDismissRequest = { menuExpanded.value = false },
                                dropdownModifier = Modifier.background(R.color.color_card_background)
                            ) {
                                @Composable
                                fun DeviceItem(info: DeviceInfo) {
                                    DropdownMenuItem(onClick = {
                                        currentDevice.value = info
                                        menuExpanded.value = false
                                    }) {
                                        Text(
                                            text = info.deviceName,
                                            color = Color.White
                                        )
                                    }
                                }

                                availableDevices.forEach {
                                    DeviceItem(it)
                                }
                                if (availableDevices.isEmpty()) {
                                    DeviceInfo.EMPTY
                                }
                            }
                            if (currentDevice.value.deviceStatus != DeviceInfo.Status.DEVICE
                                && currentDevice.value != DeviceInfo.EMPTY) {
                                Spacer(Modifier.size(R.dimen.spacing_dp))
                                Text(
                                    text = currentDevice.value.deviceStatus.toString(),
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    color = Color.Red
                                )
                            }
                            if (loading.value) {
                                Spacer(Modifier.size(R.dimen.spacing_dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Card(
                                elevation = 0.dp,
                                shape = CircleShape,
                                backgroundColor = Color.Transparent,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    modifier = Modifier.clickable(
                                        onClickLabel = R.string.refresh
                                    ) {
                                        scope.launch { refreshInfo() }
                                    }.align(Alignment.CenterVertically)
                                        .padding(12.dp)
                                )
                            }
                        }
                    }

                    Box {
                        if (currentDevice.value.deviceStatus == DeviceInfo.Status.DEVICE) {
                            LazyColumnFor(items = permissionStates.value.keys.toList()) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .height(R.dimen.permission_box_height)
                                        .padding(R.dimen.padding_dp)
                                ) {
                                    Text(text = it, modifier = Modifier.align(Alignment.CenterVertically))
                                    Spacer(modifier = Modifier.weight(1f))
                                    OutlinedButton(
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                        onClick = {
                                            scope.launch {
                                                setPermission(
                                                    currentDevice.value.deviceName,
                                                    it,
                                                    if (permissionStates.value[it]!!) "revoke" else "grant"
                                                )
                                                refreshInfo()
                                            }
                                        },
                                        border = BorderStroke(1.dp, MaterialTheme.colors.secondary)
                                    ) {
                                        Text(
                                            text = if (permissionStates.value[it]!!) R.string.revoke else R.string.grant,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }

                        if (loading.value) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = R.color.loading_overlay_color,
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

suspend fun getDevices(): List<DeviceInfo> = withContext(Dispatchers.IO) {
    val list = ArrayList<DeviceInfo>()

    runCommand("${makeAdbCommand()} devices").bufferedReader().forEachLine {
        if (!it.contains("List of", false) && it.isNotBlank()) {
            val split = it.split(Regex("\\s+"))

            list.add(DeviceInfo(split.first(), DeviceInfo.Status.fromValue(split.last())))
        }
    }

    list
}

suspend fun checkPermissionGranted(device: String, permission: String): Boolean = withContext(Dispatchers.IO) {
    var granted = false
    val pattern = Pattern.compile("\\d{8} \\d{8}")

    runCommand("${makeAdbCommand()} -s $device shell service call ${if (getSdkVersion(device) < 30) "package 20" else "permissionmgr 11"} s16 \"$permission\" s16 \"com.zacharee1.systemuituner\" i32 0")
        .bufferedReader().forEachLine {
            if (it.startsWith("Result")) {
                val matcher = pattern.matcher(it)

                if (matcher.find()) {
                    val group = matcher.group()
                    granted = group.replace(" ", "").toInt() == 0
                }
            }
        }

    granted
}

suspend fun setPermission(device: String, permission: String, action: String) = withContext(Dispatchers.IO) {
    runCommand("${makeAdbCommand()} -s $device shell pm $action com.zacharee1.systemuituner $permission")
}

suspend fun getSdkVersion(device: String): Int = withContext(Dispatchers.IO) {
    runCommand("${makeAdbCommand()} -s $device shell getprop ro.build.version.sdk").bufferedReader().use {
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

fun extractAdb() {
    val path = when {
        SystemUtils.IS_OS_WINDOWS -> {
            "adb/windows.zip"
        }
        SystemUtils.IS_OS_MAC -> {
            "adb/mac.zip"
        }
        SystemUtils.IS_OS_LINUX -> {
            "adb/linux.zip"
        }
        else -> throw IllegalStateException("Unsupported OS")
    }

    val adbDir = File(Paths.get("").toAbsolutePath().toString(), "adb")
    if (!adbDir.exists()) {
        adbDir.mkdir()
    }

    val targetDir = File(adbDir, File(path).nameWithoutExtension)

    if (!targetDir.exists() || targetDir.listFiles()?.find { it.name.startsWith("adb") } == null) {
        val targetFile = File(adbDir, File(path).name)
        if (!targetFile.exists()) {
            Thread.currentThread().contextClassLoader.getResourceAsStream(path)?.use {
                Files.copy(it, targetFile.toPath())
            }
        }

        targetDir.mkdir()

        ZipInputStream(FileInputStream(targetFile)).use { zipStream ->
            val buffer = ByteArray(1024)
            var entry: ZipEntry? = zipStream.nextEntry

            while (entry != null) {
                val file = File(targetDir, entry.name)

                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()

                    val os = FileOutputStream(file)
                    var len = zipStream.read(buffer)

                    while (len > 0) {
                        os.write(buffer, 0, len)

                        len = zipStream.read(buffer)
                    }

                    os.close()
                }

                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }

            zipStream.closeEntry()
        }
    }
}

fun makeAdbCommand(): String {
    extractAdb()

    return when {
        SystemUtils.IS_OS_WINDOWS -> "adb\\windows\\adb.exe"
        SystemUtils.IS_OS_MAC -> "adb/mac/adb"
        SystemUtils.IS_OS_LINUX -> "adb/linux/adb"
        else -> throw IllegalStateException("Unsupported OS")
    }
}