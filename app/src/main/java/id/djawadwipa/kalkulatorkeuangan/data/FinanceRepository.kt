package id.djawadwipa.kalkulatorkeuangan.data

import id.djawadwipa.kalkulatorkeuangan.data.local.AccountEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.BudgetEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.BudgetRecord
import id.djawadwipa.kalkulatorkeuangan.data.local.CategoryEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.CategorySpendingRecord
import id.djawadwipa.kalkulatorkeuangan.data.local.FinanceDao
import id.djawadwipa.kalkulatorkeuangan.data.local.FinancialProfileEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.SavingsContributionEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.SavingsGoalEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.TransactionEntity
import id.djawadwipa.kalkulatorkeuangan.data.local.TransactionRecord
import id.djawadwipa.kalkulatorkeuangan.domain.BudgetCalculator
import id.djawadwipa.kalkulatorkeuangan.domain.FinancialCalculator
import id.djawadwipa.kalkulatorkeuangan.domain.SavingsCalculator
import id.djawadwipa.kalkulatorkeuangan.domain.TransactionValidator
import id.djawadwipa.kalkulatorkeuangan.model.AccountType
import id.djawadwipa.kalkulatorkeuangan.model.BudgetItem
import id.djawadwipa.kalkulatorkeuangan.model.BudgetSummary
import id.djawadwipa.kalkulatorkeuangan.model.DashboardSummary
import id.djawadwipa.kalkulatorkeuangan.model.FinanceAccount
import id.djawadwipa.kalkulatorkeuangan.model.FinanceCategory
import id.djawadwipa.kalkulatorkeuangan.model.FinanceTransaction
import id.djawadwipa.kalkulatorkeuangan.model.FinancialHealthInput
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfile
import id.djawadwipa.kalkulatorkeuangan.model.FinancialProfileDraft
import id.djawadwipa.kalkulatorkeuangan.model.MonthlyAnalysis
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoal
import id.djawadwipa.kalkulatorkeuangan.model.SavingsGoalType
import id.djawadwipa.kalkulatorkeuangan.model.TransactionDraft
import id.djawadwipa.kalkulatorkeuangan.model.TransactionType
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _selectedMonthStart = MutableStateFlow(monthStart(System.currentTimeMillis()))
    private val currentMonthStart = monthStart(System.currentTimeMillis())

    val selectedMonthStart: StateFlow<Long> = _selectedMonthStart.asStateFlow()

    val accounts: Flow<List<FinanceAccount>> = dao.observeActiveAccounts().map { rows ->
        rows.map(AccountEntity::toModel)
    }

    val categories: Flow<List<FinanceCategory>> = dao.observeCategories().map { rows ->
        rows.map(CategoryEntity::toModel)
    }

    val profile: Flow<FinancialProfile> = dao.observeProfile().map { row ->
        row?.toModel() ?: FinancialProfile()
    }

    val allTransactions: Flow<List<FinanceTransaction>> =
        dao.observeTransactions(query = "", type = null).map { rows -> rows.map(TransactionRecord::toModel) }

    val transactions: Flow<List<FinanceTransaction>> = combine(searchQuery, typeFilter) { query, type ->
        query.trim() to type?.name
    }.flatMapLatest { (query, type) ->
        dao.observeTransactions(query = query, type = type)
    }.map { rows -> rows.map(TransactionRecord::toModel) }

    val budgetSummary: Flow<BudgetSummary> = _selectedMonthStart.flatMapLatest { month ->
        dao.observeBudgets(month, nextMonthStart(month)).map { rows ->
            BudgetCalculator.summary(month, rows.map(BudgetRecord::toModel))
        }
    }

    val monthlyAnalysis: Flow<MonthlyAnalysis> = combine(
        _selectedMonthStart,
        budgetSummary,
    ) { month, budget -> month to budget }
        .flatMapLatest { (month, budget) ->
            dao.observeCategorySpending(month, nextMonthStart(month)).map { rows ->
                BudgetCalculator.monthlyAnalysis(
                    monthStart = month,
                    dayOfMonth = analysisDayCount(month),
                    spending = rows.map(CategorySpendingRecord::toAnalysisPair),
                    budget = budget,
                )
            }
        }

    private val currentBudgetSummary: Flow<BudgetSummary> = dao.observeBudgets(
        currentMonthStart,
        nextMonthStart(currentMonthStart),
    ).map { rows ->
        BudgetCalculator.summary(currentMonthStart, rows.map(BudgetRecord::toModel))
    }

    private val savingsGoals: Flow<List<SavingsGoal>> = dao.observeSavingsGoals().map { rows ->
        val now = System.currentTimeMillis()
        rows.map { it.toModel(now) }
    }

    val summary: Flow<DashboardSummary> = combine(
        allTransactions,
        currentBudgetSummary,
        profile,
        savingsGoals,
    ) { transactionRows, budget, financialProfile, goals ->
        calculateSummary(transactionRows, budget, financialProfile, goals)
    }

    init {
        repositoryScope.launch { seedMasterData() }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query.take(80)
    }

    fun setTypeFilter(type: TransactionType?) {
        typeFilter.value = type
    }

    fun previousMonth() {
        _selectedMonthStart.value = shiftMonth(_selectedMonthStart.value, -1)
    }

    fun nextMonth() {
        _selectedMonthStart.value = shiftMonth(_selectedMonthStart.value, 1)
    }

    fun selectCurrentMonth() {
        _selectedMonthStart.value = monthStart(System.currentTimeMillis())
    }

    suspend fun saveProfile(draft: FinancialProfileDraft) {
        val error = BudgetCalculator.validateProfile(draft)
        require(error == null) { error.orEmpty() }
        dao.saveProfile(
            FinancialProfileEntity(
                displayName = draft.displayName.trim(),
                monthlyIncomeTarget = draft.monthlyIncomeTarget,
                savingsTargetPercent = draft.savingsTargetPercent,
                currencyCode = "IDR",
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun saveBudget(selectedMonthStart: Long, categoryId: Long, limitAmount: Long) {
        val error = BudgetCalculator.validateBudget(categoryId, limitAmount)
        require(error == null) { error.orEmpty() }
        val category = requireNotNull(dao.findCategoryById(categoryId)) { "Kategori tidak ditemukan" }
        require(category.type == TransactionType.EXPENSE.name) { "Budget hanya dapat dibuat untuk kategori pengeluaran" }

        val normalizedMonth = monthStart(selectedMonthStart)
        val now = System.currentTimeMillis()
        val existingId = dao.findBudgetId(normalizedMonth, categoryId)
        val entity = BudgetEntity(
            id = existingId ?: 0,
            monthStart = normalizedMonth,
            categoryId = categoryId,
            limitAmount = limitAmount,
            updatedAt = now,
        )
        if (existingId == null) dao.insertBudget(entity) else dao.updateBudget(entity)
    }

    suspend fun deleteBudget(id: Long) {
        require(id > 0L) { "ID budget tidak valid" }
        dao.deleteBudgetById(id)
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

        saveProfile(
            FinancialProfileDraft(
                displayName = "Pengguna",
                monthlyIncomeTarget = 12_500_000,
                savingsTargetPercent = 20,
            ),
        )
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
        listOf(
            "Makanan" to 2_000_000L,
            "Transportasi" to 1_000_000L,
            "Tempat Tinggal" to 3_000_000L,
        ).forEach { (name, amount) ->
            val category = requireNotNull(dao.findCategory(name, TransactionType.EXPENSE.name))
            saveBudget(currentMonthStart, category.id, amount)
        }
        if (dao.savingsGoalCount() == 0) {
            val emergencyGoalId = dao.insertSavingsGoal(
                SavingsGoalEntity(
                    name = "Dana Darurat 6 Bulan",
                    type = SavingsGoalType.EMERGENCY_FUND.name,
                    targetAmount = 24_000_000,
                    targetDate = shiftMonth(currentMonthStart, 12),
                    monthlyContributionTarget = 1_500_000,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            dao.insertSavingsContribution(
                SavingsContributionEntity(
                    goalId = emergencyGoalId,
                    amount = 6_000_000,
                    contributedAt = now - DAY_MILLIS,
                    note = "Saldo awal dana darurat",
                ),
            )
            val financialGoalId = dao.insertSavingsGoal(
                SavingsGoalEntity(
                    name = "Liburan Keluarga",
                    type = SavingsGoalType.FINANCIAL_GOAL.name,
                    targetAmount = 10_000_000,
                    targetDate = shiftMonth(currentMonthStart, 8),
                    monthlyContributionTarget = 1_000_000,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            dao.insertSavingsContribution(
                SavingsContributionEntity(
                    goalId = financialGoalId,
                    amount = 2_000_000,
                    contributedAt = now,
                    note = "Setoran pertama",
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
        if (dao.getProfile() == null) {
            dao.saveProfile(
                FinancialProfileEntity(
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun requireValid(draft: TransactionDraft) {
        val error = TransactionValidator.validate(draft)
        require(error == null) { error.orEmpty() }
    }

    private fun calculateSummary(
        transactions: List<FinanceTransaction>,
        budget: BudgetSummary,
        profile: FinancialProfile,
        goals: List<SavingsGoal>,
    ): DashboardSummary {
        val nextMonth = nextMonthStart(currentMonthStart)
        val currentMonth = transactions.filter {
            it.occurredAt >= currentMonthStart && it.occurredAt < nextMonth
        }
        val income = currentMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = currentMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val savingsRate = FinancialCalculator.savingsRate(income, expense)
        val savingsOverview = SavingsCalculator.overview(goals, expense)

        return DashboardSummary(
            income = income,
            expense = expense,
            balance = income - expense,
            savingsRate = savingsRate,
            expenseRatio = FinancialCalculator.expenseRatio(income, expense),
            healthScore = FinancialCalculator.healthScore(
                FinancialHealthInput(
                    income = income,
                    expense = expense,
                    emergencyFundMonths = savingsOverview.emergencyFundMonths,
                    budgetAdherence = budget.adherencePercent,
                ),
            ),
            transactionCount = currentMonth.size,
            incomeTargetProgress = if (profile.monthlyIncomeTarget <= 0L) {
                0.0
            } else {
                income.toDouble() / profile.monthlyIncomeTarget * 100.0
            },
            savingsTargetGap = profile.savingsTargetPercent - savingsRate,
            totalSavings = savingsOverview.totalSaved,
            emergencyFundMonths = savingsOverview.emergencyFundMonths,
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

private fun FinancialProfileEntity.toModel(): FinancialProfile = FinancialProfile(
    displayName = displayName,
    monthlyIncomeTarget = monthlyIncomeTarget,
    savingsTargetPercent = savingsTargetPercent,
    currencyCode = currencyCode,
    updatedAt = updatedAt,
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

private fun BudgetRecord.toModel(): BudgetItem = BudgetCalculator.budgetItem(
    id = id,
    monthStart = monthStart,
    categoryId = categoryId,
    categoryName = categoryName,
    limitAmount = limitAmount,
    spentAmount = spentAmount,
)

private fun CategorySpendingRecord.toAnalysisPair(): Pair<Pair<Long, String>, Long> =
    (categoryId to categoryName) to amount

private fun TransactionDraft.toEntity(id: Long = 0): TransactionEntity = TransactionEntity(
    id = id,
    type = type.name,
    amount = amount,
    accountId = accountId,
    categoryId = categoryId,
    description = description.trim(),
    occurredAt = occurredAt,
)

private fun monthStart(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun nextMonthStart(monthStart: Long): Long = shiftMonth(monthStart, 1)

private fun shiftMonth(monthStart: Long, amount: Int): Long = Calendar.getInstance().apply {
    timeInMillis = monthStart
    add(Calendar.MONTH, amount)
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun analysisDayCount(monthStart: Long): Int {
    val selected = Calendar.getInstance().apply { timeInMillis = monthStart }
    val now = Calendar.getInstance()
    return if (
        selected.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        selected.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    ) {
        now.get(Calendar.DAY_OF_MONTH)
    } else {
        selected.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}
