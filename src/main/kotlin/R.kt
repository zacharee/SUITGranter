import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object R {
    object string {
        const val app_name = "SystemUI Tuner Permissions Granter"
        const val current_device = "Current Device"
        const val no_devices_found = "No devices found"
        const val refresh = "Refresh"
        const val revoke = "Revoke"
        const val grant = "Grant"
    }

    object dimen {
        val window_size = 640
        val corner_size_dp = 8.dp
        val padding_dp = 8.dp
        val border_width_dp = 1.dp
        val menu_button_height_dp = 48.dp
        val spacing_dp = 8.dp
        val permission_box_height = 48.dp
    }

    object image {
        const val icon_png = "images/icon.png"
        const val chevron_down = "images/chevron_down.xml"
    }

    object color {
        val color_card_background = Color(40, 40, 40)
        val loading_overlay_color = Color(0, 0, 0, 0xaa)
    }
}