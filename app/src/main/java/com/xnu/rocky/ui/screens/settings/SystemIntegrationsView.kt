//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//

package com.xnu.rocky.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.RockyAccessibilityService
import com.xnu.rocky.RockyNotificationListenerService
import com.xnu.rocky.runtime.tools.NotificationInboxStore
import com.xnu.rocky.ui.theme.OpenRockyPalette

/**
 * Shows the status of Android-only system integrations and deep-links the user to the
 * right system screen to grant access.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemIntegrationsView(onBack: () -> Unit) {
    val context = LocalContext.current
    val notificationGranted by NotificationInboxStore.granted.collectAsState()
    // Also check the settings flag directly in case the listener hasn't been bound yet this session.
    var settingsFlagGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    val effectiveGranted = notificationGranted || settingsFlagGranted

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Integrations", color = OpenRockyPalette.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                IntegrationCard(
                    icon = Icons.Default.Notifications,
                    iconTint = OpenRockyPalette.accent,
                    title = "Notification Access",
                    statusLabel = if (effectiveGranted) "Granted" else "Not granted",
                    statusOk = effectiveGranted,
                    description = "Lets the notifications-read tool see your recent notifications so the assistant can answer \"what did I miss?\" or summarize unread messages. Rocky only keeps notifications in memory — never writes them to disk.",
                    actionLabel = if (effectiveGranted) "Open system settings" else "Grant access",
                    onAction = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        // Re-probe when user returns.
                        settingsFlagGranted = isNotificationListenerEnabled(context)
                    }
                )
            }

            item {
                val accessibilityEnabled = RockyAccessibilityService.isActive() ||
                    isAccessibilityServiceEnabled(context)
                IntegrationCard(
                    icon = Icons.Default.Visibility,
                    iconTint = OpenRockyPalette.warning,
                    title = "Screen Read (Accessibility)",
                    statusLabel = if (accessibilityEnabled) "Enabled" else "Disabled",
                    statusOk = accessibilityEnabled,
                    description = "Lets the screen-read tool answer \"what am I looking at?\" by reading the current screen on demand. Rocky never caches or logs screen content — it is pulled only when the tool is called, used for that single reply, then discarded. Opt-in: flip the Rocky switch in Settings → Accessibility → Installed apps.",
                    actionLabel = if (accessibilityEnabled) "Open accessibility settings" else "Enable",
                    onAction = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
            }

            item {
                IntegrationCard(
                    icon = Icons.Default.Assistant,
                    iconTint = OpenRockyPalette.secondary,
                    title = "Default Digital Assistant",
                    statusLabel = "System setting",
                    statusOk = null,
                    description = "Pick Rocky as your default digital assistant to make long-press home / the power button / the dedicated assist button open a Rocky voice session.",
                    actionLabel = "Open assistant settings",
                    onAction = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
            }

            item {
                IntegrationCard(
                    icon = Icons.Default.Dashboard,
                    iconTint = OpenRockyPalette.warning,
                    title = "Quick Settings Tile",
                    statusLabel = "Pull down the notification shade, edit tiles, and drag Rocky onto the active tray.",
                    statusOk = null,
                    description = "Once added, tapping the Rocky tile from any app or the lock screen starts a voice session.",
                    actionLabel = null,
                    onAction = {}
                )
            }

            item {
                IntegrationCard(
                    icon = Icons.Default.Widgets,
                    iconTint = OpenRockyPalette.success,
                    title = "Home Screen Widget",
                    statusLabel = "Long-press your home screen, choose Widgets, find Rocky, drag the tile onto the screen.",
                    statusOk = null,
                    description = "A 2×2 tap-to-talk tile that starts a voice session without opening the app first.",
                    actionLabel = null,
                    onAction = {}
                )
            }

            item {
                IntegrationCard(
                    icon = Icons.Default.TouchApp,
                    iconTint = OpenRockyPalette.muted,
                    title = "Launcher Shortcuts",
                    statusLabel = "Long-press the Rocky icon on your home screen / app drawer.",
                    statusOk = null,
                    description = "Voice, New Chat, and Continue shortcuts appear for fast access.",
                    actionLabel = null,
                    onAction = {}
                )
            }

            item {
                IntegrationCard(
                    icon = Icons.Default.SettingsRemote,
                    iconTint = OpenRockyPalette.accent,
                    title = "Automation Intents",
                    statusLabel = "Tasker / Automate / NFC tags / adb",
                    statusOk = null,
                    description = "Send an intent with any of these actions:\n• com.xnu.rocky.action.START_VOICE — start voice\n• com.xnu.rocky.action.NEW_CHAT — new conversation\n• com.xnu.rocky.action.SEND_PROMPT (extra: \"prompt\") — send a text prompt and trigger a reply",
                    actionLabel = null,
                    onAction = {}
                )
            }
        }
    }
}

@Composable
private fun IntegrationCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    statusLabel: String,
    statusOk: Boolean?,
    description: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.text, modifier = Modifier.weight(1f))
                statusOk?.let {
                    val color = if (it) OpenRockyPalette.success else OpenRockyPalette.warning
                    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
                        Text(
                            if (it) "Granted" else "Not granted",
                            fontSize = 11.sp,
                            color = color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            if (statusOk == null && statusLabel.isNotBlank()) {
                Text(statusLabel, fontSize = 12.sp, color = OpenRockyPalette.muted)
            }
            Text(description, fontSize = 13.sp, color = OpenRockyPalette.muted)
            if (actionLabel != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = OpenRockyPalette.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** Returns true when the user has enabled Rocky's NotificationListenerService in system settings. */
private fun isNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    val target = ComponentName(context, RockyNotificationListenerService::class.java).flattenToString()
    return flat.split(":").any { it == target }
}

/** Returns true when the user has enabled Rocky's AccessibilityService in system settings. */
private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val target = ComponentName(context, RockyAccessibilityService::class.java).flattenToString()
    return flat.split(":").any { it == target }
}
