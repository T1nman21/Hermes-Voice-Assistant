//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//

package com.xnu.rocky

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState

/**
 * Framework MediaSession wrapper used while a voice session is alive.
 *
 * The session shows up in the lock-screen media controls and on any paired Bluetooth device
 * as "Rocky voice session — Listening…", with a Stop control. Headset play/pause buttons and
 * the lock-screen Stop button both route through [onStop] so SessionRuntime can end the
 * session cleanly. iOS CarPlay/lock-screen controls are reserved for first-party audio, so
 * this integration has no iOS equivalent.
 */
class VoiceMediaSession(context: Context, private val onStop: () -> Unit) {

    private val session: MediaSession = MediaSession(context, "RockyVoice").apply {
        setCallback(object : MediaSession.Callback() {
            override fun onPause() { onStop() }
            override fun onStop() { onStop() }
        })
        setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_STOP or PlaybackState.ACTION_PAUSE)
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
        setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, context.getString(R.string.voice_service_title))
                .putString(MediaMetadata.METADATA_KEY_ARTIST, context.getString(R.string.voice_service_text))
                .build()
        )
        isActive = true
    }

    val token: MediaSession.Token = session.sessionToken

    fun release() {
        session.isActive = false
        session.release()
    }
}
