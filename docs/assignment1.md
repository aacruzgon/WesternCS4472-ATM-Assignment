# Assignment 1: Black Box Testing

## Deposit Fees: Weak Robust Equivalence Class Analysis

### Process and assumptions

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
- The specification does not explicitly define invalid numeric inputs. For the
  robust portion of the analysis, negative deposit amounts and negative account
  balances are assumed invalid. Each is tested separately while all other
  inputs remain nominal, following the single-fault principle. The expected
  behavior is rejection with `IllegalArgumentException`.
- Expected monetary values are compared using a floating-point tolerance of
  `0.000001`.

### Test cases and results

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
| DEP-WREC-09 | Yes | -1.00 | 1,000.00 | Invalid amount | `IllegalArgumentException` | Returned -0.002 | Fail |
| DEP-WREC-10 | Yes | 100.00 | -1.00 | Invalid balance | `IllegalArgumentException` | Returned 0.20 | Fail |

### Fault localization

| Failing tests | Suspected fault | Justification |
|---|---|---|
| DEP-WREC-01, DEP-WREC-02 | `bank.utils.FeesCalculator.calculateDepositInterest`: student amount threshold and rates | The implementation uses an amount threshold of $100 and rates of 1%, 0.5%, and 0.2%, rather than the specified $50 threshold and 0.5%, 0.25%, and 0% rates. |
| DEP-WREC-04 | `bank.utils.FeesCalculator.calculateDepositInterest`: student low-amount, low-balance branch | The implementation charges 0.2% when the specified fee is zero. |
| DEP-WREC-05, DEP-WREC-06 | `bank.utils.FeesCalculator.calculateDepositInterest`: non-student high-amount threshold and rates | The implementation uses a $500 amount threshold and $5,000 balance threshold instead of $250 and $2,500, and its rates do not match 0.8% and 0.4%. |
| DEP-WREC-07, DEP-WREC-08 | `bank.utils.FeesCalculator.calculateDepositInterest`: non-student low-amount rates | The implementation applies 0.5% above $10,000 and 0% otherwise, while the specification requires 0% above $10,000 and 0.1% otherwise. |
| DEP-WREC-09, DEP-WREC-10 | `bank.utils.FeesCalculator.calculateDepositInterest`: missing input validation | The method calculates a result for a negative amount or balance instead of rejecting the assumed invalid input. |

### Failure log

Times should be updated by the tester with the actual elapsed time rather than
reconstructed later.

| Failure ID | Test case(s) | Testing time before discovery | Diagnosis time | Notes |
|---|---|---|---|---|
| DEP-F01 | DEP-WREC-01, DEP-WREC-02 | To record | To record | Student high-amount threshold/rates |
| DEP-F02 | DEP-WREC-04 | To record | To record | Student low-amount, low-balance rate |
| DEP-F03 | DEP-WREC-05, DEP-WREC-06 | To record | To record | Non-student high-amount thresholds/rates |
| DEP-F04 | DEP-WREC-07, DEP-WREC-08 | To record | To record | Non-student low-amount rates |
| DEP-F05 | DEP-WREC-09, DEP-WREC-10 | To record | To record | Missing validation under stated assumption |
