package id.djawadwipa.kalkulatorkeuangan.data

import id.djawadwipa.kalkulatorkeuangan.data.local.AccountEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.CategoryEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDao
import id.djawadwipa.kalkulatorkeuangan.data.local.TransactionEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.TransactionRecord
import id.djawadwipa.kalkulatorkeuangan.domain.FinancialCalculator
import id.djawadwipa.kalkulatorkeuangan.domain.TransactionValidator
import id.djawadwipa.kalkulatorkeuangan.model.AccountType
import id.djawadwipa.kalkulatorkeuangan.model.DashboardSummary
import id.djawadwipa.kalkulatorkeuangan.model.FinanceAccount
import id.djawadwipa.kalkulatorkeuangan.model.FinanceCategory
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.FinancialHealthInput
import id.djawadwipa.kalkulatorkeuangan.model.TransactionDraft
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FinanceRepository(
    private val dao: FinanceDao,
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val searchQuery = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<TransactionType?>(null)

    val accounts: Flow<List<FinanceAccount>> = dao.observeActiveAccounts().map { rows ->
        rows.map(AccountEntity::toModel)
    }

    val categories: Flow<List<FinanceCategory>> = dao.observeCategories().map { rows ->
        rows.map(CategoryEntity::toModel)
    }

    val allTransactions: Flow<List<FinanceTransaction>> =
        dao.observeTransactions(query = "", type = null).map { rows -> rows.map(TransactionRecord::toModel) }

    val transactions: Flow<List<FinanceTransaction>> = combine(searchQuery, typeFilter) { query, type ->
        query.trim() to type?.name
    }.flatMapLatest { (query, type) ->
        dao.observeTransactions(query = query, type = type)
    }.map { rows -> rows.map(TransactionRecord::toModel) }

    val summary: Flow<DashboardSummary> = allTransactions.map(::calculateSummary)

    init {
        repositoryScope.launch { seedMasterData() }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query.take(80)
    }

    fun setTypeFilter(type: TransactionType?) {
        typeFilter.value = type
    }

    suspend fun addTransaction(draft: TransactionDraft) {
        requireValid(draft)
        dao.insertTransaction(draft.toEntity())
    }

    suspend fun updateTransaction(id: Long, draft: TransactionDraft) {
        require(id > 0) { "ID transaksi tidak valid" }
        requireValid(draft)
        dao.updateTransaction(draft.toEntity(id))
    }

    suspend fun deleteTransaction(id: Long) {
        require(id > 0) { "ID transaksi tidak valid" }
        dao.deleteTransactionById(id)
    }

    suspend fun addAccount(name: String, type: AccountType) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Nama rekening tidak boleh kosong" }
        require(normalizedName.length <= 40) { "Nama rekening maksimal 40 karakter" }
        dao.insertAccount(AccountEntity(name = normalizedName, type = type.name))
    }

    suspend fun addCategory(name: String, type: TransactionType) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Nama kategori tidak boleh kosong" }
        require(normalizedName.length <= 40) { "Nama kategori maksimal 40 karakter" }
        dao.insertCategory(CategoryEntity(name = normalizedName, type = type.name))
    }

    suspend fun addDemoData() {
        seedMasterData()
        if (dao.transactionCount() > 0) return

        val account = requireNotNull(dao.firstActiveAccount())
        val now = System.currentTimeMillis()
        val samples = listOf(
            DemoTransaction(TransactionType.INCOME, 12_500_000, "Gaji", "Pemasukan bulanan", now - DAY_MILLIS * 5),
            DemoTransaction(TransactionType.EXPENSE, 2_500_000, "Tempat Tinggal", "Sewa/KPR", now - DAY_MILLIS * 4),
            DemoTransaction(TransactionType.EXPENSE, 1_250_000, "Makanan", "Belanja bulanan", now - DAY_MILLIS * 3),
            DemoTransaction(TransactionType.EXPENSE, 650_000, "Transportasi", "BBM dan parkir", now - DAY_MILLIS * 2),
            DemoTransaction(TransactionType.EXPENSE, 500_000, "Tabungan", "Dana darurat", now - DAY_MILLIS),
        )

        samples.forEach { sample ->
            val category = requireNotNull(dao.findCategory(sample.category, sample.type.name))
            dao.insertTransaction(
                TransactionEntity(
                    type = sample.type.name,
                    amount = sample.amount,
                    accountId = account.id,
                    categoryId = category.id,
                    description = sample.description,
                    occurredAt = sample.occurredAt,
                ),
            )
        }
    }

    suspend fun clearAllTransactions() {
        dao.deleteAllTransactions()
    }

    private suspend fun seedMasterData() {
        if (dao.accountCount() == 0) {
            dao.insertAccounts(
                listOf(
                    AccountEntity(name = "Dompet Utama", type = AccountType.CASH.name),
                    AccountEntity(name = "Rekening Bank", type = AccountType.BANK.name),
                    AccountEntity(name = "E-Wallet", type = AccountType.EWALLET.name),
                ),
            )
        }
        dao.insertCategories(DEFAULT_CATEGORIES)
    }

    private fun requireValid(draft: TransactionDraft) {
        val error = TransactionValidator.validate(draft)
        require(error == null) { error.orEmpty() }
    }

    private fun calculateSummary(transactions: List<FinanceTransaction>): DashboardSummary {
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val currentMonth = transactions.filter { it.occurredAt >= monthStart }
        val income = currentMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = currentMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        return DashboardSummary(
            income = income,
            expense = expense,
            balance = income - expense,
            savingsRate = FinancialCalculator.savingsRate(income, expense),
            expenseRatio = FinancialCalculator.expenseRatio(income, expense),
            healthScore = FinancialCalculator.healthScore(
                FinancialHealthInput(income = income, expense = expense),
            ),
            transactionCount = currentMonth.size,
        )
    }

    private data class DemoTransaction(
        val type: TransactionType,
        val amount: Long,
        val category: String,
        val description: String,
        val occurredAt: Long,
    )

    companion object {
        private const val DAY_MILLIS = 86_400_000L

        private val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(name = "Gaji", type = TransactionType.INCOME.name, isDefault = true),
            CategoryEntity(name = "Bonus", type = TransactionType.INCOME.name, isDefault = true),
            CategoryEntity(name = "Usaha", type = TransactionType.INCOME.name, isDefault = true),
            CategoryEntity(name = "Investasi", type = TransactionType.INCOME.name, isDefault = true),
            CategoryEntity(name = "Lainnya", type = TransactionType.INCOME.name, isDefault = true),
            CategoryEntity(name = "Makanan", type = TransactionType.EXPENSE.name, isDefault = true),
            CategoryEntity(name = "Transportasi", type = TransactionType.EXPENSE.name, isDefault = true),
            CategoryEntity(name = "Tempat Tinggal", type = TransactionType.EXPENSE.name, isDefault = true),
            CategoryEntity(name = "Tagihan", type = TransactionType.EXPENSE.name, isDefault = true),
            CategoryEntity(name = "Kesehatan", type = TransactionType.EXPENSE.name, isDefault = true),
            CategoryEntity(name = "Pendidikan", type = TransactionType.EXPENSE.name, isDefault = true),
            CategoryEntity(name = "Hiburan", type = TransactionType.EXPENSE.name, isDefault = true),
            CategoryEntity(name = "Tabungan", type = TransactionType.EXPENSE.name, isDefault = true),
            CategoryEntity(name = "Lainnya", type = TransactionType.EXPENSE.name, isDefault = true),
        )
    }
}

private fun AccountEntity.toModel(): FinanceAccount = FinanceAccount(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
    openingBalance = openingBalance,
    isArchived = isArchived,
)

private fun CategoryEntity.toModel(): FinanceCategory = FinanceCategory(
    id = id,
    name = name,
    type = TransactionType.valueOf(type),
    isDefault = isDefault,
)

private fun TransactionRecord.toModel(): FinanceTransaction = FinanceTransaction(
    id = id,
    type = TransactionType.valueOf(type),
    amount = amount,
    accountId = accountId,
    accountName = accountName,
    categoryId = categoryId,
    categoryName = categoryName,
    description = description,
    occurredAt = occurredAt,
)

private fun TransactionDraft.toEntity(id: Long = 0): TransactionEntity = TransactionEntity(
    id = id,
    type = type.name,
    amount = amount,
    accountId = accountId,
    categoryId = categoryId,
    description = description.trim(),
    occurredAt = occurredAt,
)
