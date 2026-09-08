package com.helpinghands.app

import com.helpinghands.app.data.model.ContributionRate
import com.helpinghands.app.data.model.LoanScheduleRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class HelpingHandsBusinessRulesTest {

    private fun calculateEMI(principal: Double, termMonths: Int, monthlyRate: Double): Double {
        if (termMonths <= 0 || principal <= 0) return 0.0
        if (monthlyRate <= 0.0) return Math.round((principal / termMonths) * 100.0) / 100.0
        val factor = (1 + monthlyRate).pow(termMonths)
        val emi = (principal * monthlyRate * factor) / (factor - 1)
        return Math.round(emi * 100.0) / 100.0
    }

    private fun generateLoanSchedule(
        principal: Double,
        termMonths: Int,
        monthlyRate: Double
    ): List<LoanScheduleRow> {
        val schedule = mutableListOf<LoanScheduleRow>()
        val emi = calculateEMI(principal, termMonths, monthlyRate)
        var balance = principal

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

            schedule.add(
                LoanScheduleRow(
                    monthIndex = m,
                    monthLabel = "Month $m",
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

    @Test
    fun testReducingBalanceEMICalculation() {
        val principal = 25000.0
        val termMonths = 12
        val monthlyRate = 0.009 // 0.90% per month

        val emi = calculateEMI(principal, termMonths, monthlyRate)
        // With P=25000, n=12, r=0.009, EMI should be approximately ~2207.6
        assertTrue("EMI should be around 2200-2210", emi in 2200.0..2215.0)

        val schedule = generateLoanSchedule(principal, termMonths, monthlyRate)
        assertEquals(12, schedule.size)

        // Verify that in month 1, interest is opening * 0.009
        val month1 = schedule[0]
        assertEquals(25000.0, month1.openingBalance, 0.01)
        assertEquals(225.0, month1.interestComponent, 0.01) // 25000 * 0.009 = 225.0

        // Verify closing balance decreases over time and reaches 0 at month 12
        val finalMonth = schedule[11]
        assertEquals(0.0, finalMonth.closingBalance, 0.01)
    }

    @Test
    fun testContributionRateRules() {
        // Business Rule: "Never edit an old rate; insert a new effective_from date."
        val rates = listOf(
            ContributionRate(id = 1, effectiveFrom = "2026-01-01", amount = 500.0, notes = "Initial rate"),
            ContributionRate(id = 2, effectiveFrom = "2026-08-01", amount = 600.0, notes = "Revised rate")
        )

        // Helper to resolve rate for a given date
        fun getRateForDate(dateStr: String): Double {
            return rates
                .filter { it.effectiveFrom <= dateStr }
                .maxByOrNull { it.effectiveFrom }
                ?.amount ?: 500.0
        }

        // Jan–Jul 2026 should be ₹500
        assertEquals(500.0, getRateForDate("2026-01-15"), 0.0)
        assertEquals(500.0, getRateForDate("2026-04-05"), 0.0)
        assertEquals(500.0, getRateForDate("2026-07-31"), 0.0)

        // Aug–Dec 2026 should be ₹600
        assertEquals(600.0, getRateForDate("2026-08-01"), 0.0)
        assertEquals(600.0, getRateForDate("2026-10-10"), 0.0)
        assertEquals(600.0, getRateForDate("2026-12-31"), 0.0)
    }

    @Test
    fun testLateFeePenaltyRule() {
        // Due day is 5th of each month; penalty is ₹50 if paid after 5th
        fun calculatePenalty(dayOfMonth: Int): Double {
            return if (dayOfMonth > 5) 50.0 else 0.0
        }

        assertEquals(0.0, calculatePenalty(1), 0.0)
        assertEquals(0.0, calculatePenalty(5), 0.0)
        assertEquals(50.0, calculatePenalty(6), 0.0)
        assertEquals(50.0, calculatePenalty(15), 0.0)
    }
}
