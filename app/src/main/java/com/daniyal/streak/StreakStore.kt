package com.daniyal.streak

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Local-only storage and calendar-day streak rules for Steady. */
object StreakStore {
    private const val PREFS = "streak_prefs"
    private const val KEY_COUNT = "count"
    private const val KEY_LAST_DONE = "last_done_epoch_day"
    private const val KEY_GRACE = "grace_days"
    private const val KEY_BEST = "best_streak"
    private const val KEY_RECOVERY_COUNT = "recovery_count"
    private const val KEY_LAST_SETBACK_DATE = "last_setback_date"
    private const val KEY_LAST_SETBACK_NOTE = "last_setback_note"
    private const val KEY_RECOVERY_HISTORY = "recovery_history_json"

    const val MAX_GRACE = 2
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { preferences ->
            // Migrate the previous version's active count into best-streak storage once.
            if (!preferences.contains(KEY_BEST) && preferences.contains(KEY_COUNT)) {
                preferences.edit().putInt(KEY_BEST, preferences.getInt(KEY_COUNT, 0)).commit()
            }
        }

    private fun today(): Long = LocalDate.now().toEpochDay()
    private fun todayLabel(): String = LocalDate.now().format(dateFormatter)

    /** Epoch millis of the next local midnight. */
    fun nextMidnightMillis(): Long =
        LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun graceDays(context: Context): Int =
        prefs(context).getInt(KEY_GRACE, 0).coerceIn(0, MAX_GRACE)

    fun setGraceDays(context: Context, days: Int) {
        prefs(context).edit().putInt(KEY_GRACE, days.coerceIn(0, MAX_GRACE)).commit()
    }

    fun ruleText(context: Context): String = when (graceDays(context)) {
        0 -> "Strict mode · miss a day and the active streak resets."
        1 -> "Flexible mode · one missed day is allowed."
        else -> "Flexible mode · up to two missed days are allowed."
    }

    /** Current streak is shown as zero once the selected grace window has expired. */
    fun current(context: Context): Int {
        val p = prefs(context)
        val last = p.getLong(KEY_LAST_DONE, -1L)
        return StreakRules.current(last, p.getInt(KEY_COUNT, 0), today(), graceDays(context))
    }

    fun best(context: Context): Int = prefs(context).getInt(KEY_BEST, 0)
    fun recoveryCount(context: Context): Int = prefs(context).getInt(KEY_RECOVERY_COUNT, 0)
    fun lastSetbackDate(context: Context): String =
        prefs(context).getString(KEY_LAST_SETBACK_DATE, "No recovery check-ins yet") ?: "No recovery check-ins yet"
    fun lastSetbackNote(context: Context): String =
        prefs(context).getString(KEY_LAST_SETBACK_NOTE, "")?.takeIf { it.isNotBlank() } ?: "No note saved."

    /** Most recent recovery entries, stored locally and capped to avoid unbounded growth. */
    fun recoveryHistory(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_RECOVERY_HISTORY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val note = item.optString("note").ifBlank { "No note saved." }
                "${item.optString("date")}\n$note"
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isDoneToday(context: Context): Boolean =
        prefs(context).getLong(KEY_LAST_DONE, -1L) == today()

    /** One check-in per calendar day. Returns the resulting active streak. */
    fun markDone(context: Context): Int {
        val p = prefs(context)
        val last = p.getLong(KEY_LAST_DONE, -1L)
        val today = today()
        if (last == today) return p.getInt(KEY_COUNT, 0)

        val newCount = StreakRules.next(last, p.getInt(KEY_COUNT, 0), today, graceDays(context))
        val newBest = StreakRules.best(p.getInt(KEY_BEST, 0), newCount)
        p.edit()
            .putInt(KEY_COUNT, newCount)
            .putLong(KEY_LAST_DONE, today)
            .putInt(KEY_BEST, newBest)
            .commit()
        return newCount
    }

    /** Records a voluntary setback/recovery check-in and clears only the active streak. */
    fun recordSetback(context: Context, note: String) {
        val p = prefs(context)
        val cleanNote = note.trim().take(240)
        val history = try {
            JSONArray(p.getString(KEY_RECOVERY_HISTORY, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
        val updated = JSONArray()
        updated.put(JSONObject().put("date", todayLabel()).put("note", cleanNote))
        for (index in 0 until minOf(history.length(), 19)) updated.put(history.getJSONObject(index))

        p.edit()
            .putInt(KEY_RECOVERY_COUNT, p.getInt(KEY_RECOVERY_COUNT, 0) + 1)
            .putString(KEY_LAST_SETBACK_DATE, todayLabel())
            .putString(KEY_LAST_SETBACK_NOTE, cleanNote)
            .putString(KEY_RECOVERY_HISTORY, updated.toString())
            .remove(KEY_COUNT)
            .remove(KEY_LAST_DONE)
            .commit()
    }

    /** Manual reset keeps best streak and recovery history. */
    fun reset(context: Context) {
        prefs(context).edit().remove(KEY_COUNT).remove(KEY_LAST_DONE).commit()
    }
}
