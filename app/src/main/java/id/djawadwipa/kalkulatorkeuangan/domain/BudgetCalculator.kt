package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.BudgetItem
import id.djawadwipa.kalkulatorkeuangan.model.BudgetStatus
import id.djawadwipa.kalkulatorkeuangan.model.BudgetSummary
import id.djawadwipa.kalkulatorkeuangan.model.ExpenseCategorySummary
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfileDraft
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyAnalysis
import kotlin.math.roundToLong

object BudgetCalculator {
    fun validateProfile(draft: FinancialProfileDraft): String? = when {
        draft.displayName.trim().length > 50 -> "Nama profil maksimal 50 karakter"
        draft.monthlyIncomeTarget < 0L -> "Target pemasukan tidak boleh negatif"
        draft.savingsTargetPercent !in 0..100 -> "Target tabungan harus antara 0 sampai 100 persen"
        else -> null
    }

    fun validateBudget(categoryId: Long, limitAmount: Long): String? = when {
        categoryId <= 0L -> "Kategori budget belum dipilih"
        limitAmount <= 0L -> "Nominal budget harus lebih besar dari nol"
        limitAmount > 999_999_999_999_999L -> "Nominal budget terlalu besar"
        else -> null
    }

    fun budgetItem(
        id: Long,
        monthStart: Long,
        categoryId: Long,
        categoryName: String,
        limitAmount: Long,
        spentAmount: Long,
    ): BudgetItem {
        val utilization = if (limitAmount <= 0L) 0.0 else spentAmount.toDouble() / limitAmount * 100.0
        val status = when {
            utilization > 100.0 -> BudgetStatus.EXCEEDED
            utilization >= 75.0 -> BudgetStatus.WARNING
            else -> BudgetStatus.SAFE
        }
        return BudgetItem(
            id = id,
            monthStart = monthStart,
            categoryId = categoryId,
            categoryName = categoryName,
            limitAmount = limitAmount,
            spentAmount = spentAmount,
            remainingAmount = limitAmount - spentAmount,
            utilizationPercent = utilization.coerceAtLeast(0.0),
            status = status,
        )
    }

    fun summary(monthStart: Long, items: List<BudgetItem>): BudgetSummary {
        val totalBudget = items.sumOf(BudgetItem::limitAmount)
        val totalSpent = items.sumOf(BudgetItem::spentAmount)
        val utilization = if (totalBudget <= 0L) 0.0 else totalSpent.toDouble() / totalBudget * 100.0
        val adherence = if (items.isEmpty()) {
            0.0
        } else {
            items.count { it.spentAmount <= it.limitAmount }.toDouble() / items.size * 100.0
        }
        return BudgetSummary(
            monthStart = monthStart,
            items = items,
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remaining = totalBudget - totalSpent,
            utilizationPercent = utilization.coerceAtLeast(0.0),
            adherencePercent = adherence.coerceIn(0.0, 100.0),
        )
    }

    fun monthlyAnalysis(
        monthStart: Long,
        dayOfMonth: Int,
        spending: List<Pair<Pair<Long, String>, Long>>,
        budget: BudgetSummary,
    ): MonthlyAnalysis {
        val total = spending.sumOf { it.second }
        val categories = spending.map { (category, amount) ->
            ExpenseCategorySummary(
                categoryId = category.first,
                categoryName = category.second,
                amount = amount,
                sharePercent = if (total <= 0L) 0.0 else amount.toDouble() / total * 100.0,
            )
        }
        val top = categories.maxByOrNull(ExpenseCategorySummary::amount)
        val safeDay = dayOfMonth.coerceAtLeast(1)
        return MonthlyAnalysis(
            monthStart = monthStart,
            totalExpense = total,
            averageDailyExpense = (total.toDouble() / safeDay).roundToLong(),
            topCategoryName = top?.categoryName ?: "Belum ada data",
            topCategoryAmount = top?.amount ?: 0,
            categories = categories.sortedByDescending(ExpenseCategorySummary::amount),
            budget = budget,
        )
    }
}
