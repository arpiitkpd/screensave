package com.screencheck.monitor

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

/**
 * Reads real Instagram foreground time using Android's UsageStatsManager.
 *
 * IMPORTANT: PACKAGE_USAGE_STATS is a "special" permission. There is no
 * runtime dialog for it — the user must flip it on manually in
 * Settings > Apps > Special access > Usage access. We can only detect
 * the state and deep-link them to that screen.
 */
object UsageStatsHelper {

    const val INSTAGRAM_PACKAGE = "com.instagram.android"

    /** Returns true if this app currently has usage-access granted. */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Deep-links straight to the system Usage Access settings screen. */
    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Returns Instagram's total foreground time today (since local midnight),
     * in milliseconds. Returns 0 if usage access isn't granted or Instagram
     * isn't installed / hasn't been opened today.
     */
    fun getInstagramUsageTodayMillis(context: Context): Long {
        if (!hasUsageAccess(context)) return 0L

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()

        val statsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
            ?: return 0L

        // Multiple buckets can exist for the same package across the range;
        // sum them to get today's total.
        return statsList
            .filter { it.packageName == INSTAGRAM_PACKAGE }
            .sumOf { it.totalTimeInForeground }
    }

    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
