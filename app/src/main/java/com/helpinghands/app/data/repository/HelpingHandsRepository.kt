package com.helpinghands.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.helpinghands.app.data.db.DatabaseHelper
import com.helpinghands.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

data class DashboardSummary(
    val totalMembers: Int,
    val activeMembers: Int,
    val currentMonthContributionRate: Double,
    val totalContributionsCollected: Double,
    val totalLoansDisbursed: Double,
    val totalLoanPrincipalPaid: Double,
    val totalOutstandingLoan: Double,
    val totalInterestEarned: Double,
    val totalExpenses: Double,
    val cashBalance: Double
)

data class MonthlyFinancialSummary(
    val monthIndex: Int,
    val monthName: String,
    val applicableRate: Double,
    val expectedContribution: Double,
    val collectedContribution: Double,
    val penaltiesCollected: Double,
    val loansDisbursed: Double,
    val principalRepaid: Double,
    val interestEarned: Double,
    val expenses: Double,
    val netCashFlow: Double
)

class HelpingHandsRepository(context: Context) {
    private val dbHelper = DatabaseHelper.getInstance(context)

    private val _dataVersion = MutableStateFlow(0)
    val dataVersion: StateFlow<Int> = _dataVersion.asStateFlow()

    private fun notifyDataChanged() {
        _dataVersion.value = _dataVersion.value + 1
    }

    private fun currentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun currentDateTimeString(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    // ----------------------------------------------------
    // MEMBERS
    // ----------------------------------------------------
    suspend fun getMembers(): List<Member> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Member>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, member_id, name, father_husband_name, mobile, joining_date, nominee, occupation, status FROM members ORDER BY id ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    Member(
                        id = c.getLong(0),
                        memberId = c.getString(1),
                        name = c.getString(2),
                        fatherHusbandName = c.getString(3) ?: "",
                        mobile = c.getString(4) ?: "",
                        joiningDate = c.getString(5) ?: "",
                        nominee = c.getString(6) ?: "",
                        occupation = c.getString(7) ?: "",
                        status = c.getString(8) ?: "Active"
                    )
                )
            }
        }
        list
    }

    suspend fun addMember(member: Member, performedBy: String = "admin"): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("member_id", member.memberId)
            put("name", member.name)
            put("father_husband_name", member.fatherHusbandName)
            put("mobile", member.mobile)
            put("joining_date", member.joiningDate)
            put("nominee", member.nominee)
            put("occupation", member.occupation)
            put("status", member.status)
            put("active", if (member.status == "Active") 1 else 0)
        }
        val id = db.insert("members", null, cv)

        // Seed default monthly contributions for 2026 for this new member
        val months = listOf(
            "January 2026", "February 2026", "March 2026", "April 2026",
            "May 2026", "June 2026", "July 2026", "August 2026",
            "September 2026", "October 2026", "November 2026", "December 2026"
        )
        months.forEachIndexed { idx, m ->
            val applicable = if (idx >= 7) 600.0 else 500.0
            val contCv = ContentValues().apply {
                put("member_id", member.memberId)
                put("month_year", m)
                put("applicable_amount", applicable)
                put("paid_amount", 0.0)
                put("payment_date", "")
                put("penalty", 0.0)
                put("status", "PENDING")
                put("remarks", "")
            }
            db.insert("monthly_contributions", null, contCv)
        }

        logAudit(performedBy, "MEMBER_ADDED", "Member", "${member.memberId} - ${member.name}")
        notifyDataChanged()
        id
    }

    suspend fun updateMember(member: Member, performedBy: String = "admin") = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", member.name)
            put("father_husband_name", member.fatherHusbandName)
            put("mobile", member.mobile)
            put("joining_date", member.joiningDate)
            put("nominee", member.nominee)
            put("occupation", member.occupation)
            put("status", member.status)
            put("active", if (member.status == "Active") 1 else 0)
        }
        db.update("members", cv, "id = ?", arrayOf(member.id.toString()))
        logAudit(performedBy, "MEMBER_UPDATED", "Member", "${member.memberId} - ${member.name}")
        notifyDataChanged()
    }

    suspend fun deleteMember(id: Long, memberId: String, performedBy: String = "admin") = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("members", "id = ?", arrayOf(id.toString()))
        db.delete("monthly_contributions", "member_id = ?", arrayOf(memberId))
        logAudit(performedBy, "MEMBER_DELETED", "Member", memberId)
        notifyDataChanged()
    }

    suspend fun getNextMemberId(): String = withContext(Dispatchers.IO) {
        val members = getMembers()
        val highestNum = members.mapNotNull {
            it.memberId.removePrefix("THH").toIntOrNull()
        }.maxOrNull() ?: 0
        "THH%03d".format(highestNum + 1)
    }

    // ----------------------------------------------------
    // CONTRIBUTION RATES
    // ----------------------------------------------------
    suspend fun getContributionRates(): List<ContributionRate> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ContributionRate>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, effective_from, amount, notes FROM contribution_rates ORDER BY effective_from ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    ContributionRate(
                        id = c.getLong(0),
                        effectiveFrom = c.getString(1),
                        amount = c.getDouble(2),
                        notes = c.getString(3) ?: ""
                    )
                )
            }
        }
        list
    }

    suspend fun addContributionRate(rate: ContributionRate, performedBy: String = "admin") = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("effective_from", rate.effectiveFrom)
            put("amount", rate.amount)
            put("notes", rate.notes)
        }
        db.insert("contribution_rates", null, cv)
        logAudit(performedBy, "RATE_ADDED", "ContributionRate", "Effective ${rate.effectiveFrom}: ₹${rate.amount}")
        notifyDataChanged()
    }

    // ----------------------------------------------------
    // MONTHLY CONTRIBUTIONS
    // ----------------------------------------------------
    suspend fun getMonthlyContributions(monthYear: String): List<MonthlyContribution> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MonthlyContribution>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT mc.id, mc.member_id, m.name, mc.month_year, mc.applicable_amount, 
                   mc.paid_amount, mc.payment_date, mc.penalty, mc.status, mc.remarks
            FROM monthly_contributions mc
            LEFT JOIN members m ON mc.member_id = m.member_id
            WHERE mc.month_year = ?
            ORDER BY mc.member_id ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(monthYear))
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    MonthlyContribution(
                        id = c.getLong(0),
                        memberId = c.getString(1),
                        memberName = c.getString(2) ?: c.getString(1),
                        monthYear = c.getString(3),
                        applicableAmount = c.getDouble(4),
                        paidAmount = c.getDouble(5),
                        paymentDate = c.getString(6) ?: "",
                        penalty = c.getDouble(7),
                        status = c.getString(8) ?: "PENDING",
                        remarks = c.getString(9) ?: ""
                    )
                )
            }
        }
        list
    }

    suspend fun recordContributionPayment(
        memberId: String,
        monthYear: String,
        paidAmount: Double,
        paymentDate: String,
        penalty: Double,
        remarks: String,
        performedBy: String = "admin"
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("paid_amount", paidAmount)
            put("payment_date", paymentDate)
            put("penalty", penalty)
            put("status", if (paidAmount > 0) "PAID" else "PENDING")
            put("remarks", remarks)
        }
        db.update(
            "monthly_contributions",
            cv,
            "member_id = ? AND month_year = ?",
            arrayOf(memberId, monthYear)
        )

        // Also record Cash In in Cash Book
        val totalIn = paidAmount + penalty
        if (totalIn > 0) {
            val txnCv = ContentValues().apply {
                put("txn_date", paymentDate)
                put("txn_type", "Cash In")
                put("category", "Contribution")
                put("reference", "Contribution $monthYear - $memberId (Fee: ₹$paidAmount, Late: ₹$penalty)")
                put("amount", totalIn)
            }
            db.insert("cash_transactions", null, txnCv)
        }

        logAudit(performedBy, "CONTRIBUTION_PAID", "MonthlyContribution", "$memberId for $monthYear: ₹$totalIn")
        notifyDataChanged()
    }

    // ----------------------------------------------------
    // LOANS & REDUCING BALANCE EMI
    // ----------------------------------------------------
    suspend fun getLoans(): List<Loan> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Loan>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT l.id, l.loan_id, l.member_id, m.name, l.principal, 
                   l.interest_rate_monthly, l.start_date, l.term_months, l.emi, 
                   l.total_paid_principal, l.total_paid_interest, l.status, l.purpose
            FROM loans l
            LEFT JOIN members m ON l.member_id = m.member_id
            ORDER BY l.id DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    Loan(
                        id = c.getLong(0),
                        loanId = c.getString(1),
                        memberId = c.getString(2),
                        memberName = c.getString(3) ?: c.getString(2),
                        principal = c.getDouble(4),
                        interestRateMonthly = c.getDouble(5),
                        startDate = c.getString(6),
                        termMonths = c.getInt(7),
                        emi = c.getDouble(8),
                        totalPaidPrincipal = c.getDouble(9),
                        totalPaidInterest = c.getDouble(10),
                        status = c.getString(11) ?: "ACTIVE",
                        purpose = c.getString(12) ?: ""
                    )
                )
            }
        }
        list
    }

    fun calculateEMI(principal: Double, termMonths: Int, monthlyRate: Double): Double {
        if (termMonths <= 0 || principal <= 0) return 0.0
        if (monthlyRate <= 0.0) return Math.round((principal / termMonths) * 100.0) / 100.0
        val factor = (1 + monthlyRate).pow(termMonths)
        val emi = (principal * monthlyRate * factor) / (factor - 1)
        return Math.round(emi * 100.0) / 100.0
    }

    fun generateLoanSchedule(
        principal: Double,
        termMonths: Int,
        monthlyRate: Double,
        startDate: String
    ): List<LoanScheduleRow> {
        val schedule = mutableListOf<LoanScheduleRow>()
        if (termMonths <= 0 || principal <= 0) return schedule

        val emi = calculateEMI(principal, termMonths, monthlyRate)
        var balance = principal

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            calendar.time = sdf.parse(startDate) ?: Date()
        } catch (e: Exception) {
            calendar.time = Date()
        }

        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        for (m in 1..termMonths) {
            val opening = balance
            val interest = Math.round((opening * monthlyRate) * 100.0) / 100.0
            val principalComp = if (m == termMonths) {
                opening
            } else {
                (emi - interest).coerceAtMost(opening)
            }
            val adjustedEmi = if (m == termMonths) (opening + interest) else emi
            val closing = (opening - principalComp).coerceAtLeast(0.0)

            calendar.add(Calendar.MONTH, 1)
            val monthLabel = monthFormat.format(calendar.time)

            schedule.add(
                LoanScheduleRow(
                    monthIndex = m,
                    monthLabel = monthLabel,
                    openingBalance = Math.round(opening * 100.0) / 100.0,
                    emi = Math.round(adjustedEmi * 100.0) / 100.0,
                    interestComponent = Math.round(interest * 100.0) / 100.0,
                    principalComponent = Math.round(principalComp * 100.0) / 100.0,
                    closingBalance = Math.round(closing * 100.0) / 100.0
                )
            )
            balance = closing
        }
        return schedule
    }

    suspend fun createLoan(
        memberId: String,
        principal: Double,
        termMonths: Int,
        monthlyRate: Double,
        startDate: String,
        purpose: String,
        performedBy: String = "admin"
    ): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase

        val cursor = db.rawQuery("SELECT MAX(id) FROM loans", null)
        val nextNum = cursor.use {
            if (it.moveToNext()) it.getInt(0) + 1 else 1
        }
        val loanId = "LN%03d".format(nextNum)
        val emi = calculateEMI(principal, termMonths, monthlyRate)

        val cv = ContentValues().apply {
            put("loan_id", loanId)
            put("member_id", memberId)
            put("principal", principal)
            put("interest_rate_monthly", monthlyRate)
            put("start_date", startDate)
            put("term_months", termMonths)
            put("emi", emi)
            put("total_paid_principal", 0.0)
            put("total_paid_interest", 0.0)
            put("status", "ACTIVE")
            put("purpose", purpose)
        }
        val id = db.insert("loans", null, cv)

        // Cash Book: Cash Out for loan disbursal
        val txnCv = ContentValues().apply {
            put("txn_date", startDate)
            put("txn_type", "Cash Out")
            put("category", "Loan Disbursal")
            put("reference", "Loan $loanId Disbursal to $memberId")
            put("amount", principal)
        }
        db.insert("cash_transactions", null, txnCv)

        logAudit(performedBy, "LOAN_CREATED", "Loan", "$loanId to $memberId: ₹$principal for $termMonths mos")
        notifyDataChanged()
        id
    }

    suspend fun recordLoanPayment(
        loanId: Long,
        paymentDate: String,
        totalAmount: Double,
        interestComponent: Double,
        principalComponent: Double,
        notes: String,
        performedBy: String = "admin"
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase

        // 1. Insert loan payment
        val payCv = ContentValues().apply {
            put("loan_id", loanId)
            put("payment_date", paymentDate)
            put("amount", totalAmount)
            put("interest_component", interestComponent)
            put("principal_component", principalComponent)
            put("notes", notes)
        }
        db.insert("loan_payments", null, payCv)

        // 2. Update loan total paid
        val loanCursor = db.rawQuery("SELECT principal, total_paid_principal, total_paid_interest, loan_id, member_id FROM loans WHERE id = ?", arrayOf(loanId.toString()))
        loanCursor.use { c ->
            if (c.moveToNext()) {
                val principal = c.getDouble(0)
                val curPaidP = c.getDouble(1)
                val curPaidI = c.getDouble(2)
                val lId = c.getString(3)
                val mId = c.getString(4)

                val newPaidP = curPaidP + principalComponent
                val newPaidI = curPaidI + interestComponent
                val newStatus = if (newPaidP >= (principal - 0.5)) "CLOSED" else "ACTIVE"

                val upCv = ContentValues().apply {
                    put("total_paid_principal", newPaidP)
                    put("total_paid_interest", newPaidI)
                    put("status", newStatus)
                }
                db.update("loans", upCv, "id = ?", arrayOf(loanId.toString()))

                // 3. Cash In to Cash Book
                val txnCv = ContentValues().apply {
                    put("txn_date", paymentDate)
                    put("txn_type", "Cash In")
                    put("category", "Loan Repayment")
                    put("reference", "Loan $lId Repayment ($mId) - P: ₹$principalComponent, I: ₹$interestComponent")
                    put("amount", totalAmount)
                }
                db.insert("cash_transactions", null, txnCv)

                logAudit(performedBy, "LOAN_REPAYMENT", "Loan", "$lId: ₹$totalAmount (P: ₹$principalComponent, I: ₹$interestComponent)")
            }
        }
        notifyDataChanged()
    }

    // ----------------------------------------------------
    // EXPENSES & CASH BOOK
    // ----------------------------------------------------
    suspend fun getExpenses(): List<Expense> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Expense>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, expense_date, category, description, amount FROM expenses ORDER BY expense_date DESC, id DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    Expense(
                        id = c.getLong(0),
                        expenseDate = c.getString(1),
                        category = c.getString(2),
                        description = c.getString(3) ?: "",
                        amount = c.getDouble(4)
                    )
                )
            }
        }
        list
    }

    suspend fun addExpense(expense: Expense, performedBy: String = "admin"): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("expense_date", expense.expenseDate)
            put("category", expense.category)
            put("description", expense.description)
            put("amount", expense.amount)
        }
        val id = db.insert("expenses", null, cv)

        // Cash Book: Cash Out
        val txnCv = ContentValues().apply {
            put("txn_date", expense.expenseDate)
            put("txn_type", "Cash Out")
            put("category", expense.category)
            put("reference", expense.description)
            put("amount", expense.amount)
        }
        db.insert("cash_transactions", null, txnCv)

        logAudit(performedBy, "EXPENSE_RECORDED", "Expense", "${expense.category}: ₹${expense.amount} (${expense.description})")
        notifyDataChanged()
        id
    }

    suspend fun getCashTransactions(): List<CashTransaction> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CashTransaction>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, txn_date, txn_type, category, reference, amount FROM cash_transactions ORDER BY txn_date ASC, id ASC", null)

        var runningBalance = 0.0
        cursor.use { c ->
            while (c.moveToNext()) {
                val type = c.getString(2)
                val amount = c.getDouble(5)
                if (type == "Cash In") {
                    runningBalance += amount
                } else {
                    runningBalance -= amount
                }
                list.add(
                    CashTransaction(
                        id = c.getLong(0),
                        txnDate = c.getString(1),
                        txnType = type,
                        category = c.getString(3) ?: "",
                        reference = c.getString(4) ?: "",
                        amount = amount,
                        balance = runningBalance
                    )
                )
            }
        }
        list.reversed() // Most recent first for display
    }

    suspend fun addCashTransaction(
        date: String,
        type: String,
        category: String,
        reference: String,
        amount: Double,
        performedBy: String = "admin"
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("txn_date", date)
            put("txn_type", type)
            put("category", category)
            put("reference", reference)
            put("amount", amount)
        }
        db.insert("cash_transactions", null, cv)
        logAudit(performedBy, "CASH_TRANSACTION", "CashBook", "$type: ₹$amount - $reference")
        notifyDataChanged()
    }

    // ----------------------------------------------------
    // AUDIT LOG
    // ----------------------------------------------------
    suspend fun getAuditLogs(): List<AuditLog> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AuditLog>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, user, action, entity, entity_id, created_at FROM audit_log ORDER BY id DESC LIMIT 100", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    AuditLog(
                        id = c.getLong(0),
                        user = c.getString(1),
                        action = c.getString(2),
                        entity = c.getString(3),
                        entityId = c.getString(4) ?: "",
                        createdAt = c.getString(5)
                    )
                )
            }
        }
        list
    }

    private fun logAudit(user: String, action: String, entity: String, entityId: String) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("user", user)
            put("action", action)
            put("entity", entity)
            put("entity_id", entityId)
            put("created_at", currentDateTimeString())
        }
        db.insert("audit_log", null, cv)
    }

    // ----------------------------------------------------
    // SETTINGS
    // ----------------------------------------------------
    suspend fun getSettings(): CommitteeSettings = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT key, value FROM settings", null)
        var name = "THE HELPING HANDS"
        var address = "Kanghatti Dist. Mandsaur (M.P.)"
        var dueDay = 5
        var penalty = 50.0
        var rate = 0.009
        var total = 30

        cursor.use { c ->
            while (c.moveToNext()) {
                when (c.getString(0)) {
                    "committee_name" -> name = c.getString(1)
                    "address" -> address = c.getString(1)
                    "due_day" -> dueDay = c.getString(1).toIntOrNull() ?: 5
                    "late_penalty" -> penalty = c.getString(1).toDoubleOrNull() ?: 50.0
                    "monthly_interest_rate" -> rate = c.getString(1).toDoubleOrNull() ?: 0.009
                    "total_members" -> total = c.getString(1).toIntOrNull() ?: 30
                }
            }
        }
        CommitteeSettings(name, address, dueDay, penalty, rate, total)
    }

    suspend fun updateSettings(settings: CommitteeSettings, performedBy: String = "admin") = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val map = mapOf(
            "committee_name" to settings.committeeName,
            "address" to settings.address,
            "due_day" to settings.dueDay.toString(),
            "late_penalty" to settings.latePenalty.toString(),
            "monthly_interest_rate" to settings.monthlyInterestRate.toString(),
            "total_members" to settings.totalMembersTarget.toString()
        )
        map.forEach { (k, v) ->
            val cv = ContentValues().apply {
                put("key", k)
                put("value", v)
            }
            db.insertWithOnConflict("settings", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        }
        logAudit(performedBy, "SETTINGS_UPDATED", "Settings", "${settings.committeeName} rules updated")
        notifyDataChanged()
    }

    // ----------------------------------------------------
    // DASHBOARD & FINANCIAL SUMMARIES
    // ----------------------------------------------------
    suspend fun getDashboardSummary(): DashboardSummary = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase

        // Total & Active members
        var totalMembers = 0
        var activeMembers = 0
        db.rawQuery("SELECT COUNT(*), SUM(CASE WHEN status = 'Active' THEN 1 ELSE 0 END) FROM members", null).use { c ->
            if (c.moveToNext()) {
                totalMembers = c.getInt(0)
                activeMembers = c.getInt(1)
            }
        }

        // Current month contribution rate (check current month)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-12
        val currentRate = if (currentMonth >= 8) 600.0 else 500.0

        // Total contributions collected
        var totalContributions = 0.0
        db.rawQuery("SELECT SUM(paid_amount + penalty) FROM monthly_contributions", null).use { c ->
            if (c.moveToNext()) totalContributions = c.getDouble(0)
        }

        // Loans: Disbursed, Principal Repaid, Outstanding, Interest
        var totalDisbursed = 0.0
        var totalPaidPrincipal = 0.0
        var totalPaidInterest = 0.0
        db.rawQuery("SELECT SUM(principal), SUM(total_paid_principal), SUM(total_paid_interest) FROM loans", null).use { c ->
            if (c.moveToNext()) {
                totalDisbursed = c.getDouble(0)
                totalPaidPrincipal = c.getDouble(1)
                totalPaidInterest = c.getDouble(2)
            }
        }
        val outstanding = (totalDisbursed - totalPaidPrincipal).coerceAtLeast(0.0)

        // Expenses
        var totalExpenses = 0.0
        db.rawQuery("SELECT SUM(amount) FROM expenses", null).use { c ->
            if (c.moveToNext()) totalExpenses = c.getDouble(0)
        }

        // Cash balance from cash transactions
        var cashIn = 0.0
        var cashOut = 0.0
        db.rawQuery("SELECT SUM(CASE WHEN txn_type = 'Cash In' THEN amount ELSE 0 END), SUM(CASE WHEN txn_type = 'Cash Out' THEN amount ELSE 0 END) FROM cash_transactions", null).use { c ->
            if (c.moveToNext()) {
                cashIn = c.getDouble(0)
                cashOut = c.getDouble(1)
            }
        }
        val cashBalance = cashIn - cashOut

        DashboardSummary(
            totalMembers = totalMembers,
            activeMembers = activeMembers,
            currentMonthContributionRate = currentRate,
            totalContributionsCollected = totalContributions,
            totalLoansDisbursed = totalDisbursed,
            totalLoanPrincipalPaid = totalPaidPrincipal,
            totalOutstandingLoan = outstanding,
            totalInterestEarned = totalPaidInterest,
            totalExpenses = totalExpenses,
            cashBalance = cashBalance
        )
    }

    suspend fun getAnnualSummary(): List<MonthlyFinancialSummary> = withContext(Dispatchers.IO) {
        val months = listOf(
            "January 2026", "February 2026", "March 2026", "April 2026",
            "May 2026", "June 2026", "July 2026", "August 2026",
            "September 2026", "October 2026", "November 2026", "December 2026"
        )
        val list = mutableListOf<MonthlyFinancialSummary>()
        val db = dbHelper.readableDatabase

        months.forEachIndexed { idx, mName ->
            val rate = if (idx >= 7) 600.0 else 500.0

            var expAmt = 0.0
            var collAmt = 0.0
            var penAmt = 0.0
            val queryCont = "SELECT SUM(applicable_amount), SUM(paid_amount), SUM(penalty) FROM monthly_contributions WHERE month_year = ?"
            db.rawQuery(queryCont, arrayOf(mName)).use { c ->
                if (c.moveToNext()) {
                    expAmt = c.getDouble(0)
                    collAmt = c.getDouble(1)
                    penAmt = c.getDouble(2)
                }
            }

            // Estimate / calculate loans disbursed, repayments, expenses for that month
            val monthStr = "2026-%02d".format(idx + 1)
            var disbursed = 0.0
            db.rawQuery("SELECT SUM(principal) FROM loans WHERE start_date LIKE '$monthStr%'", null).use { c ->
                if (c.moveToNext()) disbursed = c.getDouble(0)
            }

            var principalRepaid = 0.0
            var interestEarned = 0.0
            db.rawQuery("SELECT SUM(principal_component), SUM(interest_component) FROM loan_payments WHERE payment_date LIKE '$monthStr%'", null).use { c ->
                if (c.moveToNext()) {
                    principalRepaid = c.getDouble(0)
                    interestEarned = c.getDouble(1)
                }
            }

            var expenses = 0.0
            db.rawQuery("SELECT SUM(amount) FROM expenses WHERE expense_date LIKE '$monthStr%'", null).use { c ->
                if (c.moveToNext()) expenses = c.getDouble(0)
            }

            val netInflow = (collAmt + penAmt + principalRepaid + interestEarned) - (disbursed + expenses)

            list.add(
                MonthlyFinancialSummary(
                    monthIndex = idx + 1,
                    monthName = mName,
                    applicableRate = rate,
                    expectedContribution = expAmt,
                    collectedContribution = collAmt,
                    penaltiesCollected = penAmt,
                    loansDisbursed = disbursed,
                    principalRepaid = principalRepaid,
                    interestEarned = interestEarned,
                    expenses = expenses,
                    netCashFlow = netInflow
                )
            )
        }
        list
    }

    suspend fun resetToDefaults(performedBy: String = "admin") = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        dbHelper.onUpgrade(db, 1, 1)
        logAudit(performedBy, "DATABASE_RESET", "System", "All tables reset to clean initial seeds")
        notifyDataChanged()
    }
}
