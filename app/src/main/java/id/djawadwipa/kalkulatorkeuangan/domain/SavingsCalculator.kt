package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.SavingsContributionDraft
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoal
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalDraft
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalStatus
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalType
import id.djawadwipa.kalkulatorkeuangan.model.SavingsOverview
import java.util.Calendar
import kotlin.math.ceil

object SavingsCalculator {
    fun goal(
        id: Long,
        name: String,
        type: SavingsGoalType,
        targetAmount: Long,
        currentAmount: Long,
        targetDate: Long?,
        monthlyContributionTarget: Long,
        createdAt: Long,
        updatedAt: Long,
        now: Long = System.currentTimeMillis(),
    ): SavingsGoal {
        val safeCurrent = currentAmount.coerceAtLeast(0L)
        val remaining = (targetAmount - safeCurrent).coerceAtLeast(0L)
        val progress = if (targetAmount <= 0L) 0.0 else safeCurrent.toDouble() / targetAmount * 100.0
        val status = when {
            targetAmount > 0L && safeCurrent >= targetAmount -> SavingsGoalStatus.COMPLETED
            targetDate != null && targetDate < startOfToday(now) -> SavingsGoalStatus.OVERDUE
            safeCurrent > 0L -> SavingsGoalStatus.IN_PROGRESS
            else -> SavingsGoalStatus.NOT_STARTED
        }
        val estimatedCompletion = when {
            status == SavingsGoalStatus.COMPLETED -> now
            monthlyContributionTarget <= 0L || remaining <= 0L -> null
            else -> addMonths(now, ceil(remaining.toDouble() / monthlyContributionTarget).toInt())
        }

        return SavingsGoal(
            id = id,
            name = name,
            type = type,
            targetAmount = targetAmount,
            currentAmount = safeCurrent,
            remainingAmount = remaining,
            progressPercent = progress.coerceIn(0.0, 999.0),
            targetDate = targetDate,
            monthlyContributionTarget = monthlyContributionTarget,
            estimatedCompletionDate = estimatedCompletion,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun overview(goals: List<SavingsGoal>, monthlyExpense: Long): SavingsOverview {
        val totalSaved = goals.sumOf { it.currentAmount }
        val totalTarget = goals.sumOf { it.targetAmount }
        val emergencyBalance = goals
            .filter { it.type == SavingsGoalType.EMERGENCY_FUND }
            .sumOf { it.currentAmount }
        val emergencyMonths = if (monthlyExpense > 0L) {
            emergencyBalance.toDouble() / monthlyExpense.toDouble()
        } else {
            0.0
        }

        return SavingsOverview(
            totalSaved = totalSaved,
            totalTarget = totalTarget,
            progressPercent = if (totalTarget <= 0L) 0.0 else totalSaved.toDouble() / totalTarget * 100.0,
            emergencyFundBalance = emergencyBalance,
            emergencyFundMonths = emergencyMonths.coerceAtLeast(0.0),
            activeGoalCount = goals.count { it.status != SavingsGoalStatus.COMPLETED },
            completedGoalCount = goals.count { it.status == SavingsGoalStatus.COMPLETED },
        )
    }

    fun validateGoal(draft: SavingsGoalDraft): String? = when {
        draft.name.trim().isEmpty() -> "Nama target tidak boleh kosong"
        draft.name.trim().length > 60 -> "Nama target maksimal 60 karakter"
        draft.targetAmount <= 0L -> "Target nominal harus lebih dari nol"
        draft.targetAmount > MAX_AMOUNT -> "Target nominal terlalu besar"
        draft.monthlyContributionTarget < 0L -> "Target setoran bulanan tidak valid"
        draft.monthlyContributionTarget > MAX_AMOUNT -> "Target setoran bulanan terlalu besar"
        draft.targetDate != null && draft.targetDate < startOfToday(System.currentTimeMillis()) ->
            "Tanggal target tidak boleh berada di masa lalu"
        else -> null
    }

    fun validateContribution(draft: SavingsContributionDraft): String? = when {
        draft.goalId <= 0L -> "Target tabungan tidak valid"
        draft.amount <= 0L -> "Nominal setoran harus lebih dari nol"
        draft.amount > MAX_AMOUNT -> "Nominal setoran terlalu besar"
        draft.note.trim().length > 120 -> "Catatan maksimal 120 karakter"
        draft.contributedAt <= 0L -> "Tanggal setoran tidak valid"
        else -> null
    }

    private fun startOfToday(value: Long): Long = Calendar.getInstance().apply {
        timeInMillis = value
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun addMonths(value: Long, months: Int): Long = Calendar.getInstance().apply {
        timeInMillis = value
        add(Calendar.MONTH, months.coerceAtLeast(0))
    }.timeInMillis

    private const val MAX_AMOUNT = 999_999_999_999_999L
}
