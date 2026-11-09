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

Target: `atm.session.Session`, for the withdrawal transaction only. `Session` does not check the PIN format itself. `atm.ATM.checkPin` does, so the PIN tests go through `ATM`.
Test class: `src/test/java/atm/session/SessionTest.java`
Shared setup: `src/test/java/atm/fixtures/SeededAccount.java`

**Rules under test**

| Rule | Condition | Expected result |
|---|---|---|
| P1 | The PIN has exactly 5 digits | PIN accepted |
| P2 | Any other PIN | `InvalidPinFormatException` |
| M1 | The amount is from $20 to $1,000 and can be made from $20 and $50 bills | Withdrawal completes |
| M2 | Any other amount | `InvalidAmountException` |

**Values tested**

| Input | Too low | Lowest | Next above lowest | Middle | Next below highest | Highest | Too high |
|---|---|---|---|---|---|---|---|
| PIN | `0000` | `00000` | `00001` | `55555` | `99998` | `99999` | `100000` |
| Amount ($) | 10 | 20 | 40 | 500 | 990 | 1,000 | 1,010 |

**Assumptions**

1. **S-A1. Amount range.** Withdrawals go from $20 to $1,000. $20 is the smallest amount the bills can make, and the assignment makes the $1,000 daily limit the upper boundary. This is the same range as A1 in [withdrawal.md](assignment1/withdrawal.md#assumptions).
2. **S-A2. Steps of $10.** Every amount the bills can make is a multiple of $10, so the table moves in steps of $10. The next value after $20 is $40, because $30 cannot be made. Amounts $1 away from the boundaries (19, 21, 999, 1,001) are tested separately.
3. **S-A3. Going over the limit.** The specification does not name an exception for this. We expect `InvalidAmountException`, because an amount over the limit is not a valid amount. The same applies to amounts below $20, including $0 and negative amounts.
4. **S-A4. PIN range.** A 5-digit PIN is treated as a number from `00000` to `99999`. The value below the lowest is the 4-digit `0000`, and the value above the highest is `100000`.
5. **S-A5. Fixed values.** Anything a test is not checking stays the same. The account always has a $1,000 daily limit and a $5,000 balance, belongs to a non-student, and uses Chequing. The $100,000 balance maximum is not used here, because the assignment sets this balance to $5,000.
6. **S-A6. Worst case with a single fault.** Worst case normally tries every combination of values, including two bad inputs at once. The single fault assumption allows only one bad input per test, so we kept every combination except the ones where both the PIN and the amount are bad.
7. **S-A7. No console version.** The assignment refers to a console version of the ATM, but this repository only has the Swing screens. The tests call `ATM` and `Session` directly, so no screens are needed.

**Process**

1. We picked the seven values for each input, as shown in the table above.
2. Trying every PIN with every amount gives 7 x 7 = 49 cases. Removing the 4 cases with a bad PIN and a bad amount leaves 45:
   - 25 with a good PIN and a good amount (SES-20 to SES-44);
   - 10 with a good PIN and a bad amount (SES-45 to SES-54);
   - 10 with a bad PIN and a good amount.
3. The ATM stops at a bad PIN and never asks for an amount, so changing the amount cannot change the result. We ran each bad PIN once, which turns those 10 cases into 2 tests (SES-06 and SES-07). That leaves 37 tests from the table.
4. We added 35 tests for cases the table misses:
   - 5 good PINs checked through `ATM` (SES-01 to SES-05);
   - 12 PINs with the wrong length or with non-digits (SES-08 to SES-19);
   - 8 amounts made from different mixes of bills (SES-55 to SES-62);
   - 5 amounts the bills cannot make (SES-63 to SES-67);
   - 2 amounts over the limit that the bills can make (SES-68 and SES-69);
   - 3 zero or negative amounts (SES-70 to SES-72).
5. We worked out the expected result for each test from the rules above, never from the code. This gives **72 tests**.
6. Each case is one row of a JUnit 5 `@ParameterizedTest`. Mockito replaces the screen (`MainPanel`) and the bank (`MessageDispatcher`), so the tests need no database or window.

**How PIN and amount tests run**

- PIN tests call `ATM.checkPin` with the real `FormatChecker`.
- Amount tests call `Session.setAmount` directly, so a bug in the PIN check cannot hide the amount results. `Session` stores the PIN without checking it, so these tests only confirm that the PIN reaches the bank unchanged.
- The fake bank approves everything and does not check the daily limit, so an amount can only be rejected by the ATM itself.

**How the extra tests were chosen**

The values in the table come from the specification. The bill mix and over-the-limit tests (SES-55 to SES-62, SES-68 and SES-69) were added after reading `CashValidator`, so those input values were not chosen from the specification alone. Their expected results still come only from the rules above.

**Checking the tests**

To make sure the tests themselves were correct, we fixed the faults in a temporary copy of the code. All 72 tests passed on that copy. No fixes are part of this phase.

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

The Actual result and Status columns are the test results for this section. In the JUnit test column, `[n]` is the run number shown in the test report.

| ID | JUnit test | PIN | Amount ($) | Case | Expected result | Actual result | Status |
|---|---|---|---:|---|---|---|---|
| SES-01 | `validPinIsAccepted[1]` | `00000` | Not reached | PIN lowest | PIN accepted | `InvalidPinFormatException` | Fail |
| SES-02 | `validPinIsAccepted[2]` | `00001` | Not reached | PIN next above lowest | PIN accepted | `InvalidPinFormatException` | Fail |
| SES-03 | `validPinIsAccepted[3]` | `55555` | Not reached | PIN middle | PIN accepted | `InvalidPinFormatException` | Fail |
| SES-04 | `validPinIsAccepted[4]` | `99998` | Not reached | PIN next below highest | PIN accepted | `InvalidPinFormatException` | Fail |
| SES-05 | `validPinIsAccepted[5]` | `99999` | Not reached | PIN highest | PIN accepted | `InvalidPinFormatException` | Fail |
| SES-06 | `outOfRangePinIsRejected[1]` | `0000` | Not reached | PIN too low (too short) | `InvalidPinFormatException` | PIN accepted | Fail |
| SES-07 | `outOfRangePinIsRejected[2]` | `100000` | Not reached | PIN too high (too long) | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-08 | `pinOfWrongLengthIsRejected[1]` | (empty) | Not reached | Empty | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-09 | `pinOfWrongLengthIsRejected[2]` | `5` | Not reached | 1 digit | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-10 | `pinOfWrongLengthIsRejected[3]` | `5555` | Not reached | 4 digits | `InvalidPinFormatException` | PIN accepted | Fail |
| SES-11 | `pinOfWrongLengthIsRejected[4]` | `9999` | Not reached | 4 digits, all 9s | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-12 | `pinOfWrongLengthIsRejected[5]` | `555555` | Not reached | 6 digits | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-13 | `pinOfWrongLengthIsRejected[6]` | `999999` | Not reached | 6 digits, all 9s | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-14 | `pinWithNonDigitIsRejected[1]` | `/5555` | Not reached | Character before `0`, first place | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-15 | `pinWithNonDigitIsRejected[2]` | `5555/` | Not reached | Character before `0`, last place | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-16 | `pinWithNonDigitIsRejected[3]` | `:5555` | Not reached | Character after `9`, first place | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-17 | `pinWithNonDigitIsRejected[4]` | `5555:` | Not reached | Character after `9`, last place | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-18 | `pinWithNonDigitIsRejected[5]` | `a5555` | Not reached | Letter, first place | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-19 | `pinWithNonDigitIsRejected[6]` | `5555a` | Not reached | Letter, last place | `InvalidPinFormatException` | `InvalidPinFormatException` | Pass |
| SES-20 | `validPinAndAmountCompleteWithdrawal[1]` | `00000` | 20 | PIN lowest, amount lowest | Withdrawal completes | Withdrawal completes | Pass |
| SES-21 | `validPinAndAmountCompleteWithdrawal[2]` | `00000` | 40 | PIN lowest, amount next above lowest | Withdrawal completes | Withdrawal completes | Pass |
| SES-22 | `validPinAndAmountCompleteWithdrawal[3]` | `00000` | 500 | PIN lowest, amount middle | Withdrawal completes | Withdrawal completes | Pass |
| SES-23 | `validPinAndAmountCompleteWithdrawal[4]` | `00000` | 990 | PIN lowest, amount next below highest | Withdrawal completes | Withdrawal completes | Pass |
| SES-24 | `validPinAndAmountCompleteWithdrawal[5]` | `00000` | 1,000 | PIN lowest, amount highest | Withdrawal completes | Withdrawal completes | Pass |
| SES-25 | `validPinAndAmountCompleteWithdrawal[6]` | `00001` | 20 | PIN next above lowest, amount lowest | Withdrawal completes | Withdrawal completes | Pass |
| SES-26 | `validPinAndAmountCompleteWithdrawal[7]` | `00001` | 40 | PIN next above lowest, amount next above lowest | Withdrawal completes | Withdrawal completes | Pass |
| SES-27 | `validPinAndAmountCompleteWithdrawal[8]` | `00001` | 500 | PIN next above lowest, amount middle | Withdrawal completes | Withdrawal completes | Pass |
| SES-28 | `validPinAndAmountCompleteWithdrawal[9]` | `00001` | 990 | PIN next above lowest, amount next below highest | Withdrawal completes | Withdrawal completes | Pass |
| SES-29 | `validPinAndAmountCompleteWithdrawal[10]` | `00001` | 1,000 | PIN next above lowest, amount highest | Withdrawal completes | Withdrawal completes | Pass |
| SES-30 | `validPinAndAmountCompleteWithdrawal[11]` | `55555` | 20 | PIN middle, amount lowest | Withdrawal completes | Withdrawal completes | Pass |
| SES-31 | `validPinAndAmountCompleteWithdrawal[12]` | `55555` | 40 | PIN middle, amount next above lowest | Withdrawal completes | Withdrawal completes | Pass |
| SES-32 | `validPinAndAmountCompleteWithdrawal[13]` | `55555` | 500 | PIN middle, amount middle | Withdrawal completes | Withdrawal completes | Pass |
| SES-33 | `validPinAndAmountCompleteWithdrawal[14]` | `55555` | 990 | PIN middle, amount next below highest | Withdrawal completes | Withdrawal completes | Pass |
| SES-34 | `validPinAndAmountCompleteWithdrawal[15]` | `55555` | 1,000 | PIN middle, amount highest | Withdrawal completes | Withdrawal completes | Pass |
| SES-35 | `validPinAndAmountCompleteWithdrawal[16]` | `99998` | 20 | PIN next below highest, amount lowest | Withdrawal completes | Withdrawal completes | Pass |
| SES-36 | `validPinAndAmountCompleteWithdrawal[17]` | `99998` | 40 | PIN next below highest, amount next above lowest | Withdrawal completes | Withdrawal completes | Pass |
| SES-37 | `validPinAndAmountCompleteWithdrawal[18]` | `99998` | 500 | PIN next below highest, amount middle | Withdrawal completes | Withdrawal completes | Pass |
| SES-38 | `validPinAndAmountCompleteWithdrawal[19]` | `99998` | 990 | PIN next below highest, amount next below highest | Withdrawal completes | Withdrawal completes | Pass |
| SES-39 | `validPinAndAmountCompleteWithdrawal[20]` | `99998` | 1,000 | PIN next below highest, amount highest | Withdrawal completes | Withdrawal completes | Pass |
| SES-40 | `validPinAndAmountCompleteWithdrawal[21]` | `99999` | 20 | PIN highest, amount lowest | Withdrawal completes | Withdrawal completes | Pass |
| SES-41 | `validPinAndAmountCompleteWithdrawal[22]` | `99999` | 40 | PIN highest, amount next above lowest | Withdrawal completes | Withdrawal completes | Pass |
| SES-42 | `validPinAndAmountCompleteWithdrawal[23]` | `99999` | 500 | PIN highest, amount middle | Withdrawal completes | Withdrawal completes | Pass |
| SES-43 | `validPinAndAmountCompleteWithdrawal[24]` | `99999` | 990 | PIN highest, amount next below highest | Withdrawal completes | Withdrawal completes | Pass |
| SES-44 | `validPinAndAmountCompleteWithdrawal[25]` | `99999` | 1,000 | PIN highest, amount highest | Withdrawal completes | Withdrawal completes | Pass |
| SES-45 | `outOfRangeAmountIsRejected[1]` | `00000` | 10 | PIN lowest, amount too low | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-46 | `outOfRangeAmountIsRejected[2]` | `00000` | 1,010 | PIN lowest, amount too high | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-47 | `outOfRangeAmountIsRejected[3]` | `00001` | 10 | PIN next above lowest, amount too low | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-48 | `outOfRangeAmountIsRejected[4]` | `00001` | 1,010 | PIN next above lowest, amount too high | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-49 | `outOfRangeAmountIsRejected[5]` | `55555` | 10 | PIN middle, amount too low | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-50 | `outOfRangeAmountIsRejected[6]` | `55555` | 1,010 | PIN middle, amount too high | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-51 | `outOfRangeAmountIsRejected[7]` | `99998` | 10 | PIN next below highest, amount too low | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-52 | `outOfRangeAmountIsRejected[8]` | `99998` | 1,010 | PIN next below highest, amount too high | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-53 | `outOfRangeAmountIsRejected[9]` | `99999` | 10 | PIN highest, amount too low | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-54 | `outOfRangeAmountIsRejected[10]` | `99999` | 1,010 | PIN highest, amount too high | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-55 | `dispensableAmountCompletesWithdrawal[1]` | `55555` | 50 | One $50 | Withdrawal completes | Withdrawal completes | Pass |
| SES-56 | `dispensableAmountCompletesWithdrawal[2]` | `55555` | 60 | Three $20s | Withdrawal completes | `InvalidAmountException` | Fail |
| SES-57 | `dispensableAmountCompletesWithdrawal[3]` | `55555` | 70 | $50 + $20 | Withdrawal completes | Withdrawal completes | Pass |
| SES-58 | `dispensableAmountCompletesWithdrawal[4]` | `55555` | 80 | Four $20s | Withdrawal completes | `InvalidAmountException` | Fail |
| SES-59 | `dispensableAmountCompletesWithdrawal[5]` | `55555` | 110 | $50 + three $20s | Withdrawal completes | `InvalidAmountException` | Fail |
| SES-60 | `dispensableAmountCompletesWithdrawal[6]` | `55555` | 130 | $50 + four $20s | Withdrawal completes | `InvalidAmountException` | Fail |
| SES-61 | `dispensableAmountCompletesWithdrawal[7]` | `55555` | 960 | Mix of bills, near the top | Withdrawal completes | `InvalidAmountException` | Fail |
| SES-62 | `dispensableAmountCompletesWithdrawal[8]` | `55555` | 980 | Mix of bills, near the top | Withdrawal completes | `InvalidAmountException` | Fail |
| SES-63 | `undispensableAmountIsRejected[1]` | `55555` | 19 | $1 below lowest | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-64 | `undispensableAmountIsRejected[2]` | `55555` | 21 | $1 above lowest | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-65 | `undispensableAmountIsRejected[3]` | `55555` | 30 | Gap between $20 and $40 | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-66 | `undispensableAmountIsRejected[4]` | `55555` | 999 | $1 below highest | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-67 | `undispensableAmountIsRejected[5]` | `55555` | 1,001 | $1 above highest | `InvalidAmountException` | `InvalidAmountException` | Pass |
| SES-68 | `dispensableAmountOverDailyLimitIsRejected[1]` | `55555` | 1,020 | Over the limit, bills can make it | `InvalidAmountException` | Withdrawal completes | Fail |
| SES-69 | `dispensableAmountOverDailyLimitIsRejected[2]` | `55555` | 1,050 | Over the limit, bills can make it | `InvalidAmountException` | Withdrawal completes | Fail |
| SES-70 | `nonPositiveAmountIsRejected[1]` | `55555` | 0 | Zero | `InvalidAmountException` | Withdrawal completes | Fail |
| SES-71 | `nonPositiveAmountIsRejected[2]` | `55555` | -20 | Negative | `InvalidAmountException` | Withdrawal completes | Fail |
| SES-72 | `nonPositiveAmountIsRejected[3]` | `55555` | -50 | Negative | `InvalidAmountException` | Withdrawal completes | Fail |

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

### 2.4 Transfer fee: decision table

T = true, F = false.

| Rule | R1 | R2 | R3 | R4 | R5 | R6 | R7 | R8 | R9 | R10 | R11 | R12 | R13 | R14 | R15 | R16 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| C1 student | T | T | T | T | T | T | T | T | F | F | F | F | F | F | F | F |
| C2 amount < threshold | T | T | T | T | F | F | F | F | T | T | T | T | F | F | F | F |
| C3 from < threshold | T | T | F | F | T | T | F | F | T | T | F | F | T | T | F | F |
| C4 to < threshold | T | F | T | F | T | F | T | F | T | F | T | F | T | F | T | F |
| **Fee rate** | 0.1% | 0.05% | 0.05% | 0.025% | 0.05% | 0.025% | 0.25% | 0.125% | 0.2% | 0.1% | 1% | 0.5% | 0.2% | 0.1% | 0.5% | 0.25% |

### 2.4 Transfer fee: test cases

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

See the Actual result and Status columns in section 2.1.

| JUnit test | Run | Passed | Failed |
|---|---:|---:|---:|
| `validPinIsAccepted` | 5 | 0 | 5 |
| `outOfRangePinIsRejected` | 2 | 1 | 1 |
| `pinOfWrongLengthIsRejected` | 6 | 5 | 1 |
| `pinWithNonDigitIsRejected` | 6 | 6 | 0 |
| `validPinAndAmountCompleteWithdrawal` | 25 | 25 | 0 |
| `outOfRangeAmountIsRejected` | 10 | 10 | 0 |
| `dispensableAmountCompletesWithdrawal` | 8 | 2 | 6 |
| `undispensableAmountIsRejected` | 5 | 5 | 0 |
| `dispensableAmountOverDailyLimitIsRejected` | 2 | 0 | 2 |
| `nonPositiveAmountIsRejected` | 3 | 0 | 3 |
| **Total** | **72** | **54** | **18** |

The $1,010 tests pass, but only by luck. Fault S-F2 rejects $1,010, not a daily limit check. SES-68 and SES-69 show that the limit is missing.

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
| S-F1 | SES-01 to SES-06, SES-10 | `atm/utils/FormatChecker.java`, `checkPinFormat`, line 33: `if (pin.length == 4)` | Every 5-digit PIN was rejected, while the 4-digit PINs `0000` and `5555` were accepted, so the code requires 4 digits instead of 5. The loop on line 34 also stops at 4 and needs the same change. |
| S-F2 | SES-56, SES-58 to SES-62 | `atm/utils/CashValidator.java`, `validateWithdrawal`, lines 23 and 25: `amount % 50`, then `newAmount % 20` | The code takes out as many $50 bills as it can and never tries using fewer. When $10 or $30 is left over it rejects the amount, even though $60, $80, $110, $130, $960 and $980 can all be made. |
| S-F3 | SES-68, SES-69 | `atm/utils/CashValidator.java`, `validateWithdrawal` (line 20): missing check, called from `Session.setAmount`, line 88 | $1,020 and $1,050 were paid out. Nothing in the code checks the $1,000 daily limit. |
| S-F4 | SES-70, SES-71, SES-72 | `atm/utils/CashValidator.java`, `validateWithdrawal` (line 20): missing check, called from `Session.setAmount`, line 88 | $0, -$20 and -$50 were paid out. The code only looks at what is left over after dividing, and these leave nothing over, so a minimum amount check is missing. |
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

Hidden behind S-F1: `atm/utils/FormatChecker.java`, line 35 (`pin[i] >= '9'`) rejects the digit 9. No test can show this yet, because S-F1 rejects every 5-digit PIN first. We confirmed this on a temporary copy: with only S-F1 fixed, SES-04 (`99998`) and SES-05 (`99999`) still fail.


---

## Failure log (kept for Assignment 3)

Each person adds a row as soon as they find a failure. Times are rough estimates in minutes.
Fill in your own times; do not reconstruct them later.

| Failure ID | Test case(s) | Found by | Time testing before found (min) | Time diagnosing (min) | Fault |
|---|---|---|---|---|---|
| S-F1 | SES-01 to SES-06, SES-10 | alessiap000 | | | S-F1 |
| S-F2 | SES-56, SES-58 to SES-62 | alessiap000 | | | S-F2 |
| S-F3 | SES-68, SES-69 | alessiap000 | | | S-F3 |
| S-F4 | SES-70, SES-71, SES-72 | alessiap000 | | | S-F4 |
| W-F1 | WD-S-02 | Alan Cruz | | | W-F1 |
| W-F2 | WD-N-001 | Alan Cruz | | | W-F2 |
| W-F3 | WD-N-064 | Alan Cruz | | | W-F3 |
| DEP-F01 | DEP-WREC-01, DEP-WREC-02 | Jared Jack Harrison | | | D-F1 |
| DEP-F02 | DEP-WREC-04 | Jared Jack Harrison | | | D-F2 |
| DEP-F03 | DEP-WREC-05, DEP-WREC-06 | Jared Jack Harrison | | | D-F3 |
| DEP-F04 | DEP-WREC-07, DEP-WREC-08 | Jared Jack Harrison | | | D-F4 |
| TR-F1 | TF-R1 to TF-R13, TF-R15 | Julia Kasperek | | | T-F1 |
| TR-F2 | TF-R1 to TF-R4 | Julia Kasperek | | | T-F2 |
| TR-F3 | TF-R1, TF-R2, TF-R5, TF-R6 | Julia Kasperek | | | T-F3 |
| TR-F4 | TF-R9, TF-R10 | Julia Kasperek | | | T-F4 |
| TR-F5 | TF-R9, TF-R11 | Julia Kasperek | | | T-F5 |
| TR-F6 | TF-R13, TF-R14 | Julia Kasperek | | | T-F6 |
| TR-F7 | TF-R14, TF-R16 | Julia Kasperek | | | T-F7 |
