//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.content.Context
import com.xnu.rocky.providers.ProviderConfiguration
import com.xnu.rocky.providers.ToolDefinition
import com.xnu.rocky.providers.ToolFunctionDef
import com.xnu.rocky.runtime.LogManager
import com.xnu.rocky.runtime.MemoryService
import com.xnu.rocky.runtime.SubagentEvent
import com.xnu.rocky.runtime.SubagentRuntime
import com.xnu.rocky.runtime.SubagentTask
import kotlinx.serialization.json.*

class Toolbox(
    private val context: Context,
    private val memoryService: MemoryService
) {
    var subagentChatConfiguration: ProviderConfiguration? = null
    /**
     * Structured progress events from delegate-task. The session runtime renders
     * each event as a timeline entry so the user can follow the back-end agent's
     * per-tool progress while the realtime voice stalls on the model's "稍等".
     */
    var subagentEventHandler: ((SubagentEvent) -> Unit)? = null

    private val weatherService = WeatherService()
    private val locationService = LocationService(context)
    private val todoService = TodoService(context)
    private val notificationService = NotificationService(context)
    private val calendarService = CalendarService(context)
    private val contactsService = ContactsService(context)
    private val cryptoService = CryptoService()
    private val shellService = ShellService(context)
    private val pythonService = PythonService()
    private val nearbySearchService = NearbySearchService(context)
    private val browserService = BrowserService(context)
    private val reminderService = ReminderService(context)
    private val mediaService = MediaService(context)
    private val ffmpegService = FFmpegService(context)
    private val healthService = HealthService(context)
    val mountStore = MountStore(context)
    val timerService = TimerService(context)
    val mediaPlayerService = MediaPlayerService(context)
    private val builtInToolStore = BuiltInToolStore(context)
    /**
     * User-configured Model Context Protocol servers. Their tools are surfaced
     * to the chat sub-agent (and never to the realtime voice tier) under the
     * `mcp-{label}-{tool}` namespace. Mirrors iOS `OpenRockyMCPStore.shared`.
     */
    val mcpStore = MCPStore(context)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Realtime tool surface — intentionally small.
     *
     * The OpenAI Realtime model degrades when the tool list grows past a dozen entries; it also
     * tends to over-summarize when many overlapping options compete. So we expose only fast,
     * single-shot local actions plus `delegate-task`, and route everything else (web search,
     * shell/python, browser, multi-step composition, custom skills) to the chat-provider
     * sub-agent through delegate-task. Mirrors iOS `OpenRockyToolbox.realtimeToolWhitelist`
     * with `apple-` → `android-` prefix swaps.
     */
    private val realtimeToolWhitelist = setOf(
        "delegate-task",
        "android-location",
        "timer",
        "android-alarm",
        "android-calendar-list",
        "android-calendar-create",
        "android-reminder-list",
        "android-reminder-create",
        "notification-schedule",
        "open-url",
        "memory_get",
        "memory_write",
        "camera-capture",
        "photo-pick",
        "file-pick"
    )

    fun chatToolDefinitions(): List<ToolDefinition> {
        // MCP servers expose their tools through the same chat surface so any
        // configured chat provider (not just OpenAI) can call them. They live
        // behind delegate-task — voice never sees them directly.
        return allToolDefinitions().filter { builtInToolStore.isEnabled(it.function.name) } +
            mcpToolDefinitions()
    }

    fun realtimeToolDefinitions(): List<ToolDefinition> {
        // Intentionally excludes MCP tools: the realtime model degrades past
        // ~12 tools and MCP catalogs can be large. Heavier MCP work is routed
        // behind delegate-task on the chat sub-agent, same pattern as iOS.
        return allToolDefinitions()
            .filter { builtInToolStore.isEnabled(it.function.name) }
            .filter { realtimeToolWhitelist.contains(it.function.name) }
    }

    fun subagentToolDefinitions(allowedTools: Set<String>? = null): List<ToolDefinition> {
        val enabled = allToolDefinitions().filter { builtInToolStore.isEnabled(it.function.name) }
        val nonRecursive = enabled.filter { it.function.name != "delegate-task" } + mcpToolDefinitions()
        if (allowedTools.isNullOrEmpty()) return nonRecursive
        return nonRecursive.filter { allowedTools.contains(it.function.name) }
    }

    /**
     * Project the user's enabled MCP servers' cached tool catalogs onto the
     * chat-tool surface, namespaced as `mcp-{label}-{tool}` so two servers
     * can advertise overlapping tool names without colliding. Mirrors iOS
     * `mcpToolDefinitions()`.
     *
     * We emit a stub `parameters` (free-form object) here — the MCP server's
     * own schema may use JSON Schema features our local model doesn't cover.
     * The chat model still gets the tool name + description, which is enough
     * to call most MCP tools correctly.
     */
    fun mcpToolDefinitions(): List<ToolDefinition> {
        val defs = mutableListOf<ToolDefinition>()
        for (server in mcpStore.servers.value) {
            if (!server.isEnabled) continue
            val allowSet = server.allowedToolNames.toSet()
            for (tool in server.cachedTools) {
                if (allowSet.isNotEmpty() && tool.name !in allowSet) continue
                val toolName = MCPServer.sanitizedToolName(server.label, tool.name)
                val description = "[MCP ${server.label}] ${tool.description}"
                defs.add(ToolDefinition(function = ToolFunctionDef(
                    name = toolName,
                    description = description,
                    parameters = buildJsonObject {
                        put("type", JsonPrimitive("object"))
                        putJsonObject("properties") {}
                    }
                )))
            }
        }
        return defs
    }

    fun toolDescriptions(): String {
        return chatToolDefinitions().joinToString("\n") { "- ${it.function.name}: ${it.function.description}" }
    }

    suspend fun execute(name: String, argumentsJson: String): String {
        LogManager.info("Executing tool: $name", "Toolbox")
        return try {
            val args = try { json.parseToJsonElement(argumentsJson).jsonObject } catch (_: Exception) { JsonObject(emptyMap()) }
            when (name) {
                "weather" -> weatherService.getWeather(
                    latitude = args["latitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    longitude = args["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
                "android-location" -> locationService.getCurrentLocation()
                "android-geocode" -> locationService.geocode(
                    args["address"]?.jsonPrimitive?.contentOrNull ?: ""
                )
                "memory_get" -> {
                    val key = args["key"]?.jsonPrimitive?.contentOrNull ?: ""
                    memoryService.get(key) ?: "No memory found for key: $key"
                }
                "memory_write" -> {
                    val key = args["key"]?.jsonPrimitive?.contentOrNull ?: ""
                    val value = args["value"]?.jsonPrimitive?.contentOrNull ?: ""
                    memoryService.set(key, value)
                    "Memory saved: $key"
                }
                "todo" -> {
                    val action = args["action"]?.jsonPrimitive?.contentOrNull ?: "list"
                    when (action) {
                        "add" -> todoService.add(args["title"]?.jsonPrimitive?.contentOrNull ?: "")
                        "list" -> todoService.list()
                        "complete" -> todoService.complete(args["id"]?.jsonPrimitive?.contentOrNull ?: "")
                        "delete" -> todoService.delete(args["id"]?.jsonPrimitive?.contentOrNull ?: "")
                        else -> "Unknown todo action: $action"
                    }
                }
                "web-search" -> {
                    val query = args["query"]?.jsonPrimitive?.contentOrNull ?: ""
                    WebSearchService.search(query)
                }
                "notification-schedule" -> notificationService.schedule(
                    title = args["title"]?.jsonPrimitive?.contentOrNull ?: "Rocky",
                    body = args["body"]?.jsonPrimitive?.contentOrNull ?: "",
                    delaySeconds = args["delay_seconds"]?.jsonPrimitive?.intOrNull ?: 0,
                    triggerDate = args["trigger_date"]?.jsonPrimitive?.contentOrNull
                )
                "notifications-read" -> {
                    if (!NotificationInboxStore.granted.value) {
                        "Notification listener access not granted. Ask the user to enable Rocky in Settings → Notifications → Device & app notifications → Notification access."
                    } else {
                        val limit = args["limit"]?.jsonPrimitive?.intOrNull?.coerceIn(1, 64) ?: 20
                        val packageFilter = args["package"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                        val entries = NotificationInboxStore.snapshot(limit = limit, packageFilter = packageFilter)
                        if (entries.isEmpty()) {
                            if (packageFilter != null) "No notifications from $packageFilter."
                            else "No notifications captured yet."
                        } else {
                            val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            entries.joinToString("\n") { e ->
                                val app = e.appLabel ?: e.packageName
                                val ts = fmt.format(java.util.Date(e.postedAtMs))
                                val body = if (e.text.isBlank()) e.title else "${e.title}: ${e.text}"
                                "[$ts] $app — $body"
                            }
                        }
                    }
                }
                "screen-read" -> {
                    if (!com.xnu.rocky.RockyAccessibilityService.isActive()) {
                        "Accessibility service not enabled. Ask the user to turn on Rocky in Settings → Accessibility → Installed apps. This unlocks reading the active screen on demand."
                    } else {
                        val maxChars = args["max_chars"]?.jsonPrimitive?.intOrNull?.coerceIn(200, 16000) ?: 4000
                        val text = com.xnu.rocky.RockyAccessibilityService.captureActiveWindow(maxChars)
                        when {
                            text == null -> "Accessibility service is not currently bound. Re-check the toggle in Settings → Accessibility."
                            text.isBlank() -> "No readable text on the current screen."
                            else -> text
                        }
                    }
                }
                "android-calendar-list" -> calendarService.listEvents(
                    daysAhead = args["days_ahead"]?.jsonPrimitive?.intOrNull ?: 7
                )
                "android-calendar-create" -> calendarService.createEvent(
                    title = args["title"]?.jsonPrimitive?.contentOrNull ?: "",
                    startDate = args["start_date"]?.jsonPrimitive?.contentOrNull ?: "",
                    endDate = args["end_date"]?.jsonPrimitive?.contentOrNull,
                    location = args["location"]?.jsonPrimitive?.contentOrNull
                )
                "android-contacts-search" -> contactsService.search(
                    query = args["query"]?.jsonPrimitive?.contentOrNull ?: ""
                )
                "android-alarm" -> {
                    val time = args["scheduled_at"]?.jsonPrimitive?.contentOrNull
                        ?: args["time"]?.jsonPrimitive?.contentOrNull
                        ?: ""
                    val title = args["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    AlarmService.setAlarm(context, time, title)
                }
                "crypto" -> cryptoService.execute(
                    operation = args["operation"]?.jsonPrimitive?.contentOrNull ?: "",
                    data = args["data"]?.jsonPrimitive?.contentOrNull
                        ?: args["input"]?.jsonPrimitive?.contentOrNull ?: "",
                    key = args["key"]?.jsonPrimitive?.contentOrNull,
                    iv = args["iv"]?.jsonPrimitive?.contentOrNull
                )
                "open-url" -> {
                    val url = args["url"]?.jsonPrimitive?.contentOrNull ?: ""
                    URLService.openUrl(context, url)
                }
                "file-read" -> {
                    val path = args["path"]?.jsonPrimitive?.contentOrNull ?: ""
                    FileService.readFile(context, path)
                }
                "file-write" -> {
                    val path = args["path"]?.jsonPrimitive?.contentOrNull ?: ""
                    val content = args["content"]?.jsonPrimitive?.contentOrNull ?: ""
                    FileService.writeFile(context, path, content)
                }
                "shell-execute" -> {
                    val command = args["command"]?.jsonPrimitive?.contentOrNull ?: ""
                    val result = shellService.execute(command)
                    buildString {
                        appendLine("$ $command")
                        appendLine(result.output)
                        if (result.exitCode != 0) appendLine("[exit code: ${result.exitCode}]")
                    }
                }
                "python-execute" -> {
                    val code = args["code"]?.jsonPrimitive?.contentOrNull ?: ""
                    val result = pythonService.execute(code)
                    buildString {
                        if (result.success) {
                            append(result.output)
                        } else {
                            appendLine("Error: ${result.error ?: "Unknown error"}")
                            if (result.output.isNotBlank()) appendLine(result.output)
                        }
                    }
                }
                "ffmpeg-execute" -> {
                    val command = args["command"]?.jsonPrimitive?.contentOrNull ?: ""
                    ffmpegService.execute(command)
                }
                "android-health-summary" -> {
                    val date = args["date"]?.jsonPrimitive?.contentOrNull
                    healthService.getSummary(date)
                }
                "android-health-metric" -> {
                    val metric = args["metric"]?.jsonPrimitive?.contentOrNull ?: ""
                    val startDate = args["start_date"]?.jsonPrimitive?.contentOrNull
                    val endDate = args["end_date"]?.jsonPrimitive?.contentOrNull
                    val days = args["days"]?.jsonPrimitive?.intOrNull
                    healthService.getMetric(metric, startDate, endDate, days)
                }
                "nearby-search" -> {
                    val query = args["query"]?.jsonPrimitive?.contentOrNull ?: ""
                    var lat = args["latitude"]?.jsonPrimitive?.doubleOrNull
                    var lon = args["longitude"]?.jsonPrimitive?.doubleOrNull
                    if (lat == null || lon == null) {
                        locationService.lastKnownLatLng()?.let { (fixLat, fixLon) ->
                            lat = fixLat
                            lon = fixLon
                        }
                    }
                    nearbySearchService.search(query, lat, lon)
                }
                "browser-open" -> {
                    val url = args["url"]?.jsonPrimitive?.contentOrNull ?: ""
                    browserService.openUrl(url)
                }
                "browser-read" -> {
                    val url = args["url"]?.jsonPrimitive?.contentOrNull ?: ""
                    browserService.readContent(url)
                }
                "browser-cookies" -> {
                    val domain = args["domain"]?.jsonPrimitive?.contentOrNull ?: ""
                    browserService.getCookies(domain)
                }
                "android-reminder-list" -> {
                    val daysAhead = args["days_ahead"]?.jsonPrimitive?.intOrNull ?: 7
                    reminderService.listReminders(daysAhead)
                }
                "android-reminder-create" -> {
                    val title = args["title"]?.jsonPrimitive?.contentOrNull ?: ""
                    val date = args["date"]?.jsonPrimitive?.contentOrNull ?: ""
                    val notes = args["notes"]?.jsonPrimitive?.contentOrNull
                    reminderService.createReminder(title, date, notes)
                }
                "camera-capture" -> mediaService.captureCamera()
                "photo-pick" -> mediaService.pickPhoto()
                "file-pick" -> mediaService.pickFile()
                "email-send" -> {
                    // `to` is optional — EmailService falls back to the configured default recipient.
                    val to = args["to"]?.jsonPrimitive?.contentOrNull ?: ""
                    val subject = args["subject"]?.jsonPrimitive?.contentOrNull ?: ""
                    val body = args["body"]?.jsonPrimitive?.contentOrNull ?: ""
                    EmailService.sendEmail(context, to, subject, body)
                }
                "oauth-authenticate" -> {
                    val authUrl = args["url"]?.jsonPrimitive?.contentOrNull ?: ""
                    OAuthService.authenticate(context, authUrl)
                }
                "external-list" -> {
                    val container = args["container"]?.jsonPrimitive?.contentOrNull ?: ""
                    val path = args["path"]?.jsonPrimitive?.contentOrNull ?: ""
                    if (path.contains("..") || path.startsWith("/")) return "Invalid path: must be relative, no '..' allowed"
                    val mount = mountStore.mount(container) ?: return "Mount '$container' not found. Available: ${mountStore.mounts.value.joinToString { it.name }}"
                    mountStore.listFiles(mount, path)
                }
                "external-read" -> {
                    val container = args["container"]?.jsonPrimitive?.contentOrNull ?: ""
                    val path = args["path"]?.jsonPrimitive?.contentOrNull ?: ""
                    if (path.contains("..") || path.startsWith("/")) return "Invalid path: must be relative, no '..' allowed"
                    val mount = mountStore.mount(container) ?: return "Mount '$container' not found. Available: ${mountStore.mounts.value.joinToString { it.name }}"
                    mountStore.readFile(mount, path)
                }
                "external-write" -> {
                    val container = args["container"]?.jsonPrimitive?.contentOrNull ?: ""
                    val path = args["path"]?.jsonPrimitive?.contentOrNull ?: ""
                    val content = args["content"]?.jsonPrimitive?.contentOrNull ?: ""
                    if (path.contains("..") || path.startsWith("/")) return "Invalid path: must be relative, no '..' allowed"
                    val mount = mountStore.mount(container) ?: return "Mount '$container' not found. Available: ${mountStore.mounts.value.joinToString { it.name }}"
                    mountStore.writeFile(mount, path, content)
                }
                "timer" -> {
                    val action = args["action"]?.jsonPrimitive?.contentOrNull ?: "list"
                    when (action) {
                        "create", "start" -> {
                            val label = args["label"]?.jsonPrimitive?.contentOrNull.orEmpty()
                            val seconds = args["duration_seconds"]?.jsonPrimitive?.longOrNull
                                ?: args["seconds"]?.jsonPrimitive?.longOrNull
                                ?: args["duration"]?.jsonPrimitive?.longOrNull ?: 0L
                            if (seconds <= 0) "Timer duration (seconds) must be > 0"
                            else {
                                val t = timerService.schedule(label, seconds)
                                "Timer '${t.label}' set for $seconds seconds. id=${t.id}"
                            }
                        }
                        "cancel" -> {
                            val id = args["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                            if (id.isBlank()) "Missing id"
                            else if (timerService.cancel(id)) "Cancelled timer $id" else "Timer $id not found"
                        }
                        "cancel_all", "clear" -> "Cancelled ${timerService.cancelAll()} timers"
                        "list" -> {
                            val list = timerService.list()
                            if (list.isEmpty()) "No active timers."
                            else list.joinToString("\n") {
                                val remaining = ((it.fireAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                                "- ${it.label} (${remaining}s left, id=${it.id})"
                            }
                        }
                        else -> "Unknown timer action: $action"
                    }
                }
                "media" -> {
                    val action = args["action"]?.jsonPrimitive?.contentOrNull ?: "status"
                    when (action) {
                        "load" -> {
                            val path = args["path"]?.jsonPrimitive?.contentOrNull.orEmpty()
                            val recursive = args["recursive"]?.jsonPrimitive?.booleanOrNull ?: true
                            val n = mediaPlayerService.load(path, recursive)
                            "Loaded $n media items from $path"
                        }
                        "play" -> {
                            val idx = args["index"]?.jsonPrimitive?.intOrNull
                            val item = mediaPlayerService.play(idx)
                            if (item == null) "Nothing to play (playlist empty)"
                            else "Playing ${item.filename}"
                        }
                        "pause" -> { mediaPlayerService.pause(); "Paused" }
                        "resume" -> { mediaPlayerService.resume(); "Resumed" }
                        "stop" -> { mediaPlayerService.stop(); "Stopped" }
                        "next" -> mediaPlayerService.next()?.let { "Playing ${it.filename}" } ?: "No next item"
                        "previous", "prev" -> mediaPlayerService.previous()?.let { "Playing ${it.filename}" } ?: "No previous item"
                        "mode" -> {
                            val raw = args["mode"]?.jsonPrimitive?.contentOrNull ?: "sequential"
                            val parsed = when (raw.lowercase()) {
                                "loop", "repeat_all" -> MediaPlaybackMode.LOOP
                                "random", "shuffle" -> MediaPlaybackMode.RANDOM
                                "repeat_one", "single" -> MediaPlaybackMode.REPEAT_ONE
                                else -> MediaPlaybackMode.SEQUENTIAL
                            }
                            mediaPlayerService.setMode(parsed)
                            "Playback mode: ${parsed.name.lowercase()}"
                        }
                        "status" -> mediaPlayerService.currentStatus()
                        else -> "Unknown media action: $action"
                    }
                }
                "delegate-task" -> executeDelegateTask(args)
                "app-exit" -> {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    "Exiting app"
                }
                else -> {
                    // Check for custom skill tools (skill-*)
                    if (name.startsWith("skill-")) {
                        val input = args["input"]?.jsonPrimitive?.contentOrNull ?: ""
                        "Skill '$name' activated with input: $input"
                    } else if (name.startsWith("mcp-")) {
                        // MCP tools come in as `mcp-{server}-{tool}`. Resolve
                        // and proxy the call out to the configured server.
                        // Errors from the server (HTTP, JSON-RPC, or
                        // tool-reported isError) bubble up via the catch
                        // below so the caller's failure handling records the
                        // result accurately.
                        executeMCPTool(name, argumentsJson)
                    } else {
                        "Tool '$name' is not available on Android"
                    }
                }
            }
        } catch (e: Exception) {
            LogManager.error("Tool execution failed: $name - ${e.message}", "Toolbox")
            "Error executing $name: ${e.message}"
        }
    }

    /**
     * Forward a namespaced MCP tool call to the matching configured server.
     * Errors (HTTP, JSON-RPC, or tool-reported isError) are propagated as
     * exceptions so the toolbox's outer catch records the failure and the
     * timeline shows the ✗ marker. Mirrors iOS `executeMCPTool`.
     */
    private suspend fun executeMCPTool(name: String, argumentsJson: String): String {
        val resolved = mcpStore.resolveTool(name)
            ?: throw IllegalArgumentException("Unknown MCP tool: $name")
        val (server, realToolName) = resolved
        if (!server.isEnabled) {
            throw IllegalStateException("MCP server '${server.label}' is disabled")
        }
        val client = MCPClient(server)
        return client.callTool(realToolName, argumentsJson)
    }

    private suspend fun executeDelegateTask(args: JsonObject): String {
        val config = subagentChatConfiguration ?: return """{"status":"error","message":"Missing subagent chat configuration"}"""
        val task = args["task"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        if (task.isBlank()) return """{"status":"error","message":"Missing required field: task"}"""
        val contextText = args["context"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val subtasks = args["subtasks"]?.jsonArray?.mapNotNull { item ->
            val obj = item.jsonObject
            val description = obj["description"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            if (description.isBlank()) return@mapNotNull null
            val tools = obj["tools"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet()
            SubagentTask(description = description, allowedTools = tools)
        }.orEmpty()
        // Optional per-subagent overrides — clamped inside SubagentRuntime so
        // the realtime loop can't be tied up with absurd budgets.
        val timeoutMillis = args["max_seconds"]?.jsonPrimitive?.doubleOrNull
            ?.let { (it * 1000.0).toLong() }
            ?: SubagentRuntime.DEFAULT_TIMEOUT_MS
        val maxTurns = args["max_turns"]?.jsonPrimitive?.intOrNull ?: SubagentRuntime.DEFAULT_MAX_TURNS

        val runtime = SubagentRuntime(
            toolbox = this,
            configuration = config.normalized(),
            timeoutMillis = timeoutMillis,
            maxTurns = maxTurns,
            onEvent = subagentEventHandler
        )
        val result = runtime.execute(task, subtasks, contextText)

        return buildJsonObject {
            put("status", JsonPrimitive("completed"))
            put("taskDescription", JsonPrimitive(result.taskDescription))
            put("subtaskCount", JsonPrimitive(result.results.size))
            put("totalElapsedSeconds", JsonPrimitive(result.totalElapsedSeconds))
            putJsonArray("results") {
                result.results.forEach { sub ->
                    addJsonObject {
                        put("summary", JsonPrimitive(sub.summary))
                        put("details", JsonPrimitive(sub.details))
                        put("succeeded", JsonPrimitive(sub.succeeded))
                        put("elapsedSeconds", JsonPrimitive(sub.elapsedSeconds))
                        putJsonArray("toolsUsed") {
                            sub.toolsUsed.forEach { add(JsonPrimitive(it)) }
                        }
                    }
                }
            }
        }.toString()
    }

    private fun allToolDefinitions(): List<ToolDefinition> = listOf(
        ToolDefinition(function = ToolFunctionDef(
            name = "weather",
            description = "Get current weather and forecast for a location",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("latitude") { put("type", JsonPrimitive("number")); put("description", JsonPrimitive("Latitude")) }
                    putJsonObject("longitude") { put("type", JsonPrimitive("number")); put("description", JsonPrimitive("Longitude")) }
                }
                putJsonArray("required") { add(JsonPrimitive("latitude")); add(JsonPrimitive("longitude")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "android-location",
            description = "Get the device's current GPS location with address",
            parameters = buildJsonObject { put("type", JsonPrimitive("object")); putJsonObject("properties") {} }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "android-geocode",
            description = "Convert an address or place name to coordinates",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("address") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Address or place name")) }
                }
                putJsonArray("required") { add(JsonPrimitive("address")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "memory_get",
            description = "Retrieve a value from persistent memory by key",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("key") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Memory key")) }
                }
                putJsonArray("required") { add(JsonPrimitive("key")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "memory_write",
            description = "Store a key-value pair in persistent memory",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("key") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Memory key")) }
                    putJsonObject("value") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Value to store")) }
                }
                putJsonArray("required") { add(JsonPrimitive("key")); add(JsonPrimitive("value")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "todo",
            description = "Manage a persistent todo list (add, list, complete, delete)",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("action") { put("type", JsonPrimitive("string")); put("enum", buildJsonArray { add(JsonPrimitive("add")); add(JsonPrimitive("list")); add(JsonPrimitive("complete")); add(JsonPrimitive("delete")) }) }
                    putJsonObject("title") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Title for add action")) }
                    putJsonObject("id") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("ID for complete/delete")) }
                }
                putJsonArray("required") { add(JsonPrimitive("action")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "web-search",
            description = "Search the web using DuckDuckGo",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("query") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Search query")) }
                }
                putJsonArray("required") { add(JsonPrimitive("query")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "notification-schedule",
            description = "Schedule a local notification. Can trigger at a specific time or after a delay.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("title") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Notification title.")) }
                    putJsonObject("body") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Optional notification body text.")) }
                    putJsonObject("trigger_date") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Optional ISO-8601 datetime to trigger the notification.")) }
                    putJsonObject("delay_seconds") { put("type", JsonPrimitive("number")); put("description", JsonPrimitive("Optional delay in seconds from now.")) }
                }
                putJsonArray("required") { add(JsonPrimitive("title")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "notifications-read",
            description = "Read the user's recent system notifications (most recent first). Requires the user to have enabled Notification access for Rocky in Android Settings. Use for 'what did I miss?' or per-app summaries.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("limit") { put("type", JsonPrimitive("number")); put("description", JsonPrimitive("Max entries to return (default 20, cap 64).")) }
                    putJsonObject("package") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Optional package name to filter (e.g. com.whatsapp, com.tencent.mm).")) }
                }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "screen-read",
            description = "Read the visible text on the user's current screen (on-demand only). Requires the user to have enabled Rocky as an Accessibility service. Use for 'what am I looking at?' or to help with a form / page the user is viewing.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("max_chars") { put("type", JsonPrimitive("number")); put("description", JsonPrimitive("Max characters to return (default 4000, cap 16000).")) }
                }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "android-calendar-list",
            description = "List upcoming calendar events",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("days_ahead") { put("type", JsonPrimitive("integer")); put("description", JsonPrimitive("Number of days to look ahead (default 7)")) }
                }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "android-calendar-create",
            description = "Create a new calendar event",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("title") { put("type", JsonPrimitive("string")) }
                    putJsonObject("start_date") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("ISO 8601 date")) }
                    putJsonObject("end_date") { put("type", JsonPrimitive("string")) }
                    putJsonObject("location") { put("type", JsonPrimitive("string")) }
                }
                putJsonArray("required") { add(JsonPrimitive("title")); add(JsonPrimitive("start_date")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "android-contacts-search",
            description = "Search contacts by name, phone, or email",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("query") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Search term")) }
                }
                putJsonArray("required") { add(JsonPrimitive("query")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "android-alarm",
            description = "Create a real device alarm at one exact time. Use this only after the user gave a precise time.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("title") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Short label for the alarm.")) }
                    putJsonObject("scheduled_at") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Exact ISO-8601 date-time in the user's local timezone (e.g. 2026-04-23T07:30). HH:mm is also accepted for convenience.")) }
                }
                putJsonArray("required") { add(JsonPrimitive("scheduled_at")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "crypto",
            description = "Perform cryptographic operations: HMAC-SHA256, SHA256 hash, MD5 hash, AES-128-CBC encrypt/decrypt, base64 encode/decode. Use this when a skill or API requires signing requests, generating tokens, or encrypting data.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("operation") {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Operation: hmac_sha256, sha256, md5, aes_encrypt, aes_decrypt, base64_encode, base64_decode."))
                        put("enum", buildJsonArray {
                            add(JsonPrimitive("sha256")); add(JsonPrimitive("md5"))
                            add(JsonPrimitive("hmac_sha256")); add(JsonPrimitive("hmac-sha256"))
                            add(JsonPrimitive("aes_encrypt")); add(JsonPrimitive("aes_decrypt"))
                            add(JsonPrimitive("base64_encode")); add(JsonPrimitive("base64-encode"))
                            add(JsonPrimitive("base64_decode")); add(JsonPrimitive("base64-decode"))
                        })
                    }
                    putJsonObject("data") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("The input data (text, or hex-encoded for decrypt).")) }
                    putJsonObject("key") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Key for HMAC or AES operations (hex-encoded, or raw UTF-8 as fallback).")) }
                    putJsonObject("iv") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Initialization vector for AES-CBC (hex-encoded, 16 bytes).")) }
                }
                putJsonArray("required") { add(JsonPrimitive("operation")); add(JsonPrimitive("data")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "open-url",
            description = "Open a URL in the browser or handle a deep link",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("url") { put("type", JsonPrimitive("string")) }
                }
                putJsonArray("required") { add(JsonPrimitive("url")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "file-read",
            description = "Read a file from the workspace",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("path") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Relative path in workspace")) }
                }
                putJsonArray("required") { add(JsonPrimitive("path")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "file-write",
            description = "Write content to a file in the workspace",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("path") { put("type", JsonPrimitive("string")) }
                    putJsonObject("content") { put("type", JsonPrimitive("string")) }
                }
                putJsonArray("required") { add(JsonPrimitive("path")); add(JsonPrimitive("content")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "shell-execute",
            description = "Execute a shell command in the Android sandbox. Supports Unix commands (ls, cat, echo, pwd, cp, mv, mkdir, rm, grep, wc, sort, head, tail, find, curl, ping, tar, sed, awk, diff) and more. Use for file operations, text processing, or system diagnostics.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("command") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("The shell command to execute (e.g. 'ls -la', 'cat file.txt', 'curl -I https://example.com')")) }
                }
                putJsonArray("required") { add(JsonPrimitive("command")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "python-execute",
            description = "Execute Python 3 code on the device. Use for calculations, data processing, text manipulation, algorithms, or any task that benefits from code. Use print() to return results.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("code") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Python source code to execute. Use print() to produce output.")) }
                }
                putJsonArray("required") { add(JsonPrimitive("code")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "android-health-summary",
            description = "Get a daily health summary including steps, active energy, heart rate, distance, and sleep for a given date. Requires Health Connect app.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("date") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Date in YYYY-MM-DD format (default: today)")) }
                }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "android-health-metric",
            description = "Query a specific health metric from Health Connect for a date range. Supported metrics: steps, heart_rate, active_energy, distance, sleep.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("metric") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("The health metric: steps, heart_rate, active_energy, distance, or sleep")) }
                    putJsonObject("start_date") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Start date in YYYY-MM-DD format")) }
                    putJsonObject("end_date") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("End date in YYYY-MM-DD format")) }
                    putJsonObject("days") { put("type", JsonPrimitive("integer")); put("description", JsonPrimitive("Number of days to look back (default 7, used if start_date not set)")) }
                }
                putJsonArray("required") { add(JsonPrimitive("metric")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "ffmpeg-execute",
            description = "Run an FFmpeg command for audio/video processing. Supports format conversion, trimming, merging, extracting audio, adding effects, and more. Files should use paths relative to the workspace directory.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("command") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("FFmpeg command arguments (e.g. '-i input.mp4 -vn output.mp3')")) }
                }
                putJsonArray("required") { add(JsonPrimitive("command")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "nearby-search",
            description = "Search for nearby places, businesses, or points of interest. Returns name, address, coordinates.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("query") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("What to search for (e.g. 'coffee shop', 'gas station', 'pharmacy').")) }
                    putJsonObject("latitude") { put("type", JsonPrimitive("number")); put("description", JsonPrimitive("Optional center latitude. Uses current location if omitted.")) }
                    putJsonObject("longitude") { put("type", JsonPrimitive("number")); put("description", JsonPrimitive("Optional center longitude.")) }
                }
                putJsonArray("required") { add(JsonPrimitive("query")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "browser-open",
            description = "Open a URL in the browser for the user to interact with",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("url") { put("type", JsonPrimitive("string")) }
                }
                putJsonArray("required") { add(JsonPrimitive("url")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "browser-read",
            description = "Fetch and read the text content of a web page",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("url") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("URL to read")) }
                }
                putJsonArray("required") { add(JsonPrimitive("url")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "browser-cookies",
            description = "Extract cookies stored for a domain from the WebView cookie store",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("domain") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Domain to get cookies for (e.g. example.com)")) }
                }
                putJsonArray("required") { add(JsonPrimitive("domain")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "android-reminder-list",
            description = "List upcoming reminders",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("days_ahead") { put("type", JsonPrimitive("integer")); put("description", JsonPrimitive("Days to look ahead (default 7)")) }
                }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "android-reminder-create",
            description = "Create a new reminder with alert",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("title") { put("type", JsonPrimitive("string")) }
                    putJsonObject("date") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("ISO date yyyy-MM-ddTHH:mm")) }
                    putJsonObject("notes") { put("type", JsonPrimitive("string")) }
                }
                putJsonArray("required") { add(JsonPrimitive("title")); add(JsonPrimitive("date")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "camera-capture",
            description = "Take a photo with the device camera",
            parameters = buildJsonObject { put("type", JsonPrimitive("object")); putJsonObject("properties") {} }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "photo-pick",
            description = "Pick a photo from the device gallery",
            parameters = buildJsonObject { put("type", JsonPrimitive("object")); putJsonObject("properties") {} }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "file-pick",
            description = "Pick a file from the device storage",
            parameters = buildJsonObject { put("type", JsonPrimitive("object")); putJsonObject("properties") {} }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "email-send",
            description = "Send an email via the user's configured provider (SMTP or Resend). Requires setup in Settings → Features → Send Email. If not configured, tell the user to set it up first.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("to") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Comma-separated recipient email addresses. Optional — if omitted, the email is sent to the default recipient configured in Settings → Features → Send Email. Only set this when the user names a specific address.")) }
                    putJsonObject("subject") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Email subject line.")) }
                    putJsonObject("body") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Email body text (plain text).")) }
                }
                putJsonArray("required") { add(JsonPrimitive("subject")); add(JsonPrimitive("body")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "oauth-authenticate",
            description = "Start an OAuth authentication flow in the browser",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("url") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("OAuth authorization URL")) }
                }
                putJsonArray("required") { add(JsonPrimitive("url")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "external-list",
            description = "List files and folders in a mounted external folder. Use this to browse user-mounted directories (e.g. Obsidian vault, project folders).",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("container") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Mount name (e.g. 'obsidian', 'notes')")) }
                    putJsonObject("path") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Relative path within the mount (empty or '/' for root)")) }
                }
                putJsonArray("required") { add(JsonPrimitive("container")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "external-read",
            description = "Read a file from a mounted external folder",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("container") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Mount name")) }
                    putJsonObject("path") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Relative file path within the mount")) }
                }
                putJsonArray("required") { add(JsonPrimitive("container")); add(JsonPrimitive("path")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "external-write",
            description = "Write content to a file in a mounted external folder (requires read-write mount)",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("container") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Mount name")) }
                    putJsonObject("path") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Relative file path within the mount")) }
                    putJsonObject("content") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Text content to write")) }
                }
                putJsonArray("required") { add(JsonPrimitive("container")); add(JsonPrimitive("path")); add(JsonPrimitive("content")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "delegate-task",
            description = "Delegate a complex multi-step task to background agent(s) that can use tools in parallel. Use for deep analysis and multi-source tasks; avoid for simple one-step tasks.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("task") {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Detailed overall task description"))
                    }
                    putJsonObject("subtasks") {
                        put("type", JsonPrimitive("array"))
                        put("description", JsonPrimitive("Optional parallel subtasks"))
                        putJsonObject("items") {
                            put("type", JsonPrimitive("object"))
                            putJsonObject("properties") {
                                putJsonObject("description") {
                                    put("type", JsonPrimitive("string"))
                                }
                                putJsonObject("tools") {
                                    put("type", JsonPrimitive("array"))
                                    putJsonObject("items") { put("type", JsonPrimitive("string")) }
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("description")) }
                        }
                    }
                    putJsonObject("context") {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Relevant context for subagent execution"))
                    }
                    putJsonObject("max_seconds") {
                        put("type", JsonPrimitive("number"))
                        put("description", JsonPrimitive("Optional per-subagent timeout in seconds. Default 60, max 180. Raise only for genuinely deep work — long values delay the spoken reply."))
                    }
                    putJsonObject("max_turns") {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Optional per-subagent tool-use round limit. Default 10, max 20. Raise when the task needs many sequential tool calls."))
                    }
                }
                putJsonArray("required") { add(JsonPrimitive("task")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "timer",
            description = "In-app countdown timer. Actions: create (needs duration_seconds; optional label), list (returns active timers), cancel (needs id), cancel_all. Prefer this over android-alarm when the user says 'in N minutes' rather than an absolute time.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("action") {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("One of: create, list, cancel, cancel_all."))
                        put("enum", buildJsonArray {
                            add(JsonPrimitive("create")); add(JsonPrimitive("cancel")); add(JsonPrimitive("cancel_all")); add(JsonPrimitive("list"))
                        })
                    }
                    putJsonObject("label") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Short label for the timer (e.g. 'tea', 'break'). Optional.")) }
                    putJsonObject("duration_seconds") { put("type", JsonPrimitive("number")); put("description", JsonPrimitive("Countdown length in seconds (for create).")) }
                    putJsonObject("id") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Timer ID to cancel.")) }
                }
                putJsonArray("required") { add(JsonPrimitive("action")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "media",
            description = "Control the built-in audio/video player. Actions: load (path), play, pause, resume, stop, next, previous, mode, status.",
            parameters = buildJsonObject {
                put("type", JsonPrimitive("object"))
                putJsonObject("properties") {
                    putJsonObject("action") {
                        put("type", JsonPrimitive("string"))
                        put("enum", buildJsonArray {
                            add(JsonPrimitive("load")); add(JsonPrimitive("play")); add(JsonPrimitive("pause"))
                            add(JsonPrimitive("resume")); add(JsonPrimitive("stop")); add(JsonPrimitive("next"))
                            add(JsonPrimitive("previous")); add(JsonPrimitive("mode")); add(JsonPrimitive("status"))
                        })
                    }
                    putJsonObject("path") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("Directory to scan for media (for load)")) }
                    putJsonObject("recursive") { put("type", JsonPrimitive("boolean")); put("description", JsonPrimitive("Recursive scan (for load)")) }
                    putJsonObject("index") { put("type", JsonPrimitive("integer")); put("description", JsonPrimitive("Playlist index (for play)")) }
                    putJsonObject("mode") { put("type", JsonPrimitive("string")); put("description", JsonPrimitive("sequential, loop, random, repeat_one")) }
                }
                putJsonArray("required") { add(JsonPrimitive("action")) }
            }
        )),
        ToolDefinition(function = ToolFunctionDef(
            name = "app-exit",
            description = "Exit the application",
            parameters = buildJsonObject { put("type", JsonPrimitive("object")); putJsonObject("properties") {} }
        )),
    )
}
