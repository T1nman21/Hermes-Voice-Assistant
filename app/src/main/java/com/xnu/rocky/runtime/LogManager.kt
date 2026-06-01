package com.xnu.rocky.runtime

import android.util.Log

object LogManager {
    fun info(msg: String, tag: String) = Log.i(tag, msg)
    fun warning(msg: String, tag: String) = Log.w(tag, msg)
    fun error(msg: String, tag: String) = Log.e(tag, msg)
}
