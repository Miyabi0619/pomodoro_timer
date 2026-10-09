package com.miyabi0619.pomodorotimer

import com.miyabi0619.pomodorotimer.timer.Phase
import com.miyabi0619.pomodorotimer.timer.RunStatus
import com.miyabi0619.pomodorotimer.timer.TimerEngine
import com.miyabi0619.pomodorotimer.timer.TimerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TimerEngineTest {
    private val settings = TimerSettings()
    private val minute = 60_000L
    private val t0 = 1_000_000L

    @Test
    fun initialState_isIdleWorkWithDefaultDuration() {
        val state = TimerEngine.initial(settings)

        assertEquals(Phase.WORK, state.phase)
        assertEquals(RunStatus.IDLE, state.status)
        assertEquals(0, state.completedWorkCount)
        assertEquals(25 * minute, state.remainingMillis)
    }

    @Test
    fun play_setsEndTimeFromNow() {
        val state = TimerEngine.play(TimerEngine.initial(settings), t0)

        assertEquals(RunStatus.RUNNING, state.status)
        assertEquals(t0 + 25 * minute, state.endTimeMillis)
    }

    @Test
    fun pauseAndResume_preserveRemainingTime() {
        val running = TimerEngine.play(TimerEngine.initial(settings), t0)
        val paused = TimerEngine.pause(running, t0 + 10 * minute)

        assertEquals(RunStatus.PAUSED, paused.status)
        assertEquals(15 * minute, paused.remainingMillis)

        val resumedAt = t0 + 30 * minute
        val resumed = TimerEngine.play(paused, resumedAt)
        assertEquals(resumedAt + 15 * minute, resumed.endTimeMillis)
    }

    @Test
    fun completeWork_movesToShortBreakAndCountsOnce() {
        val running = TimerEngine.play(TimerEngine.initial(settings), t0)
        val next = TimerEngine.complete(running, settings, running.endTimeMillis)

        assertEquals(Phase.SHORT_BREAK, next.phase)
        assertEquals(RunStatus.RUNNING, next.status)
        assertEquals(1, next.completedWorkCount)
        assertEquals(running.endTimeMillis + 5 * minute, next.endTimeMillis)
    }

    @Test
    fun everyFourthWork_leadsToLongBreak() {
        var state = TimerEngine.play(TimerEngine.initial(settings), t0)
        val phasesAfterWork = mutableListOf<Phase>()

        repeat(4) {
            state = TimerEngine.complete(state, settings, state.endTimeMillis) // work -> break
            phasesAfterWork += state.phase
            state = TimerEngine.complete(state, settings, state.endTimeMillis) // break -> work
        }

        assertEquals(
            listOf(Phase.SHORT_BREAK, Phase.SHORT_BREAK, Phase.SHORT_BREAK, Phase.LONG_BREAK),
            phasesAfterWork,
        )
        assertEquals(Phase.WORK, state.phase)
        assertEquals(4, state.completedWorkCount)
    }

    @Test
    fun completeBreak_returnsToWorkWithoutCounting() {
        val shortBreak = TimerEngine.complete(
            TimerEngine.play(TimerEngine.initial(settings), t0),
            settings,
            t0 + 25 * minute,
        )
        val work = TimerEngine.complete(shortBreak, settings, shortBreak.endTimeMillis)

        assertEquals(Phase.WORK, work.phase)
        assertEquals(1, work.completedWorkCount)
    }

    @Test
    fun complete_whenFarOverdue_startsNextPhaseFromNow() {
        val running = TimerEngine.play(TimerEngine.initial(settings), t0)
        val now = running.endTimeMillis + 3 * 60 * minute

        val next = TimerEngine.complete(running, settings, now)

        assertEquals(now + 5 * minute, next.endTimeMillis)
    }

    @Test
    fun skipWork_doesNotCountAndGoesToShortBreak() {
        val running = TimerEngine.play(TimerEngine.initial(settings), t0)
        val next = TimerEngine.skip(running, settings, t0 + minute)

        assertEquals(Phase.SHORT_BREAK, next.phase)
        assertEquals(RunStatus.RUNNING, next.status)
        assertEquals(0, next.completedWorkCount)
    }

    @Test
    fun skipWhileIdle_staysIdle() {
        val next = TimerEngine.skip(TimerEngine.initial(settings), settings, t0)

        assertEquals(Phase.SHORT_BREAK, next.phase)
        assertEquals(RunStatus.IDLE, next.status)
        assertEquals(5 * minute, next.remainingMillis)
    }

    @Test
    fun applySettings_updatesIdleButNotRunning() {
        val updated = settings.copy(workSeconds = 30 * 60)

        val idle = TimerEngine.applySettings(TimerEngine.initial(settings), updated)
        assertEquals(30 * minute, idle.remainingMillis)

        val running = TimerEngine.play(TimerEngine.initial(settings), t0)
        assertSame(running, TimerEngine.applySettings(running, updated))
    }

    @Test
    fun applySettings_clampsPausedRemainingToNewDuration() {
        val paused = TimerEngine.pause(TimerEngine.play(TimerEngine.initial(settings), t0), t0 + minute)

        val updated = TimerEngine.applySettings(paused, settings.copy(workSeconds = 10 * 60))

        assertEquals(10 * minute, updated.remainingMillis)
    }

    @Test
    fun addTime_whileRunning_postponesEndTimeAndGrowsTotal() {
        val running = TimerEngine.play(TimerEngine.initial(settings), t0)
        val now = t0 + 10 * minute

        val extended = TimerEngine.addTime(running, 5 * minute, now)

        assertEquals(RunStatus.RUNNING, extended.status)
        assertEquals(running.endTimeMillis + 5 * minute, extended.endTimeMillis)
        assertEquals(30 * minute, extended.totalMillis)
    }

    @Test
    fun addTime_whilePausedOrIdle_extendsRemaining() {
        val idle = TimerEngine.addTime(TimerEngine.initial(settings), minute, t0)
        assertEquals(26 * minute, idle.remainingMillis)
        assertEquals(26 * minute, idle.totalMillis)

        val paused = TimerEngine.pause(TimerEngine.play(TimerEngine.initial(settings), t0), t0 + 10 * minute)
        val extended = TimerEngine.addTime(paused, 5 * minute, t0 + 20 * minute)
        assertEquals(RunStatus.PAUSED, extended.status)
        assertEquals(20 * minute, extended.remainingMillis)
    }

    @Test
    fun addTime_isClampedToMaximumAndIgnoresNonPositive() {
        val initial = TimerEngine.initial(settings)
        val maxMillis = TimerSettings.MAX_SECONDS * 1_000L

        assertEquals(maxMillis, TimerEngine.addTime(initial, maxMillis, t0).remainingMillis)
        assertSame(initial, TimerEngine.addTime(initial, 0, t0))
    }

    @Test
    fun addTime_doesNotCarryOverToNextPhase() {
        val extended = TimerEngine.addTime(TimerEngine.play(TimerEngine.initial(settings), t0), 5 * minute, t0)

        val next = TimerEngine.complete(extended, settings, extended.endTimeMillis)

        assertEquals(5 * minute, next.totalMillis)
    }

    @Test
    fun formatClock_roundsUpToWholeSeconds() {
        assertEquals("25:00", TimerEngine.formatClock(25 * minute))
        assertEquals("00:01", TimerEngine.formatClock(1))
        assertEquals("04:59", TimerEngine.formatClock(4 * minute + 58_001))
        assertEquals("00:00", TimerEngine.formatClock(-5))
    }

    @Test
    fun formatClock_showsHoursOnlyFromOneHour() {
        assertEquals("59:59", TimerEngine.formatClock(59 * minute + 59_000))
        assertEquals("1:00:00", TimerEngine.formatClock(60 * minute))
        assertEquals("99:59:59", TimerEngine.formatClock(TimerSettings.MAX_SECONDS * 1_000L))
    }

    @Test
    fun secondLevelSettings_areUsedForDuration() {
        val custom = settings.copy(workSeconds = 90)

        assertEquals(90_000L, TimerEngine.durationMillis(Phase.WORK, custom))
        assertEquals(90_000L, TimerEngine.initial(custom).remainingMillis)
    }
}
