package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.TransactionDraft

object TransactionValidator {
    const val MAX_AMOUNT = 999_999_999_999_999L

    fun validate(draft: TransactionDraft): String? = when {
        draft.amount <= 0L -> "Nominal harus lebih besar dari nol"
        draft.amount > MAX_AMOUNT -> "Nominal terlalu besar"
        draft.accountId <= 0L -> "Rekening harus dipilih"
        draft.categoryId <= 0L -> "Kategori harus dipilih"
        draft.description.length > 120 -> "Catatan maksimal 120 karakter"
        draft.occurredAt <= 0L -> "Tanggal transaksi tidak valid"
        else -> null
    }
}
