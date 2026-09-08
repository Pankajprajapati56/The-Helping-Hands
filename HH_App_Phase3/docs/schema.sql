CREATE TABLE users(id INTEGER PRIMARY KEY, member_id TEXT UNIQUE, role TEXT, password_hash TEXT, active INTEGER DEFAULT 1);
CREATE TABLE members(id INTEGER PRIMARY KEY, member_id TEXT UNIQUE, name TEXT, mobile TEXT, joining_date TEXT, active INTEGER DEFAULT 1);
CREATE TABLE contribution_rates(id INTEGER PRIMARY KEY, effective_from TEXT, amount REAL, notes TEXT);
CREATE TABLE monthly_contributions(id INTEGER PRIMARY KEY, member_id TEXT, month TEXT, applicable_amount REAL, paid_amount REAL DEFAULT 0, payment_date TEXT, penalty REAL DEFAULT 0, status TEXT DEFAULT 'PENDING', UNIQUE(member_id,month));
CREATE TABLE loans(id INTEGER PRIMARY KEY, member_id TEXT, principal REAL, interest_rate_monthly REAL, start_date TEXT, term_months INTEGER, emi REAL, status TEXT DEFAULT 'ACTIVE');
CREATE TABLE loan_payments(id INTEGER PRIMARY KEY, loan_id INTEGER, payment_date TEXT, amount REAL, interest_component REAL DEFAULT 0, principal_component REAL DEFAULT 0);
CREATE TABLE expenses(id INTEGER PRIMARY KEY, expense_date TEXT, category TEXT, description TEXT, amount REAL);
CREATE TABLE cash_transactions(id INTEGER PRIMARY KEY, txn_date TEXT, txn_type TEXT, reference TEXT, amount REAL);
CREATE TABLE audit_log(id INTEGER PRIMARY KEY, user_id INTEGER, action TEXT, entity TEXT, entity_id TEXT, created_at TEXT);
