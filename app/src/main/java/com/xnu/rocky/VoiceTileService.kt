//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//

package com.xnu.rocky

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile — pull down the notification shade, tap Rocky, and a voice session starts.
 *
 * Users must add the tile once via the "Edit tiles" affordance (Android drags an unused tile list).
 * From then on it works from lock screen or any app — an Android-only, one-tap voice-first entry
 * point that the iOS Control Center cannot replicate (Apple reserves those tiles for first-party).
 */
class VoiceTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.tile_voice_label)
            contentDescription = getString(R.string.tile_voice_description)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_START_VOICE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: must collapse via PendingIntent so the system can enforce trampoline rules.
            val pi = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
