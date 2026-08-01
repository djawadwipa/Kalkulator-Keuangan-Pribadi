package id.djawadwipa.kalkulatorkeuangan.data

import id.djawadwipa.kalkulatorkeuangan.data.local.DebtEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.DebtPaymentEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.DebtPaymentRecord
import id.djawadwipa.kalkulatorkeuangan.data.local.DebtRecord
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDao
import id.djawadwipa.kalkulatorkeuangan.model.Debt
import id.djawadwipa.kalkulatorkeuangan.model.DebtDraft
import id.djawadwipa.kalkulatorkeuangan.model.DebtPayment
import id.djawadwipa.kalkulatorkeuangan.model.DebtPaymentDraft
import id.djawadwipa.kalkulatorkeuangan.model.DebtType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DebtRepository(
    private val dao: FinanceDao,
) {
    val debts: Flow<List<Debt>> = dao.observeDebts().map { rows -> rows.map(DebtRecord::toModel) }

    val payments: Flow<List<DebtPayment>> = dao.observeDebtPayments().map { rows ->
        rows.map(DebtPaymentRecord::toModel)
    }

    suspend fun saveDebt(id: Long?, draft: DebtDraft) {
        validateDebt(draft)
        val now = System.currentTimeMillis()
        if (id == null) {
            dao.insertDebt(
                DebtEntity(
                    name = draft.name.trim(),
                    creditor = draft.creditor.trim(),
                    type = draft.type.name,
                    startingBalance = draft.startingBalance,
                    annualInterestRate = draft.annualInterestRate,
                    minimumPayment = draft.minimumPayment,
                    dueDay = draft.dueDay,
                    startDate = draft.startDate,
                    targetPayoffDate = draft.targetPayoffDate,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            require(id > 0L) { "ID utang tidak valid" }
            val existing = requireNotNull(dao.findDebtById(id)) { "Utang tidak ditemukan" }
            val paidAmount = dao.debtPaidAmount(id)
            require(draft.startingBalance >= paidAmount) {
                "Saldo awal tidak boleh lebih kecil dari total pembayaran ${paidAmount}."
            }
            dao.updateDebt(
                existing.copy(
                    name = draft.name.trim(),
                    creditor = draft.creditor.trim(),
                    type = draft.type.name,
                    startingBalance = draft.startingBalance,
                    annualInterestRate = draft.annualInterestRate,
                    minimumPayment = draft.minimumPayment,
                    dueDay = draft.dueDay,
                    startDate = draft.startDate,
                    targetPayoffDate = draft.targetPayoffDate,
                    updatedAt = now,
                ),
            )
        }
    }

    suspend fun deleteDebt(id: Long) {
        require(id > 0L) { "ID utang tidak valid" }
        dao.deleteDebtById(id)
    }

    suspend fun addPayment(draft: DebtPaymentDraft) {
        require(draft.debtId > 0L) { "Utang belum dipilih" }
        require(draft.amount > 0L) { "Nominal pembayaran harus lebih dari nol" }
        require(draft.paidAt > 0L) { "Tanggal pembayaran tidak valid" }
        require(draft.note.length <= 120) { "Catatan maksimal 120 karakter" }
        val debt = requireNotNull(dao.findDebtById(draft.debtId)) { "Utang tidak ditemukan" }
        val paidAmount = dao.debtPaidAmount(draft.debtId)
        val remaining = (debt.startingBalance - paidAmount).coerceAtLeast(0L)
        require(remaining > 0L) { "Utang ini sudah lunas" }
        require(draft.amount <= remaining) {
            "Pembayaran melebihi sisa utang. Maksimal ${remaining}."
        }
        dao.insertDebtPayment(
            DebtPaymentEntity(
                debtId = draft.debtId,
                amount = draft.amount,
                paidAt = draft.paidAt,
                note = draft.note.trim(),
            ),
        )
    }

    suspend fun deletePayment(id: Long) {
        require(id > 0L) { "ID pembayaran tidak valid" }
        dao.deleteDebtPaymentById(id)
    }

    private fun validateDebt(draft: DebtDraft) {
        require(draft.name.isNotBlank()) { "Nama utang wajib diisi" }
        require(draft.name.length <= 60) { "Nama utang maksimal 60 karakter" }
        require(draft.creditor.length <= 60) { "Nama kreditur maksimal 60 karakter" }
        require(draft.startingBalance > 0L) { "Saldo utang harus lebih dari nol" }
        require(draft.annualInterestRate.isFinite() && draft.annualInterestRate in 0.0..100.0) {
            "Bunga tahunan harus antara 0 dan 100 persen"
        }
        require(draft.minimumPayment > 0L) { "Pembayaran minimum harus lebih dari nol" }
        require(draft.dueDay in 1..31) { "Tanggal jatuh tempo harus 1 sampai 31" }
        require(draft.startDate > 0L) { "Tanggal mulai tidak valid" }
        require(draft.targetPayoffDate == null || draft.targetPayoffDate >= draft.startDate) {
            "Target pelunasan tidak boleh sebelum tanggal mulai"
        }
    }
}

private fun DebtRecord.toModel(): Debt = Debt(
    id = id,
    name = name,
    creditor = creditor,
    type = DebtType.valueOf(type),
    startingBalance = startingBalance,
    annualInterestRate = annualInterestRate,
    minimumPayment = minimumPayment,
    dueDay = dueDay,
    startDate = startDate,
    targetPayoffDate = targetPayoffDate,
    paidAmount = paidAmount,
    currentBalance = currentBalance,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun DebtPaymentRecord.toModel(): DebtPayment = DebtPayment(
    id = id,
    debtId = debtId,
    debtName = debtName,
    amount = amount,
    paidAt = paidAt,
    note = note,
)
