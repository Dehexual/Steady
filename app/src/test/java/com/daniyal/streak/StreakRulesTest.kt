package com.daniyal.streak

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakRulesTest {
    @Test fun firstCheckInStartsAtOne() {
        assertEquals(1, StreakRules.next(-1L, 0, 100L, 0))
    }

    @Test fun duplicateSameDayCheckInDoesNotIncrement() {
        assertEquals(4, StreakRules.next(100L, 4, 100L, 0))
    }

    @Test fun consecutiveDayIncrementsStreak() {
        assertEquals(5, StreakRules.next(100L, 4, 101L, 0))
    }

    @Test fun strictModeBreaksAfterOneMissedDay() {
        assertEquals(0, StreakRules.current(100L, 4, 102L, 0))
        assertEquals(1, StreakRules.next(100L, 4, 102L, 0))
    }

    @Test fun oneGraceDayAllowsOneDayGap() {
        assertEquals(4, StreakRules.current(100L, 4, 102L, 1))
        assertEquals(5, StreakRules.next(100L, 4, 102L, 1))
    }

    @Test fun expiredGraceWindowBreaksStreak() {
        assertEquals(0, StreakRules.current(100L, 4, 103L, 1))
        assertEquals(1, StreakRules.next(100L, 4, 103L, 1))
    }

    @Test fun bestNeverDecreases() {
        assertEquals(12, StreakRules.best(12, 3))
        assertEquals(13, StreakRules.best(12, 13))
    }
}
