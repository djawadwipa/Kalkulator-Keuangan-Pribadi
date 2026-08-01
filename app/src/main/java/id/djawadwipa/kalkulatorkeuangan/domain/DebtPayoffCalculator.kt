package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.Debt
import id.djawadwipa.kalkulatorkeuangan.model.DebtOverview
import id.djawadwipa.kalkulatorkeuangan.model.DebtPayoffPlan
import id.djawadwipa.kalkulatorkeuangan.model.DebtPayoffStep
import id.djawadwipa.kalkulatorkeuangan.model.PayoffStrategy
import java.util.Calendar
import kotlin.math.roundToLong

object DebtPayoffCalculator {
    private const val MAX_MONTHS = 1_200

    fun overview(debts: List<Debt>): DebtOverview {
        val active = debts.filterNot(Debt::isPaidOff)
        val starting = active.sumOf(Debt::startingBalance)
        val paid = active.sumOf(Debt::paidAmount)
        val balance = active.sumOf(Debt::currentBalance)
        return DebtOverview(
            activeCount = active.size,
            totalStartingBalance = starting,
            totalPaid = paid,
            totalBalance = balance,
            minimumMonthlyPayment = active.sumOf { minOf(it.minimumPayment, it.currentBalance) },
            progressPercent = if (starting <= 0L) 0.0 else {
                (paid.toDouble() / starting * 100.0).coerceIn(0.0, 100.0)
            },
        )
    }

    fun simulate(
        debts: List<Debt>,
        monthlyBudget: Long,
        strategy: PayoffStrategy,
        startAt: Long = System.currentTimeMillis(),
    ): DebtPayoffPlan {
        val activeDebts = debts.filter { it.currentBalance > 0L }
        val totalBalance = activeDebts.sumOf(Debt::currentBalance)
        val minimumRequired = activeDebts.sumOf { minOf(it.minimumPayment, it.currentBalance) }

        if (activeDebts.isEmpty()) {
            return DebtPayoffPlan(
                strategy = strategy,
                monthlyBudget = monthlyBudget,
                minimumRequired = 0,
                totalBalance = 0,
                monthsToDebtFree = 0,
                debtFreeAt = startAt,
                totalInterest = 0,
                feasible = true,
                message = "Tidak ada utang aktif.",
                steps = emptyList(),
            )
        }

        if (monthlyBudget < minimumRequired || monthlyBudget <= 0L) {
            return DebtPayoffPlan(
                strategy = strategy,
                monthlyBudget = monthlyBudget,
                minimumRequired = minimumRequired,
                totalBalance = totalBalance,
                monthsToDebtFree = null,
                debtFreeAt = null,
                totalInterest = 0,
                feasible = false,
                message = "Anggaran bulanan minimal harus sebesar Rp$minimumRequired.",
                steps = emptyList(),
            )
        }

        val working = activeDebts.map { debt ->
            MutableDebt(
                debt = debt,
                initialBalance = debt.currentBalance.toDouble(),
                balance = debt.currentBalance.toDouble(),
            )
        }
        val initialOrder = ordered(working, strategy).mapIndexed { index, debt -> debt.debt.id to index + 1 }.toMap()
        var completedMonth: Int? = null

        for (month in 1..MAX_MONTHS) {
            val current = working.filter { it.balance > 0.005 }
            if (current.isEmpty()) {
                completedMonth = month - 1
                break
            }

            current.forEach { item ->
                val interest = item.balance * item.debt.annualInterestRate / 1_200.0
                item.balance += interest
                item.interestPaid += interest
            }

            var remaining = monthlyBudget.toDouble()
            current.forEach { item ->
                val payment = minOf(item.debt.minimumPayment.toDouble(), item.balance, remaining)
                item.balance -= payment
                item.totalPaid += payment
                remaining -= payment
            }

            ordered(working.filter { it.balance > 0.005 }, strategy).forEach { item ->
                if (remaining <= 0.0) return@forEach
                val payment = minOf(item.balance, remaining)
                item.balance -= payment
                item.totalPaid += payment
                remaining -= payment
            }

            working.filter { it.balance <= 0.005 && it.payoffMonth == null }.forEach { item ->
                item.balance = 0.0
                item.payoffMonth = month
            }

            if (working.all { it.balance <= 0.005 }) {
                completedMonth = month
                break
            }
        }

        if (completedMonth == null) {
            return DebtPayoffPlan(
                strategy = strategy,
                monthlyBudget = monthlyBudget,
                minimumRequired = minimumRequired,
                totalBalance = totalBalance,
                monthsToDebtFree = null,
                debtFreeAt = null,
                totalInterest = working.sumOf { it.interestPaid }.roundToLong(),
                feasible = false,
                message = "Utang belum lunas setelah 100 tahun. Naikkan anggaran atau minimum pembayaran.",
                steps = emptyList(),
            )
        }

        val steps = working.map { item ->
            val payoffMonth = requireNotNull(item.payoffMonth)
            DebtPayoffStep(
                order = initialOrder.getValue(item.debt.id),
                debtId = item.debt.id,
                debtName = item.debt.name,
                startingBalance = item.initialBalance.roundToLong(),
                annualInterestRate = item.debt.annualInterestRate,
                payoffMonth = payoffMonth,
                payoffDate = addMonths(startAt, payoffMonth),
                interestPaid = item.interestPaid.roundToLong(),
                totalPaid = item.totalPaid.roundToLong(),
            )
        }.sortedWith(compareBy<DebtPayoffStep> { it.payoffMonth }.thenBy { it.order })

        return DebtPayoffPlan(
            strategy = strategy,
            monthlyBudget = monthlyBudget,
            minimumRequired = minimumRequired,
            totalBalance = totalBalance,
            monthsToDebtFree = completedMonth,
            debtFreeAt = addMonths(startAt, completedMonth),
            totalInterest = working.sumOf { it.interestPaid }.roundToLong(),
            feasible = true,
            message = "Simulasi memakai bunga bulanan dan anggaran tetap.",
            steps = steps,
        )
    }

    private fun ordered(debts: List<MutableDebt>, strategy: PayoffStrategy): List<MutableDebt> = when (strategy) {
        PayoffStrategy.SNOWBALL -> debts.sortedWith(
            compareBy<MutableDebt> { it.balance }.thenByDescending { it.debt.annualInterestRate },
        )
        PayoffStrategy.AVALANCHE -> debts.sortedWith(
            compareByDescending<MutableDebt> { it.debt.annualInterestRate }.thenBy { it.balance },
        )
    }

    private fun addMonths(value: Long, months: Int): Long = Calendar.getInstance().run {
        timeInMillis = value
        add(Calendar.MONTH, months)
        timeInMillis
    }

    private data class MutableDebt(
        val debt: Debt,
        val initialBalance: Double,
        var balance: Double,
        var interestPaid: Double = 0.0,
        var totalPaid: Double = 0.0,
        var payoffMonth: Int? = null,
    )
}