# Assignment 1: Black Box Testing

Team: Alan Cruz, Jared Jack Harrison, alessiap000, Julia Kasperek
Pull request: [#1](https://github.com/aacruzgon/WesternCS4472-ATM-Assignment/pull/1)

| Target | Technique | Owner | Test class |
|---|---|---|---|
| `atm.session.Session` (PIN format, withdrawal amount) | Robust Worst Case BVA, single fault assumption | alessiap000 | `src/test/java/atm/session/SessionTest.java` |
| `FeesCalculator.calculateWithdrawalFee` | Robust Worst Case BVA | Alan Cruz | `src/test/java/bank/utils/FeesCalculatorWithdrawalTest.java` |
| `FeesCalculator.calculateDepositInterest` | Weak Robust Equivalence Class Analysis | Jared Jack Harrison | `src/test/java/bank/utils/DepositFeesTest.java` |
| `FeesCalculator.calculateTransferFee` | Decision Table Analysis | Julia Kasperek | `src/test/java/bank/utils/TransferFeeTest.java` |

The withdrawal fee write-up is long (154 test cases), so it lives in its own file:
[`docs/assignment1/withdrawal.md`](assignment1/withdrawal.md). Its bugs are included in the team tables below.

---

## Section 1: Process and assumptions

### 1.1 Session: PIN format and withdrawal amount (Robust Worst Case BVA)

The Session target is tested through two public entry points: `ATM.checkPin` (which
delegates to `FormatChecker.checkPinFormat`) for the PIN format, and
`Session.setAmount` (which delegates to `CashValidator.validateWithdrawal`) for the
withdrawal amount. Only the withdrawal transaction (choice 1) is exercised, on an
account with a $1,000 daily limit and a $5,000 balance, as the assignment requires.

**Technique.** Robust Worst Case Boundary Value Analysis under the single-fault
assumption. For each input we take the seven robust boundary values
(min-, min, min+, nominal, max-, max, max+). "Worst case" means we cross the two
inputs; the single-fault assumption means a test carries at most one out-of-range
value, so the combinations with both an invalid PIN and an invalid amount are
dropped.

**Inputs and boundary values.**

| Variable | Valid range | min- | min | min+ | nom | max- | max | max+ |
|---|---|---|---|---|---|---|---|---|
| PIN | 00000-99999 (5 digits) | 0000 | 00000 | 00001 | 55555 | 99998 | 99999 | 100000 |
| Amount | $20-$1,000 | 10 | 20 | 40 | 500 | 990 | 1000 | 1010 |

Amounts move in $10 steps; the value after $20 is $40 because $30 cannot be formed
from $20 and $50 bills.

**Case count.** Crossing 7 PIN values with 7 amount values gives 49. Removing the
4 cells that have both an invalid PIN and an invalid amount leaves 45 cases:
25 good-PIN/good-amount, 10 good-PIN/bad-amount, and 10 bad-PIN/good-amount. Because
the ATM stops at a bad PIN before it ever reads an amount, the 10 bad-PIN/good-amount
cells reduce to 2 distinct tests (one per invalid PIN). We then add cases the
worst-case grid does not reach: PINs with non-digit characters and other wrong
lengths, amounts that are dispensable combinations of $20 and $50 bills, amounts
over the daily limit, and non-positive amounts. In total the suite runs 72 JUnit
cases.

**Assumptions.**

1. A valid PIN is exactly 5 digits, 00000-99999 (ATM Session spec). The PIN is
   checked by `FormatChecker`, reached through `ATM.checkPin`; `Session` itself does
   not validate the PIN, so the amount tests are unaffected by any PIN fault.
2. The withdrawal amount domain is $20 (smallest dispensable bill) to $1,000 (the
   account's daily limit, which the assignment fixes as the upper boundary). A valid
   amount must also be formable from $20 and $50 bills.
3. The account is a non-student with a $5,000 balance and a $1,000 daily limit, so
   the withdrawal itself carries no fee; the tests check acceptance/rejection and
   that the transaction is dispatched, not the fee value.
4. Irrelevant inputs are held at nominal: amount tests use PIN 55555 where the PIN is
   not the variable under test.
5. A rejected input must throw the matching exception (`InvalidPinFormatException` or
   `InvalidAmountException`) and must not contact the bank or dispatch a transaction;
   an accepted input must dispatch exactly one withdrawal.

### 1.2 Withdrawal fee (Robust Worst Case BVA)
See [docs/assignment1/withdrawal.md, Section 1](assignment1/withdrawal.md#section-1-process-and-assumptions).

### 1.3 Deposit fee (Weak Robust Equivalence Class Analysis)

The deposit fee tests target
`FeesCalculator.calculateDepositInterest(amount, accountBalance, student)`.
Although the method name uses the word `Interest`, expected results are derived
from the deposit *fee* specification.

Weak Robust Equivalence Class Analysis was applied as follows:

- The `student` input has two valid classes: student and non-student.
- For students, the amount classes are `amount > 50` and `amount <= 50`.
  The applicable balance classes are `balance > 500` and `balance <= 500`
  for the first amount class, and `balance > 5,000` and
  `balance <= 5,000` for the second.
- For non-students, the amount classes are `amount > 250` and
  `amount <= 250`. The applicable balance classes are `balance > 2,500`
  and `balance <= 2,500` for the first amount class, and
  `balance > 10,000` and `balance <= 10,000` for the second.
- One representative was selected for every valid combination that produces a
  distinct fee rule. This gives the minimum eight cases needed to exercise all
  specified deposit outcomes.
- For the robust part of the analysis, a negative deposit amount and a negative
  account balance are the invalid classes. Each is tested on its own while the
  other inputs stay valid, following the single-fault principle.
- The specification defines no error for out-of-range values, so the fee rules
  are applied literally: -$1.00 is "$50 or less" and a balance of -$1.00 is not
  "more than $500". Input validation is the responsibility of `Session`, which
  the specification gives `InvalidAmountException` and
  `InvalidPinFormatException`. This is the same assumption as A4 in the
  withdrawal section.
- Expected monetary values are compared using a floating-point tolerance of
  `0.000001`.

### 1.4 Transfer fee (Decision Table Analysis)

**Process.** The transfer specification is a set of nested yes/no decisions, so we modelled it as a
limited-entry decision table. We identified four conditions:

| ID | Condition |
|---|---|
| C1 | The client is a student |
| C2 | `amount` is below the amount threshold |
| C3 | `fromAccountBalance` is below the "from" threshold |
| C4 | `toAccountBalance` is below the "to" threshold |

Each condition is true or false, giving 2^4 = 16 rules. Every combination is reachable and the
specification gives a fee for each one, so no rules were merged or marked impossible. The action
for each rule is the fee rate taken from the specification. We wrote one test case per rule
(16 test cases), choosing input values well inside each condition's region, not on its boundary
(boundaries are what BVA tests, not decision tables).

**Thresholds.** The thresholds depend on which branch of the specification applies:

| Branch | Amount threshold (C2) | From threshold (C3) | To threshold (C4) |
|---|---|---|---|
| Student, amount < $200 | $200 | $2,000 | $1,000 |
| Student, amount >= $200 | $200 | $2,000 | $1,000 |
| Non-student, amount < $100 | $100 | **$4,000** | **$2,000** |
| Non-student, amount >= $100 | $100 | $2,000 | $1,000 |

**Assumptions.**

1. **Different thresholds for small non-student transfers.** The specification uses $4,000 / $2,000
   for non-student transfers under $100 but $2,000 / $1,000 for $100 or more. We assume this is
   intentional and test exactly what the specification says.
2. **"Otherwise" binds to the innermost condition.** In "if from < X and if to < Y then A, otherwise
   B", we read "otherwise" as "from < X and to >= Y". The case from >= X is covered separately by the
   following "If, however" clause.
3. **"Less than" and "or more".** "Less than $N" means `value < N`, and "$N or more" means
   `value >= N`, as written in the specification.
4. **Fee is returned in dollars.** `calculateTransferFee` returns `amount x rate` (for example,
   0.1% of $150 = $0.15). This matches the existing withdrawal test in the starter code, where
   0.1% of $200 returns 0.2.
5. **Valid inputs only.** Decision table testing checks the business rules, so all inputs are
   positive amounts and non-negative balances. Invalid inputs (negative amounts) are out of scope
   for this technique.
6. **Test value choice.** Where possible we picked values that fall between two thresholds the
   specification uses (for example a student amount of $150, between $100 and $200; a non-student
   "from" balance of $3,000, between $2,000 and $4,000). Such a value lands on the correct side only
   if the code uses the threshold the specification states for that branch.
7. **Floating point.** Expected and actual fees are compared with a tolerance of 0.000001, the same as the withdrawal and deposit tests.

---

## Section 2: Test cases

### 2.1 Session

The Expected, Actual and Status columns are the test results for this section (see
also Section 3.1).

#### 2.1.1 PIN format

| ID | PIN | Boundary / class | Expected | Actual | Status |
|---|---|---|---|---|---|
| SE-P-01 | 00000 | min (valid) | Accepted | InvalidPinFormatException | Fail |
| SE-P-02 | 00001 | min+ (valid) | Accepted | InvalidPinFormatException | Fail |
| SE-P-03 | 55555 | nominal (valid) | Accepted | InvalidPinFormatException | Fail |
| SE-P-04 | 99998 | max- (valid) | Accepted | InvalidPinFormatException | Fail |
| SE-P-05 | 99999 | max (valid) | Accepted | InvalidPinFormatException | Fail |
| SE-P-06 | 0000 | min- (4 digits, invalid) | Rejected | Accepted | Fail |
| SE-P-07 | 100000 | max+ (6 digits, invalid) | Rejected | Rejected | Pass |
| SE-P-08 | (empty) | wrong length | Rejected | Rejected | Pass |
| SE-P-09 | 5 | wrong length | Rejected | Rejected | Pass |
| SE-P-10 | 5555 | wrong length (4 digits) | Rejected | Accepted | Fail |
| SE-P-11 | 9999 | wrong length (4 digits) | Rejected | Rejected | Pass |
| SE-P-12 | 555555 | wrong length (6 digits) | Rejected | Rejected | Pass |
| SE-P-13 | 999999 | wrong length (6 digits) | Rejected | Rejected | Pass |
| SE-P-14 | /5555 | non-digit | Rejected | Rejected | Pass |
| SE-P-15 | 5555/ | non-digit | Rejected | Rejected | Pass |
| SE-P-16 | :5555 | non-digit | Rejected | Rejected | Pass |
| SE-P-17 | 5555: | non-digit | Rejected | Rejected | Pass |
| SE-P-18 | a5555 | non-digit | Rejected | Rejected | Pass |
| SE-P-19 | 5555a | non-digit | Rejected | Rejected | Pass |

SE-P-11 passes only by coincidence: the code also rejects the valid digit 9 (the
secondary bug in S-F1), which happens to yield the expected rejection for a 4-digit
PIN that should be rejected on length anyway.

#### 2.1.2 Withdrawal amount

Boundary rows SE-A-01 to SE-A-07 are each run with all five valid PINs (worst-case
cross product = 35 executions); the remaining rows use nominal PIN 55555. "Dispatched"
means the withdrawal was accepted and sent to the bank.

| ID | Amount | Boundary / class | Expected | Actual | Status |
|---|---|---|---|---|---|
| SE-A-01 | 20 | min (valid) | Dispatched | Dispatched | Pass |
| SE-A-02 | 40 | min+ (valid) | Dispatched | Dispatched | Pass |
| SE-A-03 | 500 | nominal (valid) | Dispatched | Dispatched | Pass |
| SE-A-04 | 990 | max- (valid) | Dispatched | Dispatched | Pass |
| SE-A-05 | 1000 | max / daily limit (valid) | Dispatched | Dispatched | Pass |
| SE-A-06 | 10 | min- (below $20) | InvalidAmountException | Rejected | Pass |
| SE-A-07 | 1010 | max+ (over daily limit) | InvalidAmountException | Rejected | Pass |
| SE-A-08 | 50 | dispensable (1x$50) | Dispatched | Dispatched | Pass |
| SE-A-09 | 60 | dispensable (3x$20) | Dispatched | InvalidAmountException | Fail |
| SE-A-10 | 70 | dispensable ($20+$50) | Dispatched | Dispatched | Pass |
| SE-A-11 | 80 | dispensable (4x$20) | Dispatched | InvalidAmountException | Fail |
| SE-A-12 | 110 | dispensable ($50+3x$20) | Dispatched | InvalidAmountException | Fail |
| SE-A-13 | 130 | dispensable | Dispatched | InvalidAmountException | Fail |
| SE-A-14 | 960 | dispensable | Dispatched | InvalidAmountException | Fail |
| SE-A-15 | 980 | dispensable | Dispatched | InvalidAmountException | Fail |
| SE-A-16 | 19 | not dispensable | InvalidAmountException | Rejected | Pass |
| SE-A-17 | 21 | not dispensable | InvalidAmountException | Rejected | Pass |
| SE-A-18 | 30 | not dispensable | InvalidAmountException | Rejected | Pass |
| SE-A-19 | 999 | not dispensable | InvalidAmountException | Rejected | Pass |
| SE-A-20 | 1001 | not dispensable | InvalidAmountException | Rejected | Pass |
| SE-A-21 | 1020 | over daily limit (dispensable) | InvalidAmountException | Dispatched | Fail |
| SE-A-22 | 1050 | over daily limit (dispensable) | InvalidAmountException | Dispatched | Fail |
| SE-A-23 | 0 | non-positive | InvalidAmountException | Dispatched | Fail |
| SE-A-24 | -20 | non-positive | InvalidAmountException | Dispatched | Fail |
| SE-A-25 | -50 | non-positive | InvalidAmountException | Dispatched | Fail |

SE-A-07 passes only by coincidence. $1,010 can be dispensed (48 x $20 + 1 x $50), so
the only correct reason to reject it is the $1,000 daily limit, which the code never
checks (S-F3). The code rejects it because its bill check (S-F2) wrongly treats $1,010
as undispensable, so one fault happens to hide the other on this input.

### 2.2 Withdrawal fee
See [docs/assignment1/withdrawal.md, Section 2](assignment1/withdrawal.md#section-2-test-cases).

### 2.3 Deposit fee

The Actual result and Status columns are the test results for this section.

| ID | Student | Amount ($) | Balance ($) | Equivalence class | Expected result | Actual result | Status |
|---|---:|---:|---:|---|---:|---:|---|
| DEP-WREC-01 | Yes | 100.00 | 1,000.00 | Amount > 50; balance > 500 | 0.50 | 0.20 | Fail |
| DEP-WREC-02 | Yes | 100.00 | 500.00 | Amount > 50; balance <= 500 | 0.25 | 0.20 | Fail |
| DEP-WREC-03 | Yes | 25.00 | 6,000.00 | Amount <= 50; balance > 5,000 | 0.125 | 0.125 | Pass |
| DEP-WREC-04 | Yes | 25.00 | 5,000.00 | Amount <= 50; balance <= 5,000 | 0.00 | 0.05 | Fail |
| DEP-WREC-05 | No | 500.00 | 3,000.00 | Amount > 250; balance > 2,500 | 4.00 | 0.00 | Fail |
| DEP-WREC-06 | No | 500.00 | 2,500.00 | Amount > 250; balance <= 2,500 | 2.00 | 0.00 | Fail |
| DEP-WREC-07 | No | 100.00 | 11,000.00 | Amount <= 250; balance > 10,000 | 0.00 | 0.50 | Fail |
| DEP-WREC-08 | No | 100.00 | 10,000.00 | Amount <= 250; balance <= 10,000 | 0.10 | 0.00 | Fail |
| DEP-WREC-09 | Yes | -1.00 | 1,000.00 | Invalid amount (< 0), so amount <= 50; balance <= 5,000 | 0.00 | -0.002 | Fail |
| DEP-WREC-10 | Yes | 100.00 | -1.00 | Invalid balance (< 0), so amount > 50; balance <= 500 | 0.25 | 0.20 | Fail |

### 2.4.1 Transfer fee: decision table

T = true, F = false.

| Rule | R1 | R2 | R3 | R4 | R5 | R6 | R7 | R8 | R9 | R10 | R11 | R12 | R13 | R14 | R15 | R16 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| C1 student | T | T | T | T | T | T | T | T | F | F | F | F | F | F | F | F |
| C2 amount < threshold | T | T | T | T | F | F | F | F | T | T | T | T | F | F | F | F |
| C3 from < threshold | T | T | F | F | T | T | F | F | T | T | F | F | T | T | F | F |
| C4 to < threshold | T | F | T | F | T | F | T | F | T | F | T | F | T | F | T | F |
| **Fee rate** | 0.1% | 0.05% | 0.05% | 0.025% | 0.05% | 0.025% | 0.25% | 0.125% | 0.2% | 0.1% | 1% | 0.5% | 0.2% | 0.1% | 0.5% | 0.25% |

### 2.4.2 Transfer fee: test cases

| Test | Rule | amount | from balance | to balance | student | Rate | Expected fee |
|---|---|---|---|---|---|---|---|
| TF-R1 | R1 | 150 | 1,000 | 500 | true | 0.1% | 0.15 |
| TF-R2 | R2 | 150 | 1,000 | 1,500 | true | 0.05% | 0.075 |
| TF-R3 | R3 | 150 | 3,000 | 500 | true | 0.05% | 0.075 |
| TF-R4 | R4 | 150 | 3,000 | 1,500 | true | 0.025% | 0.0375 |
| TF-R5 | R5 | 500 | 1,000 | 500 | true | 0.05% | 0.25 |
| TF-R6 | R6 | 500 | 1,000 | 1,500 | true | 0.025% | 0.125 |
| TF-R7 | R7 | 500 | 3,000 | 500 | true | 0.25% | 1.25 |
| TF-R8 | R8 | 500 | 3,000 | 1,500 | true | 0.125% | 0.625 |
| TF-R9 | R9 | 50 | 3,000 | 1,500 | false | 0.2% | 0.10 |
| TF-R10 | R10 | 50 | 3,000 | 2,500 | false | 0.1% | 0.05 |
| TF-R11 | R11 | 50 | 5,000 | 1,500 | false | 1% | 0.50 |
| TF-R12 | R12 | 50 | 5,000 | 2,500 | false | 0.5% | 0.25 |
| TF-R13 | R13 | 150 | 1,000 | 500 | false | 0.2% | 0.30 |
| TF-R14 | R14 | 150 | 1,000 | 1,500 | false | 0.1% | 0.15 |
| TF-R15 | R15 | 150 | 3,000 | 500 | false | 0.5% | 0.75 |
| TF-R16 | R16 | 150 | 3,000 | 1,500 | false | 0.25% | 0.375 |

---

## Section 3: Test results

| Target | Run | Passed | Failed |
|---|---|---|---|
| Session | 72 | 54 | 18 |
| Withdrawal fee | 154 | 70 | 84 |
| Deposit fee | 10 | 1 | 9 |
| Transfer fee | 16 | 0 | 16 |

### 3.1 Session

72 cases run, 54 passed, 18 failed. The pass/fail status of every case is in the
Status column of Section 2.1. The failures group into four faults:

| Fault | Failing cases | Count |
|---|---|---|
| S-F1 | SE-P-01 to SE-P-05, SE-P-06, SE-P-10 | 7 |
| S-F2 | SE-A-09, SE-A-11, SE-A-12, SE-A-13, SE-A-14, SE-A-15 | 6 |
| S-F3 | SE-A-21, SE-A-22 | 2 |
| S-F4 | SE-A-23, SE-A-24, SE-A-25 | 3 |

See Section 4 for the fault localization.

### 3.2 Withdrawal fee
See [docs/assignment1/withdrawal.md, Test results](assignment1/withdrawal.md#test-results).

### 3.3 Deposit fee
See the Actual result and Status columns in section 2.3.

### 3.4 Transfer fee

| Test | Expected | Actual | Result |
|---|---|---|---|
| TF-R1 | 0.15 | 3.75 | Fail |
| TF-R2 | 0.075 | 1.875 | Fail |
| TF-R3 | 0.075 | 3.75 | Fail |
| TF-R4 | 0.0375 | 1.875 | Fail |
| TF-R5 | 0.25 | 12.5 | Fail |
| TF-R6 | 0.125 | 6.25 | Fail |
| TF-R7 | 1.25 | 12.5 | Fail |
| TF-R8 | 0.625 | 6.25 | Fail |
| TF-R9 | 0.10 | 2.5 | Fail |
| TF-R10 | 0.05 | 2.5 | Fail |
| TF-R11 | 0.50 | 2.5 | Fail |
| TF-R12 | 0.25 | 2.5 | Fail |
| TF-R13 | 0.30 | 7.5 | Fail |
| TF-R14 | 0.15 | 8.25 | Fail |
| TF-R15 | 0.75 | 7.5 | Fail |
| TF-R16 | 0.375 | 8.25 | Fail |

16 run, 0 passed, 16 failed.

---

## Section 4: Fault localization

All faults are in `src/main/java/bank/utils/FeesCalculator.java` unless stated otherwise. Several
failing tests can expose the same fault, so they are grouped. One test can expose more than one
fault. Fault IDs start with S (session), W (withdrawal), D (deposit) or T (transfer).

| Fault | Failing tests | Location (method, line, statement) | Justification |
|---|---|---|---|
| S-F1 | SE-P-01 to SE-P-05, SE-P-06, SE-P-10 | `atm.utils.FormatChecker.checkPinFormat`, line 33: `if (pin.length == 4)` | The spec requires a 5-digit PIN, but the code only accepts length 4, so valid 5-digit PINs are rejected and invalid 4-digit PINs are accepted. (Line 35, `pin[i] >= '9'`, also wrongly rejects the digit 9, but is masked by the length check.) |
| S-F2 | SE-A-09, SE-A-11, SE-A-12, SE-A-13, SE-A-14, SE-A-15 | `atm.utils.CashValidator.validateWithdrawal`, lines 23-28: `amount % 50` then `% 20` | Amounts that can be dispensed with a mix of $20 and $50 bills (e.g. $60 = 3x$20) are wrongly rejected, because the check never tests combinations of the two denominations. |
| S-F3 | SE-A-21, SE-A-22 | `atm.session.Session.setAmount` / `atm.utils.CashValidator.validateWithdrawal`: no daily-limit check | The $1,000 daily limit is never enforced anywhere in the code, so withdrawals above the limit are accepted when the spec says the daily limit is the upper boundary. |
| S-F4 | SE-A-23, SE-A-24, SE-A-25 | `atm.utils.CashValidator.validateWithdrawal`: no lower-bound check | Zero and negative amounts pass validation (e.g. `0 % 50 == 0`), so they are accepted instead of rejected as invalid amounts. |
| W-F1 | 35 student weekday/weekend tests (WD-S-02 ... WD-S-48, full list in [withdrawal.md](assignment1/withdrawal.md#fault-localization)) | `calculateWithdrawalFee`, line 12: `if ((dayOfWeek == Calendar.SATURDAY) \|\| (dayOfWeek == Calendar.SUNDAY))` | Every student weekday case expected no fee but was charged 0.1%, and every weekend case expected 0.1% but was charged nothing, so the weekday/weekend branches are swapped. |
| W-F2 | 35 non-student tests WD-N-001 to WD-N-035 | `calculateWithdrawalFee`, line 19: `feePercentage = 0.002;` | Every non-student case with balance below $1,000 returned 0.2% of the amount instead of the specified 0.3%, so the rate constant is wrong. |
| W-F3 | 14 non-student tests WD-N-064 to WD-N-077 | `calculateWithdrawalFee`, line 20: `else if (accountBalance < 10000.00)` | Balances of $5,000.00 and $5,000.01 were charged 0.1% instead of no fee, so the upper threshold of the 0.1% band is $10,000 instead of $5,000. |
| D-F1 | DEP-WREC-01, DEP-WREC-02, DEP-WREC-10 | `bank.utils.FeesCalculator.calculateDepositInterest`: student amount threshold and rates | The implementation uses an amount threshold of $100 and rates of 1%, 0.5%, and 0.2%, rather than the specified $50 threshold and 0.5%, 0.25%, and 0% rates. |
| D-F2 | DEP-WREC-04, DEP-WREC-09 | `bank.utils.FeesCalculator.calculateDepositInterest`: student low-amount, low-balance branch | The implementation charges 0.2% when the specified fee is zero. |
| D-F3 | DEP-WREC-05, DEP-WREC-06 | `bank.utils.FeesCalculator.calculateDepositInterest`: non-student high-amount threshold and rates | The implementation uses a $500 amount threshold and $5,000 balance threshold instead of $250 and $2,500, and its rates do not match 0.8% and 0.4%. |
| D-F4 | DEP-WREC-07, DEP-WREC-08 | `bank.utils.FeesCalculator.calculateDepositInterest`: non-student low-amount rates | The implementation applies 0.5% above $10,000 and 0% otherwise, while the specification requires 0% above $10,000 and 0.1% otherwise. |
| T-F1 | TF-R1 to TF-R13, TF-R15 | `calculateTransferFee`, the `feePercentage = ...` rates (lines 83-133) | Rates are coded 10x too large (for example `0.01` for 0.1%, where `calculateWithdrawalFee` correctly uses `0.001`). The tests where no threshold fault applies show this directly: TF-R7, TF-R8, TF-R12 and TF-R15 each return exactly 10x the expected fee (TF-R7: 12.5 instead of 1.25). In the other tests a threshold fault (T-F2 to T-F6) also sends the input to another rule, so the ratio differs (TF-R11 returned 2.5, 5x the expected 0.50). |
| T-F2 | TF-R1, TF-R2, TF-R3, TF-R4 | `calculateTransferFee`, line 80, `if (amount < 100.00)` in the student branch | The student amount threshold should be $200, so a $150 student transfer goes to the ">= $200" branch, as the actual values show (TF-R3 returned 150 x 0.025 = 3.75, the large-amount rate). |
| T-F3 | TF-R1, TF-R2, TF-R5, TF-R6 | `calculateTransferFee`, lines 81 and 95, `if (fromAccountBalance < 1000.00)` in the student branch | The student "from" threshold should be $2,000, so a from balance of $1,000 is treated as ">= threshold" (TF-R5 returned 500 x 0.025 = 12.5, the from-high rate). |
| T-F4 | TF-R9, TF-R10 | `calculateTransferFee`, line 111, `if (fromAccountBalance < 1000.00)` in the non-student, amount < $100 branch | The specification uses $4,000 here, so a from balance of $3,000 is wrongly treated as ">= threshold". |
| T-F5 | TF-R9, TF-R11 | `calculateTransferFee`, lines 112 and 118, `if (toAccountBalance < 1000.00)` in the non-student, amount < $100 branch | The specification uses $2,000 here, so a to balance of $1,500 is wrongly treated as ">= threshold". |
| T-F6 | TF-R13, TF-R14 | `calculateTransferFee`, line 125, `if (fromAccountBalance < 1000.00)` in the non-student, amount >= $100 branch | The threshold should be $2,000, so a from balance of $1,000 takes the from-high path (TF-R13 returned 150 x 0.05 = 7.5). |
| T-F7 | TF-R14, TF-R16 | `calculateTransferFee`, line 135, `feePercentage = 0.055;` | For 0.25% the rate should be `0.0025`; `0.055` (5.5%) does not match even the 10x pattern of T-F1, so both tests returned 150 x 0.055 = 8.25. |


---

## Failure log (kept for Assignment 3)

Each person adds a row as soon as they find a failure. Times are rough estimates in minutes.
Fill in your own times; do not reconstruct them later.

| Failure ID | Test case(s) | Found by | Time testing before found (min) | Time diagnosing (min) | Fault |
|---|---|---|---|---|---|
| S-F1 | SE-P-01 | Alessia Pilla | | | S-F1 |
| S-F2 | SE-A-09 | Alessia Pilla | | | S-F2 |
| S-F3 | SE-A-21 | Alessia Pilla | | | S-F3 |
| S-F4 | SE-A-23 | Alessia Pilla | | | S-F4 |
| W-F1 | WD-S-02 | Alan Cruz | | | W-F1 |
| W-F2 | WD-N-001 | Alan Cruz | | | W-F2 |
| W-F3 | WD-N-064 | Alan Cruz | | | W-F3 |
| D-F1 | DEP-WREC-01, DEP-WREC-02 | Jared Jack Harrison | 85 | 18 | D-F1 |
| D-F2 | DEP-WREC-04 | Jared Jack Harrison | 6 | 9 | D-F2 |
| D-F3 | DEP-WREC-05, DEP-WREC-06 | Jared Jack Harrison | 4 | 14 | D-F3 |
| D-F4 | DEP-WREC-07, DEP-WREC-08 | Jared Jack Harrison | 5 | 8 | D-F4 |
| T-F1 | TF-R1 to TF-R13, TF-R15 | Julia Kasperek | 15 | 20 | T-F1 |
| T-F2 | TF-R1 to TF-R4 | Julia Kasperek | 15 | 10 | T-F2 |
| T-F3 | TF-R1, TF-R2, TF-R5, TF-R6 | Julia Kasperek | 15 | 12 | T-F3 |
| T-F4 | TF-R9, TF-R10 | Julia Kasperek | 15 | 15 | T-F4 |
| T-F5 | TF-R9, TF-R11 | Julia Kasperek | 15 | 15 | T-F5 |
| T-F6 | TF-R13, TF-R14 | Julia Kasperek | 15 | 8 | T-F6 |
| T-F7 | TF-R14, TF-R16 | Julia Kasperek | 15 | 5 | T-F7 |

Transfer rows (T-F1 to T-F7): times were not tracked during testing, so these are estimates made afterwards.
