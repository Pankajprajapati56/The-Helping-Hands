package com.helpinghands.app.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.helpinghands.app.data.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "helping_hands.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                member_id TEXT UNIQUE,
                name TEXT NOT NULL,
                father_husband_name TEXT,
                mobile TEXT,
                joining_date TEXT,
                nominee TEXT,
                occupation TEXT,
                status TEXT DEFAULT 'Active',
                active INTEGER DEFAULT 1
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE contribution_rates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                effective_from TEXT NOT NULL,
                amount REAL NOT NULL,
                notes TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE monthly_contributions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                member_id TEXT NOT NULL,
                month_year TEXT NOT NULL,
                applicable_amount REAL NOT NULL,
                paid_amount REAL DEFAULT 0,
                payment_date TEXT,
                penalty REAL DEFAULT 0,
                status TEXT DEFAULT 'PENDING',
                remarks TEXT,
                UNIQUE(member_id, month_year)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE loans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                loan_id TEXT UNIQUE NOT NULL,
                member_id TEXT NOT NULL,
                principal REAL NOT NULL,
                interest_rate_monthly REAL NOT NULL,
                start_date TEXT NOT NULL,
                term_months INTEGER NOT NULL,
                emi REAL NOT NULL,
                total_paid_principal REAL DEFAULT 0,
                total_paid_interest REAL DEFAULT 0,
                status TEXT DEFAULT 'ACTIVE',
                purpose TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE loan_payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                loan_id INTEGER NOT NULL,
                payment_date TEXT NOT NULL,
                amount REAL NOT NULL,
                interest_component REAL DEFAULT 0,
                principal_component REAL DEFAULT 0,
                notes TEXT
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                expense_date TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT,
                amount REAL NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE cash_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                txn_date TEXT NOT NULL,
                txn_type TEXT NOT NULL,
                category TEXT NOT NULL,
                reference TEXT,
                amount REAL NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE audit_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user TEXT NOT NULL,
                action TEXT NOT NULL,
                entity TEXT NOT NULL,
                entity_id TEXT,
                created_at TEXT NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """.trimIndent())

        seedInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS settings")
        db.execSQL("DROP TABLE IF EXISTS audit_log")
        db.execSQL("DROP TABLE IF EXISTS cash_transactions")
        db.execSQL("DROP TABLE IF EXISTS expenses")
        db.execSQL("DROP TABLE IF EXISTS loan_payments")
        db.execSQL("DROP TABLE IF EXISTS loans")
        db.execSQL("DROP TABLE IF EXISTS monthly_contributions")
        db.execSQL("DROP TABLE IF EXISTS contribution_rates")
        db.execSQL("DROP TABLE IF EXISTS members")
        onCreate(db)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        // 1. Settings
        val settingsMap = mapOf(
            "committee_name" to "THE HELPING HANDS",
            "address" to "Kanghatti Dist. Mandsaur (M.P.)",
            "due_day" to "5",
            "late_penalty" to "50",
            "monthly_interest_rate" to "0.009",
            "total_members" to "30"
        )
        settingsMap.forEach { (k, v) ->
            val cv = ContentValues().apply {
                put("key", k)
                put("value", v)
            }
            db.insert("settings", null, cv)
        }

        // 2. Contribution Rates as per APP_SPEC rule:
        // Jan - Jul 2026: 500
        // Aug - Dec 2026: 600
        db.insert("contribution_rates", null, ContentValues().apply {
            put("effective_from", "2026-01-01")
            put("amount", 500.0)
            put("notes", "Initial monthly contribution (Jan–Jul 2026)")
        })
        db.insert("contribution_rates", null, ContentValues().apply {
            put("effective_from", "2026-08-01")
            put("amount", 600.0)
            put("notes", "Updated monthly contribution (Aug–Dec 2026)")
        })

        // 3. 30 Registered Members (Kanghatti, Mandsaur community)
        val initialMembers = listOf(
            Triple("THH001", "Pankaj Prajapat", "Rameshwar Prajapat"),
            Triple("THH002", "Suresh Kumar Sharma", "Gopal Sharma"),
            Triple("THH003", "Dinesh Patidar", "Ramnarayan Patidar"),
            Triple("THH004", "Mohanlal Rathore", "Bherulal Rathore"),
            Triple("THH005", "Rajesh Kumar Jain", "Shantilal Jain"),
            Triple("THH006", "Kamlesh Prajapati", "Bhagwandas Prajapati"),
            Triple("THH007", "Mukesh Malviya", "Kanhaiyalal Malviya"),
            Triple("THH008", "Jagdish Chandra Joshi", "Madhusudan Joshi"),
            Triple("THH009", "Vijay Singh Rajput", "Balwant Singh Rajput"),
            Triple("THH010", "Ghanshyam Porwal", "Babulal Porwal"),
            Triple("THH011", "Kailash Chand Verma", "Ramkaran Verma"),
            Triple("THH012", "Santosh Kumar Gupta", "Devkishan Gupta"),
            Triple("THH013", "Narendra Singh Sisodiya", "Dileep Singh Sisodiya"),
            Triple("THH014", "Deepak Chouhan", "Radheshyam Chouhan"),
            Triple("THH015", "Manish Patidar", "Omprakash Patidar"),
            Triple("THH016", "Anil Kumar Soni", "Shyamlal Soni"),
            Triple("THH017", "Mahesh Chandra Sharma", "Kishorilal Sharma"),
            Triple("THH018", "Vinod Prajapat", "Mangilal Prajapat"),
            Triple("THH019", "Rakesh Solanki", "Govind Solanki"),
            Triple("THH020", "Pramod Dhakar", "Bhagwati Prasad Dhakar"),
            Triple("THH021", "Govind Ram Nayak", "Jagram Nayak"),
            Triple("THH022", "Babulal Meena", "Hariram Meena"),
            Triple("THH023", "Shyam Sundar Vyas", "Girdharilal Vyas"),
            Triple("THH024", "Ashok Kumar Sen", "Mangi Sen"),
            Triple("THH025", "Hemant Kumar Tiwari", "Durgesh Tiwari"),
            Triple("THH026", "Bharat Singh Tomar", "Shivpal Singh Tomar"),
            Triple("THH027", "Sunil Kumar Parihar", "Babulal Parihar"),
            Triple("THH028", "Arjun Lal Mali", "Chhogalal Mali"),
            Triple("THH029", "Pappu Lal Prajapati", "Nandram Prajapati"),
            Triple("THH030", "Kanhaiya Lal Rawat", "Jagannath Rawat")
        )

        initialMembers.forEachIndexed { index, (mId, name, fName) ->
            val cv = ContentValues().apply {
                put("member_id", mId)
                put("name", name)
                put("father_husband_name", fName)
                put("mobile", "98" + (26000000 + index * 12345).toString().take(8))
                put("joining_date", "2026-01-01")
                put("nominee", "Family Nominee")
                put("occupation", if (index % 3 == 0) "Agriculture" else if (index % 3 == 1) "Business" else "Service")
                put("status", "Active")
                put("active", 1)
            }
            db.insert("members", null, cv)
        }

        // 4. Months for 2026
        val months = listOf(
            "January 2026", "February 2026", "March 2026", "April 2026",
            "May 2026", "June 2026", "July 2026", "August 2026",
            "September 2026", "October 2026", "November 2026", "December 2026"
        )

        // Seed contributions: For Jan-Aug 2026, seed members as paid with realistic payment dates
        months.forEachIndexed { mIdx, mName ->
            val applicable = if (mIdx >= 7) 600.0 else 500.0 // Month 8 (Aug) onwards is 600
            initialMembers.forEachIndexed { memIdx, (mId, _, _) ->
                val cv = ContentValues().apply {
                    put("member_id", mId)
                    put("month_year", mName)
                    put("applicable_amount", applicable)
                    // Mark Jan, Feb, Mar as paid, others as pending or partially paid
                    if (mIdx < 2 || (mIdx == 2 && memIdx < 22)) {
                        val payDay = if (memIdx % 7 == 0) 8 else 4 // some paid after 5th
                        val penalty = if (payDay > 5) 50.0 else 0.0
                        put("paid_amount", applicable)
                        put("payment_date", "2026-%02d-%02d".format(mIdx + 1, payDay))
                        put("penalty", penalty)
                        put("status", "PAID")
                        put("remarks", if (penalty > 0) "Paid with ₹50 late fee" else "On-time contribution")
                    } else {
                        put("paid_amount", 0.0)
                        put("payment_date", "")
                        put("penalty", 0.0)
                        put("status", "PENDING")
                        put("remarks", "")
                    }
                }
                db.insert("monthly_contributions", null, cv)
            }
        }

        // 5. Seed Loans & Repayments with Reducing Balance Method
        // Loan 1: THH003 Dinesh Patidar - ₹30,000 for 12 months @ 0.90% monthly
        val p1 = 30000.0
        val r1 = 0.009
        val n1 = 12
        val emi1 = (p1 * r1 * (1 + r1).pow(n1)) / ((1 + r1).pow(n1) - 1)
        val l1Id = db.insert("loans", null, ContentValues().apply {
            put("loan_id", "LN001")
            put("member_id", "THH003")
            put("principal", p1)
            put("interest_rate_monthly", r1)
            put("start_date", "2026-01-10")
            put("term_months", n1)
            put("emi", Math.round(emi1 * 100.0) / 100.0)
            put("total_paid_principal", 4800.0)
            put("total_paid_interest", 530.0)
            put("status", "ACTIVE")
            put("purpose", "Agricultural equipment purchase")
        })

        // Loan 2: THH007 Mukesh Malviya - ₹20,000 for 10 months @ 0.90% monthly
        val p2 = 20000.0
        val r2 = 0.009
        val n2 = 10
        val emi2 = (p2 * r2 * (1 + r2).pow(n2)) / ((1 + r2).pow(n2) - 1)
        db.insert("loans", null, ContentValues().apply {
            put("loan_id", "LN002")
            put("member_id", "THH007")
            put("principal", p2)
            put("interest_rate_monthly", r2)
            put("start_date", "2026-02-05")
            put("term_months", n2)
            put("emi", Math.round(emi2 * 100.0) / 100.0)
            put("total_paid_principal", 1900.0)
            put("total_paid_interest", 180.0)
            put("status", "ACTIVE")
            put("purpose", "Small shop expansion")
        })

        // 6. Expenses
        val sampleExpenses = listOf(
            Expense(expenseDate = "2026-01-05", category = "Stationery", description = "Register, Passbooks and receipt books", amount = 1200.0),
            Expense(expenseDate = "2026-01-26", category = "Meeting/Refreshment", description = "Republic Day annual committee gathering tea & snacks", amount = 850.0),
            Expense(expenseDate = "2026-02-10", category = "Office Expense", description = "Digital record printing and stamp pad", amount = 450.0),
            Expense(expenseDate = "2026-03-01", category = "Welfare/Event", description = "Member welfare medical support fund", amount = 2000.0)
        )
        sampleExpenses.forEach { exp ->
            db.insert("expenses", null, ContentValues().apply {
                put("expense_date", exp.expenseDate)
                put("category", exp.category)
                put("description", exp.description)
                put("amount", exp.amount)
            })
        }

        // 7. Cash Book Transactions
        val cashTxns = listOf(
            Pair("2026-01-01", Triple("Cash In", "Opening Committee Capital Balance", 45000.0)),
            Pair("2026-01-05", Triple("Cash In", "January Contributions (30 members)", 15050.0)),
            Pair("2026-01-05", Triple("Cash Out", "Stationery & Passbooks", 1200.0)),
            Pair("2026-01-10", Triple("Cash Out", "Loan LN001 Disbursal to THH003", 30000.0)),
            Pair("2026-01-26", Triple("Cash Out", "Republic Day Meeting Refreshment", 850.0)),
            Pair("2026-02-05", Triple("Cash In", "February Contributions (30 members)", 15100.0)),
            Pair("2026-02-05", Triple("Cash Out", "Loan LN002 Disbursal to THH007", 20000.0)),
            Pair("2026-02-10", Triple("Cash In", "LN001 EMI Repayment Month 1", 2650.0)),
            Pair("2026-02-10", Triple("Cash Out", "Office Supplies", 450.0)),
            Pair("2026-03-05", Triple("Cash In", "March Contributions (22 members)", 11000.0)),
            Pair("2026-03-10", Triple("Cash In", "LN001 & LN002 EMI Repayments", 4750.0))
        )

        cashTxns.forEach { (date, triple) ->
            val (type, desc, amt) = triple
            db.insert("cash_transactions", null, ContentValues().apply {
                put("txn_date", date)
                put("txn_type", type)
                put("category", if (type == "Cash In") "Inflow" else "Expense/Disbursal")
                put("reference", desc)
                put("amount", amt)
            })
        }

        // 8. Audit Log
        val initialLogs = listOf(
            AuditLog(user = "admin", action = "SYSTEM_INIT", entity = "System", entityId = "2026", createdAt = "2026-01-01 10:00:00"),
            AuditLog(user = "admin", action = "MEMBER_BATCH_IMPORT", entity = "Members", entityId = "30 records", createdAt = "2026-01-01 10:05:00"),
            AuditLog(user = "admin", action = "RATE_CONFIG", entity = "ContributionRates", entityId = "₹500 (Jan-Jul), ₹600 (Aug-Dec)", createdAt = "2026-01-01 10:10:00"),
            AuditLog(user = "admin", action = "LOAN_APPROVED", entity = "Loans", entityId = "LN001 - ₹30,000", createdAt = "2026-01-10 14:30:00")
        )
        initialLogs.forEach { log ->
            db.insert("audit_log", null, ContentValues().apply {
                put("user", log.user)
                put("action", log.action)
                put("entity", log.entity)
                put("entity_id", log.entityId)
                put("created_at", log.createdAt)
            })
        }
    }
}
