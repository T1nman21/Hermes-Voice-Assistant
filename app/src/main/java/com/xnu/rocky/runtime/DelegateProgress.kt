//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-05-04
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime

import java.util.UUID

/**
 * Live snapshot of an in-flight delegate-task — what subtasks are running,
 * which tool the sub-agent just hit, how it ended. Maintained by
 * [SessionRuntime] as a [kotlinx.coroutines.flow.StateFlow] so the voice
 * overlay can render the sub-agent's progress in real time instead of
 * leaving the user staring at a single status line for 5–30 seconds.
 *
 * Cleared a few seconds after [SubagentEvent.PlanCompleted] so the panel
 * naturally fades away when the work is done. Mirrors iOS
 * `OpenRockyDelegateProgress`.
 */
data class DelegateProgress(
    val id: String = UUID.randomUUID().toString(),
    val taskDescription: String,
    val subtasks: List<Subtask> = emptyList(),
    val startedAtMs: Long = System.currentTimeMillis(),
    val finishedAtMs: Long? = null,
    val planSucceededCount: Int? = null,
    val planTotalCount: Int? = null
) {
    /** True once `PlanCompleted` has fired. */
    val isFinished: Boolean get() = finishedAtMs != null

    data class Subtask(
        val id: String,
        val index: Int,
        val totalCount: Int,
        val description: String,
        val status: Status,
        /**
         * Most recent ~5 tool events for compact display. Older ones rotate
         * out so the panel doesn't grow unbounded; full history still lives
         * in the timeline for users who want everything.
         */
        val toolEvents: List<ToolEvent> = emptyList(),
        val summary: String? = null,
        val succeeded: Boolean? = null,
        val elapsedSeconds: Double? = null
    ) {
        sealed class Status {
            object Running : Status()
            data class Thinking(val turn: Int) : Status()
            object Completed : Status()
        }
    }

    data class ToolEvent(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val isSkill: Boolean,
        val argsPreview: String,
        val status: Status,
        val resultPreview: String? = null,
        val elapsedSeconds: Double? = null
    ) {
        sealed class Status {
            object Running : Status()
            data class Completed(val succeeded: Boolean) : Status()
        }
    }

    companion object {
        /**
         * How many tool events to keep per subtask. Beyond this we drop the
         * oldest. Five is enough to show "what the agent is doing right now"
         * while small enough that the overlay panel stays reasonable on
         * small devices.
         */
        const val MAX_TOOL_EVENTS_PER_SUBTASK = 5
    }
}
