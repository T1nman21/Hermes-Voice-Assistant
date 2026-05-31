//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.xnu.rocky.providers.SecureStore
import com.xnu.rocky.runtime.LogManager

class OpenRockyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SecureStore.init(this)
        LogManager.init(this)

        // Initialize embedded Python interpreter
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
            val py = Python.getInstance()
            val ver = py.getModule("openrocky_exec").callAttr("version").toString()
            LogManager.info("Rocky started. $ver", "App")
        } else {
            LogManager.info("Rocky started. Python already running.", "App")
        }
    }
}
