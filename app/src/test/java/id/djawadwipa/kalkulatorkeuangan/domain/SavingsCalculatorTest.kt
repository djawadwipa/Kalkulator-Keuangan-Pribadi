package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.SavingsContributionDraft
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalDraft
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalStatus
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsCalculatorTest {
    @Test
    fun goalCalculatesProgressRemainingAndEstimatedCompletion() {
        val now = 1_704_067_200_000L
        val goal = SavingsCalculator.goal(
            id = 1,
            name = "Dana Darurat",
            type = SavingsGoalType.EMERGENCY_FUND,
            targetAmount = 12_000_000,
            currentAmount = 3_000_000,
            targetDate = null,
            monthlyContributionTarget = 1_500_000,
            createdAt = now,
            updatedAt = now,
            now = now,
        )

        assertEquals(25.0, goal.progressPercent, 0.01)
        assertEquals(9_000_000L, goal.remainingAmount)
        assertEquals(SavingsGoalStatus.IN_PROGRESS, goal.status)
        assertNotNull(goal.estimatedCompletionDate)
        assertTrue(requireNotNull(goal.estimatedCompletionDate) > now)
    }

    @Test
    fun completedGoalHasNoRemainingAmount() {
        val goal = SavingsCalculator.goal(
            id = 1,
            name = "Laptop",
            type = SavingsGoalType.FINANCIAL_GOAL,
            targetAmount = 10_000_000,
            currentAmount = 12_000_000,
            targetDate = null,
            monthlyContributionTarget = 0,
            createdAt = 1,
            updatedAt = 1,
            now = 2,
        )

        assertEquals(0L, goal.remainingAmount)
        assertEquals(SavingsGoalStatus.COMPLETED, goal.status)
        assertEquals(2L, goal.estimatedCompletionDate)
    }

    @Test
    fun overviewCalculatesEmergencyFundCoverage() {
        val emergency = SavingsCalculator.goal(
            id = 1,
            name = "Dana Darurat",
            type = SavingsGoalType.EMERGENCY_FUND,
            targetAmount = 24_000_000,
            currentAmount = 8_000_000,
            targetDate = null,
            monthlyContributionTarget = 1_000_000,
            createdAt = 1,
            updatedAt = 1,
            now = 2,
        )
        val regular = SavingsCalculator.goal(
            id = 2,
            name = "Liburan",
            type = SavingsGoalType.FINANCIAL_GOAL,
            targetAmount = 10_000_000,
            currentAmount = 2_000_000,
            targetDate = null,
            monthlyContributionTarget = 500_000,
            createdAt = 1,
            updatedAt = 1,
            now = 2,
        )

        val overview = SavingsCalculator.overview(listOf(emergency, regular), monthlyExpense = 4_000_000)

        assertEquals(10_000_000L, overview.totalSaved)
        assertEquals(8_000_000L, overview.emergencyFundBalance)
        assertEquals(2.0, overview.emergencyFundMonths, 0.01)
        assertEquals(2, overview.activeGoalCount)
    }

    @Test
    fun validationRejectsInvalidGoalAndContribution() {
        assertNotNull(
            SavingsCalculator.validateGoal(
                SavingsGoalDraft(
                    name = "",
                    type = SavingsGoalType.SAVINGS,
                    targetAmount = 1_000_000,
                    targetDate = null,
                    monthlyContributionTarget = 100_000,
                ),
            ),
        )
        assertNotNull(
            SavingsCalculator.validateContribution(
                SavingsContributionDraft(
                    goalId = 1,
                    amount = 0,
                    contributedAt = 1,
                    note = "",
                ),
            ),
        )
        assertNull(
            SavingsCalculator.validateGoal(
                SavingsGoalDraft(
                    name = "Tabungan",
                    type = SavingsGoalType.SAVINGS,
                    targetAmount = 1_000_000,
                    targetDate = null,
                    monthlyContributionTarget = 100_000,
                ),
            ),
        )
    }
}
