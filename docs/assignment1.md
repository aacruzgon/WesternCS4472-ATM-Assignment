# Assignment 1: Black Box Testing

Team: _names here_
Pull request: _link here_

| Target | Technique | Owner | Test class |
|---|---|---|---|
| `atm.session.Session` (PIN format, withdrawal amount) | Robust Worst Case BVA, single fault assumption | _Person ?_ | `src/test/java/atm/session/...` |
| `FeesCalculator.calculateWithdrawalFee` | Robust Worst Case BVA | _Person ?_ | `src/test/java/bank/utils/...` |
| `FeesCalculator.calculateDepositInterest` | Weak Robust Equivalence Class Analysis | _Person ?_ | `src/test/java/bank/utils/...` |
| `FeesCalculator.calculateTransferFee` | Decision Table Analysis | Person C | `src/test/java/bank/utils/TransferFeeTest.java` |

---

## Section 1: Process and assumptions

### 1.1 Session: PIN format and withdrawal amount (Robust Worst Case BVA)
_TODO (owner)_

### 1.2 Withdrawal fee (Robust Worst Case BVA)
_TODO (owner)_

### 1.3 Deposit fee (Weak Robust Equivalence Class Analysis)
_TODO (owner)_

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
7. **Floating point.** Expected and actual fees are compared with a tolerance of 0.0001.

---

## Section 2: Test cases

### 2.1 Session
_TODO (owner)_

### 2.2 Withdrawal fee
_TODO (owner)_

### 2.3 Deposit fee
_TODO (owner)_

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

### 3.1 Session
_TODO (owner)_

### 3.2 Withdrawal fee
_TODO (owner)_

### 3.3 Deposit fee
_TODO (owner)_

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

All faults are in `src/main/java/bank/utils/FeesCalculator.java`. Several failing tests expose the
same fault, so they are grouped. One test can expose more than one fault.

| Fault | Failing tests | Location (method, line, statement) | Justification |
|---|---|---|---|
| F-T1 | TF-R1 to TF-R16 | `calculateTransferFee`, every `feePercentage = ...` (lines 83-135) | Rates are coded 10x too large (for example `0.01` for 0.1%, where `calculateWithdrawalFee` correctly uses `0.001`), so every fee is at least 10x the expected value, as in TF-R7 (12.5 instead of 1.25). |
| F-T2 | TF-R1, TF-R2, TF-R3, TF-R4 | `calculateTransferFee`, line 80, `if (amount < 100.00)` in the student branch | The student amount threshold should be $200, so a $150 student transfer goes to the ">= $200" branch, as the actual values show (TF-R3 returned 150 x 0.025 = 3.75, the large-amount rate). |
| F-T3 | TF-R1, TF-R2, TF-R5, TF-R6 | `calculateTransferFee`, lines 81 and 95, `if (fromAccountBalance < 1000.00)` in the student branch | The student "from" threshold should be $2,000, so a from balance of $1,000 is treated as ">= threshold" (TF-R5 returned 500 x 0.025 = 12.5, the from-high rate). |
| F-T4 | TF-R9, TF-R10 | `calculateTransferFee`, line 111, `if (fromAccountBalance < 1000.00)` in the non-student, amount < $100 branch | The specification uses $4,000 here, so a from balance of $3,000 is wrongly treated as ">= threshold". |
| F-T5 | TF-R9, TF-R11 | `calculateTransferFee`, lines 112 and 118, `if (toAccountBalance < 1000.00)` in the non-student, amount < $100 branch | The specification uses $2,000 here, so a to balance of $1,500 is wrongly treated as ">= threshold". |
| F-T6 | TF-R13, TF-R14 | `calculateTransferFee`, line 125, `if (fromAccountBalance < 1000.00)` in the non-student, amount >= $100 branch | The threshold should be $2,000, so a from balance of $1,000 takes the from-high path (TF-R13 returned 150 x 0.05 = 7.5). |
| F-T7 | TF-R14, TF-R16 | `calculateTransferFee`, line 135, `feePercentage = 0.055;` | For 0.25% the rate should be `0.0025`; `0.055` (5.5%) does not match even the 10x pattern of F-T1, so both tests returned 150 x 0.055 = 8.25. |

---

## Failure log (kept for Assignment 3)

Each team member adds a row as soon as they find a failure. Times are rough estimates in minutes.

| Failure ID | Test case | Found by | Time testing before found (min) | Time diagnosing (min) | Fault |
|---|---|---|---|---|---|
| FL-01 | TF-R1 to TF-R16 | Person C | | | F-T1 |
| FL-02 | TF-R1 to TF-R4 | Person C | | | F-T2 |
| FL-03 | TF-R1, TF-R2, TF-R5, TF-R6 | Person C | | | F-T3 |
| FL-04 | TF-R9, TF-R10 | Person C | | | F-T4 |
| FL-05 | TF-R9, TF-R11 | Person C | | | F-T5 |
| FL-06 | TF-R13, TF-R14 | Person C | | | F-T6 |
| FL-07 | TF-R14, TF-R16 | Person C | | | F-T7 |
