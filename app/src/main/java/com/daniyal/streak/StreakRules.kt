package com.daniyal.streak

/** Pure streak calculations kept separate so their edge cases can be unit-tested. */
internal object StreakRules {
    fun current(lastDoneEpochDay: Long, storedCount: Int, todayEpochDay: Long, graceDays: Int): Int {
        if (lastDoneEpochDay < 0L) return 0
        val gap = todayEpochDay - lastDoneEpochDay
        return if (gap in 0L..(1L + graceDays.coerceIn(0, 2))) storedCount else 0
    }

    fun next(lastDoneEpochDay: Long, storedCount: Int, todayEpochDay: Long, graceDays: Int): Int {
        if (lastDoneEpochDay == todayEpochDay) return storedCount
        val gap = todayEpochDay - lastDoneEpochDay
        val alive = lastDoneEpochDay >= 0L && gap >= 1L && gap <= 1L + graceDays.coerceIn(0, 2)
        return if (alive) storedCount + 1 else 1
    }

    fun best(previousBest: Int, newCount: Int): Int = maxOf(previousBest, newCount)
}
