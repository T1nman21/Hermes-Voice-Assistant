//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-04-13
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HealthService(private val context: Context) {

    private fun getClient(): HealthConnectClient? {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            if (status == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getSummary(dateStr: String?): String = withContext(Dispatchers.IO) {
        val client = getClient()
            ?: return@withContext "Health Connect is not available on this device. Install the Health Connect app from Google Play."

        try {
            val date = if (!dateStr.isNullOrBlank()) {
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            } else {
                LocalDate.now()
            }

            val zone = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zone).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant()
            val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

            val results = mutableListOf<String>()
            results.add("Health Summary for $date:")

            // Steps
            try {
                val steps = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRange))
                val totalSteps = steps.records.sumOf { it.count }
                results.add("  Steps: $totalSteps")
            } catch (_: Exception) {
                results.add("  Steps: No permission or data")
            }

            // Active calories
            try {
                val energy = client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRange))
                val totalCal = energy.records.sumOf { it.energy.inKilocalories }
                results.add("  Active Calories: %.0f kcal".format(totalCal))
            } catch (_: Exception) {
                results.add("  Active Calories: No permission or data")
            }

            // Heart rate
            try {
                val hr = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRange))
                val allSamples = hr.records.flatMap { it.samples }
                if (allSamples.isNotEmpty()) {
                    val avg = allSamples.map { it.beatsPerMinute }.average()
                    val min = allSamples.minOf { it.beatsPerMinute }
                    val max = allSamples.maxOf { it.beatsPerMinute }
                    results.add("  Heart Rate: avg %.0f bpm (min %d, max %d)".format(avg, min, max))
                } else {
                    results.add("  Heart Rate: No data")
                }
            } catch (_: Exception) {
                results.add("  Heart Rate: No permission or data")
            }

            // Distance
            try {
                val dist = client.readRecords(ReadRecordsRequest(DistanceRecord::class, timeRange))
                val totalKm = dist.records.sumOf { it.distance.inKilometers }
                results.add("  Distance: %.2f km".format(totalKm))
            } catch (_: Exception) {
                results.add("  Distance: No permission or data")
            }

            // Sleep
            try {
                val sleep = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, timeRange))
                if (sleep.records.isNotEmpty()) {
                    val totalMinutes = sleep.records.sumOf {
                        java.time.Duration.between(it.startTime, it.endTime).toMinutes()
                    }
                    val hours = totalMinutes / 60
                    val mins = totalMinutes % 60
                    results.add("  Sleep: ${hours}h ${mins}m")
                } else {
                    results.add("  Sleep: No data")
                }
            } catch (_: Exception) {
                results.add("  Sleep: No permission or data")
            }

            results.joinToString("\n")
        } catch (e: Exception) {
            "Failed to read health data: ${e.message}"
        }
    }

    suspend fun getMetric(metricName: String, startDate: String?, endDate: String?, days: Int?): String = withContext(Dispatchers.IO) {
        val client = getClient()
            ?: return@withContext "Health Connect is not available on this device."

        try {
            val zone = ZoneId.systemDefault()
            val end = if (!endDate.isNullOrBlank()) {
                LocalDate.parse(endDate).plusDays(1).atStartOfDay(zone).toInstant()
            } else {
                Instant.now()
            }
            val start = if (!startDate.isNullOrBlank()) {
                LocalDate.parse(startDate).atStartOfDay(zone).toInstant()
            } else {
                end.atZone(zone).minusDays((days ?: 7).toLong()).toInstant()
            }
            val timeRange = TimeRangeFilter.between(start, end)

            when (metricName.lowercase()) {
                "steps" -> {
                    val records = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRange))
                    val total = records.records.sumOf { it.count }
                    "Steps (${formatRange(start, end)}): $total total steps across ${records.records.size} records"
                }
                "heart_rate" -> {
                    val records = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRange))
                    val samples = records.records.flatMap { it.samples }
                    if (samples.isEmpty()) return@withContext "No heart rate data for this period"
                    val avg = samples.map { it.beatsPerMinute }.average()
                    val min = samples.minOf { it.beatsPerMinute }
                    val max = samples.maxOf { it.beatsPerMinute }
                    "Heart Rate (${formatRange(start, end)}): avg %.1f bpm, min %d, max %d (%d samples)".format(avg, min, max, samples.size)
                }
                "active_energy" -> {
                    val records = client.readRecords(ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRange))
                    val total = records.records.sumOf { it.energy.inKilocalories }
                    "Active Energy (${formatRange(start, end)}): %.0f kcal".format(total)
                }
                "distance_walking_running", "distance" -> {
                    val records = client.readRecords(ReadRecordsRequest(DistanceRecord::class, timeRange))
                    val total = records.records.sumOf { it.distance.inKilometers }
                    "Distance (${formatRange(start, end)}): %.2f km".format(total)
                }
                "sleep" -> {
                    val records = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, timeRange))
                    if (records.records.isEmpty()) return@withContext "No sleep data for this period"
                    val sessions = records.records.map {
                        val mins = java.time.Duration.between(it.startTime, it.endTime).toMinutes()
                        "${it.startTime.atZone(zone).toLocalDate()}: ${mins / 60}h ${mins % 60}m"
                    }
                    "Sleep (${formatRange(start, end)}):\n" + sessions.joinToString("\n")
                }
                else -> "Unknown metric: $metricName. Supported: steps, heart_rate, active_energy, distance, sleep"
            }
        } catch (e: Exception) {
            "Failed to read health metric: ${e.message}"
        }
    }

    private fun formatRange(start: Instant, end: Instant): String {
        val zone = ZoneId.systemDefault()
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return "${start.atZone(zone).format(fmt)} to ${end.atZone(zone).format(fmt)}"
    }
}
