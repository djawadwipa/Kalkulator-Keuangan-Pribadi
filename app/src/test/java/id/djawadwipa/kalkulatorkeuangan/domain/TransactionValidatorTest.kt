package id.djawadwipa.kalkulatorkeuangan.domain

import id.djawadwipa.kalkulatorkeuangan.model.TransactionDraft
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionValidatorTest {
    private val validDraft = TransactionDraft(
        type = TransactionType.EXPENSE,
        amount = 50_000,
        accountId = 1,
        categoryId = 1,
        description = "Belanja",
        occurredAt = 1_700_000_000_000,
    )

    @Test
    fun validDraftHasNoError() {
        assertNull(TransactionValidator.validate(validDraft))
    }

    @Test
    fun zeroAmountIsRejected() {
        val error = TransactionValidator.validate(validDraft.copy(amount = 0))
        assertTrue(error?.contains("Nominal") == true)
    }

    @Test
    fun missingAccountIsRejected() {
        val error = TransactionValidator.validate(validDraft.copy(accountId = 0))
        assertTrue(error?.contains("Rekening") == true)
    }
}
