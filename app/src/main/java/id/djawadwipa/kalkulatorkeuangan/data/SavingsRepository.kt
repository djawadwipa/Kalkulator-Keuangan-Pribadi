package id.djawadwipa.kalkulatorkeuangan.data

import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDao
import id.djawadwipa.kalkulatorkeuangan.data.local.SavingsContributionEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.SavingsContributionRecord
import id.djawadwipa.kalkulatorkeuangan.data.local.SavingsGoalEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.SavingsGoalRecord
import id.djawadwipa.kalkulatorkeuangan.domain.SavingsCalculator
import id.djawadwipa.kalkulatorkeuangan.model.SavingsContribution
import id.djawadwipa.kalkulatorkeuangan.model.SavingsContributionDraft
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoal
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalDraft
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavingsRepository(
    private val dao: FinanceDao,
) {
    val goals: Flow<List<SavingsGoal>> = dao.observeSavingsGoals().map { rows ->
        val now = System.currentTimeMillis()
        rows.map { it.toModel(now) }
    }

    val contributions: Flow<List<SavingsContribution>> = dao.observeSavingsContributions().map { rows ->
        rows.map(SavingsContributionRecord::toModel)
    }

    suspend fun saveGoal(id: Long?, draft: SavingsGoalDraft) {
        val error = SavingsCalculator.validateGoal(draft)
        require(error == null) { error.orEmpty() }
        val now = System.currentTimeMillis()
        if (id == null) {
            dao.insertSavingsGoal(
                SavingsGoalEntity(
                    name = draft.name.trim(),
                    type = draft.type.name,
                    targetAmount = draft.targetAmount,
                    targetDate = draft.targetDate,
                    monthlyContributionTarget = draft.monthlyContributionTarget,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            require(id > 0L) { "ID target tidak valid" }
            val existing = requireNotNull(dao.findSavingsGoalById(id)) { "Target tidak ditemukan" }
            dao.updateSavingsGoal(
                existing.copy(
                    name = draft.name.trim(),
                    type = draft.type.name,
                    targetAmount = draft.targetAmount,
                    targetDate = draft.targetDate,
                    monthlyContributionTarget = draft.monthlyContributionTarget,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun deleteGoal(id: Long) {
        require(id > 0L) { "ID target tidak valid" }
        dao.deleteSavingsGoalById(id)
    }

    suspend fun addContribution(draft: SavingsContributionDraft) {
        val error = SavingsCalculator.validateContribution(draft)
        require(error == null) { error.orEmpty() }
        requireNotNull(dao.findSavingsGoalById(draft.goalId)) { "Target tidak ditemukan" }
        dao.insertSavingsContribution(
            SavingsContributionEntity(
                goalId = draft.goalId,
                amount = draft.amount,
                contributedAt = draft.contributedAt,
                note = draft.note.trim(),
            ),
        )
    }

    suspend fun deleteContribution(id: Long) {
        require(id > 0L) { "ID setoran tidak valid" }
        dao.deleteSavingsContributionById(id)
    }
}

internal fun SavingsGoalRecord.toModel(now: Long = System.currentTimeMillis()): SavingsGoal = SavingsCalculator.goal(
    id = id,
    name = name,
    type = SavingsGoalType.valueOf(type),
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    targetDate = targetDate,
    monthlyContributionTarget = monthlyContributionTarget,
    createdAt = createdAt,
    updatedAt = updatedAt,
    now = now,
)

private fun SavingsContributionRecord.toModel(): SavingsContribution = SavingsContribution(
    id = id,
    goalId = goalId,
    goalName = goalName,
    amount = amount,
    contributedAt = contributedAt,
    note = note,
)
