package com.daniyal.streak

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var countView: TextView
    private lateinit var bestView: TextView
    private lateinit var recoveryCountView: TextView
    private lateinit var recoverySummaryView: TextView
    private lateinit var ruleView: TextView
    private lateinit var doneButton: Button
    private val handler = Handler(Looper.getMainLooper())
    private val midnightRefresh = Runnable {
        refresh()
        scheduleMidnightRefresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        countView = findViewById(R.id.count)
        bestView = findViewById(R.id.best_count)
        recoveryCountView = findViewById(R.id.recovery_count)
        recoverySummaryView = findViewById(R.id.recovery_summary)
        ruleView = findViewById(R.id.rule_text)
        doneButton = findViewById(R.id.done_button)

        doneButton.setOnClickListener {
            StreakStore.markDone(this)
            refresh()
        }
        findViewById<Button>(R.id.rules_button).setOnClickListener { showRulesDialog() }
        findViewById<Button>(R.id.recovery_button).setOnClickListener { showRecoveryDialog() }
        findViewById<Button>(R.id.history_button).setOnClickListener { showRecoveryHistory() }
        findViewById<Button>(R.id.reset_button).setOnClickListener { confirmReset() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        scheduleMidnightRefresh()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(midnightRefresh)
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Reset active streak?")
            .setMessage("Your current streak will reset. Your best streak and recovery history will stay saved.")
            .setPositiveButton("Reset streak") { _, _ ->
                StreakStore.reset(this)
                refresh()
            }
            .setNegativeButton("Keep it", null)
            .show()
    }

    private fun showRulesDialog() {
        val options = arrayOf(
            "Strict · a missed day resets the active streak",
            "1 grace day · one missed day allowed",
            "2 grace days · two missed days allowed"
        )
        AlertDialog.Builder(this)
            .setTitle("Streak rules")
            .setSingleChoiceItems(options, StreakStore.graceDays(this)) { dialog, which ->
                StreakStore.setGraceDays(this, which)
                dialog.dismiss()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRecoveryDialog() {
        val input = EditText(this).apply {
            hint = "Optional: what made today difficult?"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            maxLines = 4
        }
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 8, 48, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("Reset and recover")
            .setMessage("This records a private recovery check-in and resets the active streak. Your best streak stays saved. A setback is information, not a verdict.")
            .setView(container)
            .setPositiveButton("Save recovery") { _, _ ->
                StreakStore.recordSetback(this, input.text.toString())
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRecoveryHistory() {
        val history = StreakStore.recoveryHistory(this)
        val entries = if (history.isEmpty()) "No recovery entries yet." else history.take(5).joinToString("\n\n────────────\n\n")
        AlertDialog.Builder(this)
            .setTitle("Private recovery history")
            .setMessage(
                "Recovery check-ins: ${StreakStore.recoveryCount(this)}\n" +
                "Showing the latest ${minOf(history.size, 5)} entries\n\n$entries\n\n" +
                "Stored only on this device. Up to 20 entries are kept."
            )
            .setPositiveButton("Done", null)
            .show()
    }

    private fun scheduleMidnightRefresh() {
        handler.removeCallbacks(midnightRefresh)
        val delay = (StreakStore.nextMidnightMillis() - System.currentTimeMillis() + 1000L).coerceAtLeast(1000L)
        handler.postDelayed(midnightRefresh, delay)
    }

    private fun refresh() {
        val done = StreakStore.isDoneToday(this)
        countView.text = StreakStore.current(this).toString()
        bestView.text = StreakStore.best(this).toString()
        recoveryCountView.text = StreakStore.recoveryCount(this).toString()
        recoverySummaryView.text = if (StreakStore.recoveryCount(this) == 0) {
            "A setback doesn't erase what you've learned."
        } else {
            "Last recovery check-in · ${StreakStore.lastSetbackDate(this)}"
        }
        ruleView.text = StreakStore.ruleText(this)
        doneButton.isEnabled = !done
        doneButton.text = if (done) "CHECKED IN TODAY  ✓" else "CHECK IN FOR TODAY"
        doneButton.alpha = if (done) 0.65f else 1f
        StreakWidget.updateAll(this)
    }
}
