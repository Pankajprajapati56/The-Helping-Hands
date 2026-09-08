package com.helpinghands.app.data.model

data class Member(
    val id: Long = 0,
    val memberId: String,
    val name: String,
    val fatherHusbandName: String = "",
    val mobile: String = "",
    val joiningDate: String = "2026-01-01",
    val nominee: String = "",
    val occupation: String = "",
    val status: String = "Active"
)

data class ContributionRate(
    val id: Long = 0,
    val effectiveFrom: String, // e.g. "2026-01-01", "2026-08-01"
    val amount: Double,        // ₹500, ₹600
    val notes: String = ""
)

data class MonthlyContribution(
    val id: Long = 0,
    val memberId: String,
    val memberName: String = "",
    val monthYear: String,       // e.g. "2026-01" or "January 2026"
    val applicableAmount: Double,
    val paidAmount: Double = 0.0,
    val paymentDate: String = "",
    val penalty: Double = 0.0,
    val status: String = "PENDING", // "PAID", "PENDING"
    val remarks: String = ""
) {
    val totalAmount: Double get() = paidAmount + penalty
}

data class Loan(
    val id: Long = 0,
    val loanId: String,
    val memberId: String,
    val memberName: String = "",
    val principal: Double,
    val interestRateMonthly: Double = 0.009, // 0.90%
    val startDate: String,
    val termMonths: Int = 12,
    val emi: Double,
    val totalPaidPrincipal: Double = 0.0,
    val totalPaidInterest: Double = 0.0,
    val status: String = "ACTIVE", // "ACTIVE", "CLOSED"
    val purpose: String = ""
) {
    val outstandingPrincipal: Double get() = (principal - totalPaidPrincipal).coerceAtLeast(0.0)
}

data class LoanPayment(
    val id: Long = 0,
    val loanId: Long,
    val paymentDate: String,
    val amount: Double,
    val interestComponent: Double = 0.0,
    val principalComponent: Double = 0.0,
    val notes: String = ""
)

data class LoanScheduleRow(
    val monthIndex: Int,
    val monthLabel: String,
    val openingBalance: Double,
    val emi: Double,
    val interestComponent: Double,
    val principalComponent: Double,
    val closingBalance: Double
)

data class Expense(
    val id: Long = 0,
    val expenseDate: String,
    val category: String,
    val description: String,
    val amount: Double
)

data class CashTransaction(
    val id: Long = 0,
    val txnDate: String,
    val txnType: String, // "Cash In" or "Cash Out"
    val category: String,
    val reference: String,
    val amount: Double,
    val balance: Double = 0.0
)

data class AuditLog(
    val id: Long = 0,
    val user: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val createdAt: String
)

data class CommitteeSettings(
    val committeeName: String = "THE HELPING HANDS",
    val address: String = "Kanghatti Dist. Mandsaur (M.P.)",
    val dueDay: Int = 5,
    val latePenalty: Double = 50.0,
    val monthlyInterestRate: Double = 0.009,
    val totalMembersTarget: Int = 30
)

data class AppUser(
    val username: String,
    val role: String, // "ADMIN" or "MEMBER"
    val memberId: String? = null,
    val displayName: String
)
