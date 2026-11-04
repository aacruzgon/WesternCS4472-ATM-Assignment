# Assignment 1: Withdrawal fees (Robust Worst Case BVA)

Target: `bank.utils.FeesCalculator.calculateWithdrawalFee(double amount, double accountBalance, boolean student, int dayOfWeek)`
Technique: Robust Worst Case Boundary Value Analysis
JUnit class: `src/test/java/bank/utils/FeesCalculatorWithdrawalTest.java`

## Section 1: Process and assumptions

### Specification under test

| Rule | Condition | Fee |
|---|---|---|
| S1 | Student, transaction on a weekday | no fee |
| S2 | Student, otherwise | 0.1% of amount |
| N1 | Not a student, balance < $1,000 | 0.3% of amount |
| N2 | Not a student, $1,000 <= balance < $5,000 | 0.1% of amount |
| N3 | Not a student, balance >= $5,000 | no fee |

The expected fee for every test case is `amount x rate` with the rate taken from this table. The expected values were worked out from the specification only, never from the code.

### Input variables and domains

| Variable | Domain used for the analysis | Step | Boundaries inside the domain |
|---|---|---|---|
| `amount` | $20 to $1,000 | $1 | none (the fee is proportional to the amount) |
| `accountBalance` | $0.00 to $100,000.00 | $0.01 | $1,000 and $5,000 (non-students only) |
| `dayOfWeek` | 1 (`Calendar.SUNDAY`) to 7 (`Calendar.SATURDAY`) | 1 | weekend/weekday changes between 1 and 2 and between 6 and 7 |
| `student` | `true` / `false` | n/a | Boolean: not a range, so it splits the analysis into two parts |

### Assumptions

1. **A1. Amount domain.** The specification gives no range for the withdrawal amount. We use $20 (the smallest bill the ATM dispenses) as the minimum and $1,000 (the daily limit the assignment fixes for the account) as the maximum. Withdrawals are whole dollars, so the step is $1.
2. **A2. Balance domain.** A balance cannot be negative, so the minimum is $0.00. The specification has no maximum, so we assume $100,000.00, well above the highest threshold ($5,000). Balances are in cents, so the step is $0.01.
3. **A3. Day encoding.** `dayOfWeek` uses `java.util.Calendar` constants (`SUNDAY = 1`, `MONDAY = 2`, ..., `SATURDAY = 7`), because the caller (`bank.transactions.BankWithdrawal`) passes `Calendar.getInstance().get(Calendar.DAY_OF_WEEK)`. A weekday is Monday to Friday (2 to 6).
4. **A4. Out-of-range values (robust cases).** `calculateWithdrawalFee` returns a `double` and the specification defines no error for bad input, so we expect the rules to be applied literally to out-of-range values:
   - an amount of $19 or $1,001 is charged at the normal rate;
   - a balance of -$0.01 is "less than $1,000" (N1), and $100,000.01 is "$5,000 or more" (N3);
   - a day of 0 or 8 is not a weekday, so for a student it falls under "otherwise" (S2, 0.1%).

   The deposit section uses the same assumption. Input validation belongs to `atm.session.Session`, where the specification defines `InvalidAmountException` and `InvalidPinFormatException`.
5. **A5. Irrelevant variables are held at nominal.** The specification makes the student fee depend only on the day and the non-student fee depend only on the balance. For students the balance is held at a nominal $3,000.00; for non-students the day is held at a nominal 4 (`WEDNESDAY`). Without this, the product would be 2 x 7 x 15 x 7 = 1,470 cases, and more than 1,300 of them would only repeat inputs the specification ignores.
6. **A6. Oracle tolerance.** Fees are `double`s, so they are compared with a tolerance of 1e-6. That is far smaller than the smallest difference between two specified fees (0.1% of $19 = $0.019), so no fault can hide inside the tolerance.

### Process

1. **Values per variable.** For each range we took the seven robust boundary values min-, min, min+, nom, max-, max, max+:
   - `amount` on [$20, $1,000]: 19, 20, 21, 510, 999, 1,000, 1,001 (7 values).
   - `dayOfWeek` on [1, 7]: 0, 1, 2, 4, 6, 7, 8 (7 values). These also cover both weekend/weekday changes (1/2 and 6/7), because the weekday range [2, 6] is exactly min+ to max- of the domain.
   - `accountBalance`: the balance has two internal boundaries, so we split [$0, $100,000] into the three ranges the specification defines ([0, 1,000), [1,000, 5,000), [5,000, 100,000]) and took robust boundary values for each. Neighbouring ranges share boundary points, which leaves 15 distinct values: -0.01, 0.00, 0.01, 500.00, 999.99 | 1,000.00, 1,000.01, 3,000.00, 4,999.99 | 5,000.00, 5,000.01, 52,500.00, 99,999.99, 100,000.00, 100,000.01. Every threshold is tested just below, on and just above.
2. **Worst case product.** Robust worst case takes the Cartesian product of the value sets of the variables that affect the output (A5):
   - student: `amount x dayOfWeek` = 7 x 7 = **49 cases** (WD-S-01 to WD-S-49);
   - non-student: `amount x accountBalance` = 7 x 15 = **105 cases** (WD-N-001 to WD-N-105).
   - Total: **154 test cases**.
3. **Expected values.** For each case we applied the matching rule (S1, S2, N1, N2 or N3) and computed `amount x rate`.
4. **Implementation.** Each table row is one row of a JUnit 5 `@ParameterizedTest` / `@CsvSource` in `FeesCalculatorWithdrawalTest`, and the test ID is the display name, so a failure in the CI report points straight to a row below.

## Section 2: Test cases

### Student (49 cases; balance held at $3,000.00)

| ID | Amount | Balance | Student | Day of week | Spec rule | Rate | Expected fee |
|---|---|---|---|---|---|---|---|
| WD-S-01 | $19.00 | $3,000.00 | yes | 0 (invalid) | S2 | 0.1% | 0.019 |
| WD-S-02 | $19.00 | $3,000.00 | yes | 1 SUNDAY | S2 | 0.1% | 0.019 |
| WD-S-03 | $19.00 | $3,000.00 | yes | 2 MONDAY | S1 | 0% | 0.0 |
| WD-S-04 | $19.00 | $3,000.00 | yes | 4 WEDNESDAY | S1 | 0% | 0.0 |
| WD-S-05 | $19.00 | $3,000.00 | yes | 6 FRIDAY | S1 | 0% | 0.0 |
| WD-S-06 | $19.00 | $3,000.00 | yes | 7 SATURDAY | S2 | 0.1% | 0.019 |
| WD-S-07 | $19.00 | $3,000.00 | yes | 8 (invalid) | S2 | 0.1% | 0.019 |
| WD-S-08 | $20.00 | $3,000.00 | yes | 0 (invalid) | S2 | 0.1% | 0.02 |
| WD-S-09 | $20.00 | $3,000.00 | yes | 1 SUNDAY | S2 | 0.1% | 0.02 |
| WD-S-10 | $20.00 | $3,000.00 | yes | 2 MONDAY | S1 | 0% | 0.0 |
| WD-S-11 | $20.00 | $3,000.00 | yes | 4 WEDNESDAY | S1 | 0% | 0.0 |
| WD-S-12 | $20.00 | $3,000.00 | yes | 6 FRIDAY | S1 | 0% | 0.0 |
| WD-S-13 | $20.00 | $3,000.00 | yes | 7 SATURDAY | S2 | 0.1% | 0.02 |
| WD-S-14 | $20.00 | $3,000.00 | yes | 8 (invalid) | S2 | 0.1% | 0.02 |
| WD-S-15 | $21.00 | $3,000.00 | yes | 0 (invalid) | S2 | 0.1% | 0.021 |
| WD-S-16 | $21.00 | $3,000.00 | yes | 1 SUNDAY | S2 | 0.1% | 0.021 |
| WD-S-17 | $21.00 | $3,000.00 | yes | 2 MONDAY | S1 | 0% | 0.0 |
| WD-S-18 | $21.00 | $3,000.00 | yes | 4 WEDNESDAY | S1 | 0% | 0.0 |
| WD-S-19 | $21.00 | $3,000.00 | yes | 6 FRIDAY | S1 | 0% | 0.0 |
| WD-S-20 | $21.00 | $3,000.00 | yes | 7 SATURDAY | S2 | 0.1% | 0.021 |
| WD-S-21 | $21.00 | $3,000.00 | yes | 8 (invalid) | S2 | 0.1% | 0.021 |
| WD-S-22 | $510.00 | $3,000.00 | yes | 0 (invalid) | S2 | 0.1% | 0.51 |
| WD-S-23 | $510.00 | $3,000.00 | yes | 1 SUNDAY | S2 | 0.1% | 0.51 |
| WD-S-24 | $510.00 | $3,000.00 | yes | 2 MONDAY | S1 | 0% | 0.0 |
| WD-S-25 | $510.00 | $3,000.00 | yes | 4 WEDNESDAY | S1 | 0% | 0.0 |
| WD-S-26 | $510.00 | $3,000.00 | yes | 6 FRIDAY | S1 | 0% | 0.0 |
| WD-S-27 | $510.00 | $3,000.00 | yes | 7 SATURDAY | S2 | 0.1% | 0.51 |
| WD-S-28 | $510.00 | $3,000.00 | yes | 8 (invalid) | S2 | 0.1% | 0.51 |
| WD-S-29 | $999.00 | $3,000.00 | yes | 0 (invalid) | S2 | 0.1% | 0.999 |
| WD-S-30 | $999.00 | $3,000.00 | yes | 1 SUNDAY | S2 | 0.1% | 0.999 |
| WD-S-31 | $999.00 | $3,000.00 | yes | 2 MONDAY | S1 | 0% | 0.0 |
| WD-S-32 | $999.00 | $3,000.00 | yes | 4 WEDNESDAY | S1 | 0% | 0.0 |
| WD-S-33 | $999.00 | $3,000.00 | yes | 6 FRIDAY | S1 | 0% | 0.0 |
| WD-S-34 | $999.00 | $3,000.00 | yes | 7 SATURDAY | S2 | 0.1% | 0.999 |
| WD-S-35 | $999.00 | $3,000.00 | yes | 8 (invalid) | S2 | 0.1% | 0.999 |
| WD-S-36 | $1,000.00 | $3,000.00 | yes | 0 (invalid) | S2 | 0.1% | 1 |
| WD-S-37 | $1,000.00 | $3,000.00 | yes | 1 SUNDAY | S2 | 0.1% | 1 |
| WD-S-38 | $1,000.00 | $3,000.00 | yes | 2 MONDAY | S1 | 0% | 0.0 |
| WD-S-39 | $1,000.00 | $3,000.00 | yes | 4 WEDNESDAY | S1 | 0% | 0.0 |
| WD-S-40 | $1,000.00 | $3,000.00 | yes | 6 FRIDAY | S1 | 0% | 0.0 |
| WD-S-41 | $1,000.00 | $3,000.00 | yes | 7 SATURDAY | S2 | 0.1% | 1 |
| WD-S-42 | $1,000.00 | $3,000.00 | yes | 8 (invalid) | S2 | 0.1% | 1 |
| WD-S-43 | $1,001.00 | $3,000.00 | yes | 0 (invalid) | S2 | 0.1% | 1.001 |
| WD-S-44 | $1,001.00 | $3,000.00 | yes | 1 SUNDAY | S2 | 0.1% | 1.001 |
| WD-S-45 | $1,001.00 | $3,000.00 | yes | 2 MONDAY | S1 | 0% | 0.0 |
| WD-S-46 | $1,001.00 | $3,000.00 | yes | 4 WEDNESDAY | S1 | 0% | 0.0 |
| WD-S-47 | $1,001.00 | $3,000.00 | yes | 6 FRIDAY | S1 | 0% | 0.0 |
| WD-S-48 | $1,001.00 | $3,000.00 | yes | 7 SATURDAY | S2 | 0.1% | 1.001 |
| WD-S-49 | $1,001.00 | $3,000.00 | yes | 8 (invalid) | S2 | 0.1% | 1.001 |

### Non-student (105 cases; day held at 4 WEDNESDAY)

| ID | Amount | Balance | Student | Day of week | Spec rule | Rate | Expected fee |
|---|---|---|---|---|---|---|---|
| WD-N-001 | $19.00 | -$0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 0.057 |
| WD-N-002 | $20.00 | -$0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 0.06 |
| WD-N-003 | $21.00 | -$0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 0.063 |
| WD-N-004 | $510.00 | -$0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 1.53 |
| WD-N-005 | $999.00 | -$0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 2.997 |
| WD-N-006 | $1,000.00 | -$0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 3 |
| WD-N-007 | $1,001.00 | -$0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 3.003 |
| WD-N-008 | $19.00 | $0.00 | no | 4 WEDNESDAY | N1 | 0.3% | 0.057 |
| WD-N-009 | $20.00 | $0.00 | no | 4 WEDNESDAY | N1 | 0.3% | 0.06 |
| WD-N-010 | $21.00 | $0.00 | no | 4 WEDNESDAY | N1 | 0.3% | 0.063 |
| WD-N-011 | $510.00 | $0.00 | no | 4 WEDNESDAY | N1 | 0.3% | 1.53 |
| WD-N-012 | $999.00 | $0.00 | no | 4 WEDNESDAY | N1 | 0.3% | 2.997 |
| WD-N-013 | $1,000.00 | $0.00 | no | 4 WEDNESDAY | N1 | 0.3% | 3 |
| WD-N-014 | $1,001.00 | $0.00 | no | 4 WEDNESDAY | N1 | 0.3% | 3.003 |
| WD-N-015 | $19.00 | $0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 0.057 |
| WD-N-016 | $20.00 | $0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 0.06 |
| WD-N-017 | $21.00 | $0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 0.063 |
| WD-N-018 | $510.00 | $0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 1.53 |
| WD-N-019 | $999.00 | $0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 2.997 |
| WD-N-020 | $1,000.00 | $0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 3 |
| WD-N-021 | $1,001.00 | $0.01 | no | 4 WEDNESDAY | N1 | 0.3% | 3.003 |
| WD-N-022 | $19.00 | $500.00 | no | 4 WEDNESDAY | N1 | 0.3% | 0.057 |
| WD-N-023 | $20.00 | $500.00 | no | 4 WEDNESDAY | N1 | 0.3% | 0.06 |
| WD-N-024 | $21.00 | $500.00 | no | 4 WEDNESDAY | N1 | 0.3% | 0.063 |
| WD-N-025 | $510.00 | $500.00 | no | 4 WEDNESDAY | N1 | 0.3% | 1.53 |
| WD-N-026 | $999.00 | $500.00 | no | 4 WEDNESDAY | N1 | 0.3% | 2.997 |
| WD-N-027 | $1,000.00 | $500.00 | no | 4 WEDNESDAY | N1 | 0.3% | 3 |
| WD-N-028 | $1,001.00 | $500.00 | no | 4 WEDNESDAY | N1 | 0.3% | 3.003 |
| WD-N-029 | $19.00 | $999.99 | no | 4 WEDNESDAY | N1 | 0.3% | 0.057 |
| WD-N-030 | $20.00 | $999.99 | no | 4 WEDNESDAY | N1 | 0.3% | 0.06 |
| WD-N-031 | $21.00 | $999.99 | no | 4 WEDNESDAY | N1 | 0.3% | 0.063 |
| WD-N-032 | $510.00 | $999.99 | no | 4 WEDNESDAY | N1 | 0.3% | 1.53 |
| WD-N-033 | $999.00 | $999.99 | no | 4 WEDNESDAY | N1 | 0.3% | 2.997 |
| WD-N-034 | $1,000.00 | $999.99 | no | 4 WEDNESDAY | N1 | 0.3% | 3 |
| WD-N-035 | $1,001.00 | $999.99 | no | 4 WEDNESDAY | N1 | 0.3% | 3.003 |
| WD-N-036 | $19.00 | $1,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 0.019 |
| WD-N-037 | $20.00 | $1,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 0.02 |
| WD-N-038 | $21.00 | $1,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 0.021 |
| WD-N-039 | $510.00 | $1,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 0.51 |
| WD-N-040 | $999.00 | $1,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 0.999 |
| WD-N-041 | $1,000.00 | $1,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 1 |
| WD-N-042 | $1,001.00 | $1,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 1.001 |
| WD-N-043 | $19.00 | $1,000.01 | no | 4 WEDNESDAY | N2 | 0.1% | 0.019 |
| WD-N-044 | $20.00 | $1,000.01 | no | 4 WEDNESDAY | N2 | 0.1% | 0.02 |
| WD-N-045 | $21.00 | $1,000.01 | no | 4 WEDNESDAY | N2 | 0.1% | 0.021 |
| WD-N-046 | $510.00 | $1,000.01 | no | 4 WEDNESDAY | N2 | 0.1% | 0.51 |
| WD-N-047 | $999.00 | $1,000.01 | no | 4 WEDNESDAY | N2 | 0.1% | 0.999 |
| WD-N-048 | $1,000.00 | $1,000.01 | no | 4 WEDNESDAY | N2 | 0.1% | 1 |
| WD-N-049 | $1,001.00 | $1,000.01 | no | 4 WEDNESDAY | N2 | 0.1% | 1.001 |
| WD-N-050 | $19.00 | $3,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 0.019 |
| WD-N-051 | $20.00 | $3,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 0.02 |
| WD-N-052 | $21.00 | $3,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 0.021 |
| WD-N-053 | $510.00 | $3,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 0.51 |
| WD-N-054 | $999.00 | $3,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 0.999 |
| WD-N-055 | $1,000.00 | $3,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 1 |
| WD-N-056 | $1,001.00 | $3,000.00 | no | 4 WEDNESDAY | N2 | 0.1% | 1.001 |
| WD-N-057 | $19.00 | $4,999.99 | no | 4 WEDNESDAY | N2 | 0.1% | 0.019 |
| WD-N-058 | $20.00 | $4,999.99 | no | 4 WEDNESDAY | N2 | 0.1% | 0.02 |
| WD-N-059 | $21.00 | $4,999.99 | no | 4 WEDNESDAY | N2 | 0.1% | 0.021 |
| WD-N-060 | $510.00 | $4,999.99 | no | 4 WEDNESDAY | N2 | 0.1% | 0.51 |
| WD-N-061 | $999.00 | $4,999.99 | no | 4 WEDNESDAY | N2 | 0.1% | 0.999 |
| WD-N-062 | $1,000.00 | $4,999.99 | no | 4 WEDNESDAY | N2 | 0.1% | 1 |
| WD-N-063 | $1,001.00 | $4,999.99 | no | 4 WEDNESDAY | N2 | 0.1% | 1.001 |
| WD-N-064 | $19.00 | $5,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-065 | $20.00 | $5,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-066 | $21.00 | $5,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-067 | $510.00 | $5,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-068 | $999.00 | $5,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-069 | $1,000.00 | $5,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-070 | $1,001.00 | $5,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-071 | $19.00 | $5,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-072 | $20.00 | $5,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-073 | $21.00 | $5,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-074 | $510.00 | $5,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-075 | $999.00 | $5,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-076 | $1,000.00 | $5,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-077 | $1,001.00 | $5,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-078 | $19.00 | $52,500.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-079 | $20.00 | $52,500.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-080 | $21.00 | $52,500.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-081 | $510.00 | $52,500.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-082 | $999.00 | $52,500.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-083 | $1,000.00 | $52,500.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-084 | $1,001.00 | $52,500.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-085 | $19.00 | $99,999.99 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-086 | $20.00 | $99,999.99 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-087 | $21.00 | $99,999.99 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-088 | $510.00 | $99,999.99 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-089 | $999.00 | $99,999.99 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-090 | $1,000.00 | $99,999.99 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-091 | $1,001.00 | $99,999.99 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-092 | $19.00 | $100,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-093 | $20.00 | $100,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-094 | $21.00 | $100,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-095 | $510.00 | $100,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-096 | $999.00 | $100,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-097 | $1,000.00 | $100,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-098 | $1,001.00 | $100,000.00 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-099 | $19.00 | $100,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-100 | $20.00 | $100,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-101 | $21.00 | $100,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-102 | $510.00 | $100,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-103 | $999.00 | $100,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-104 | $1,000.00 | $100,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |
| WD-N-105 | $1,001.00 | $100,000.01 | no | 4 WEDNESDAY | N3 | 0% | 0.0 |

## Test results

**Summary:** 154 test cases, 70 passed, **84 failed**. Student: 14/49 passed. Non-student: 56/105 passed.

### Student

| ID | Expected fee | Actual fee | Result |
|---|---|---|---|
| WD-S-01 | 0.019 | 0.019 | Pass |
| WD-S-02 | 0.019 | 0.0 | **Fail** |
| WD-S-03 | 0.0 | 0.019 | **Fail** |
| WD-S-04 | 0.0 | 0.019 | **Fail** |
| WD-S-05 | 0.0 | 0.019 | **Fail** |
| WD-S-06 | 0.019 | 0.0 | **Fail** |
| WD-S-07 | 0.019 | 0.019 | Pass |
| WD-S-08 | 0.02 | 0.02 | Pass |
| WD-S-09 | 0.02 | 0.0 | **Fail** |
| WD-S-10 | 0.0 | 0.02 | **Fail** |
| WD-S-11 | 0.0 | 0.02 | **Fail** |
| WD-S-12 | 0.0 | 0.02 | **Fail** |
| WD-S-13 | 0.02 | 0.0 | **Fail** |
| WD-S-14 | 0.02 | 0.02 | Pass |
| WD-S-15 | 0.021 | 0.021 | Pass |
| WD-S-16 | 0.021 | 0.0 | **Fail** |
| WD-S-17 | 0.0 | 0.021 | **Fail** |
| WD-S-18 | 0.0 | 0.021 | **Fail** |
| WD-S-19 | 0.0 | 0.021 | **Fail** |
| WD-S-20 | 0.021 | 0.0 | **Fail** |
| WD-S-21 | 0.021 | 0.021 | Pass |
| WD-S-22 | 0.51 | 0.51 | Pass |
| WD-S-23 | 0.51 | 0.0 | **Fail** |
| WD-S-24 | 0.0 | 0.51 | **Fail** |
| WD-S-25 | 0.0 | 0.51 | **Fail** |
| WD-S-26 | 0.0 | 0.51 | **Fail** |
| WD-S-27 | 0.51 | 0.0 | **Fail** |
| WD-S-28 | 0.51 | 0.51 | Pass |
| WD-S-29 | 0.999 | 0.999 | Pass |
| WD-S-30 | 0.999 | 0.0 | **Fail** |
| WD-S-31 | 0.0 | 0.999 | **Fail** |
| WD-S-32 | 0.0 | 0.999 | **Fail** |
| WD-S-33 | 0.0 | 0.999 | **Fail** |
| WD-S-34 | 0.999 | 0.0 | **Fail** |
| WD-S-35 | 0.999 | 0.999 | Pass |
| WD-S-36 | 1 | 1.0 | Pass |
| WD-S-37 | 1 | 0.0 | **Fail** |
| WD-S-38 | 0.0 | 1.0 | **Fail** |
| WD-S-39 | 0.0 | 1.0 | **Fail** |
| WD-S-40 | 0.0 | 1.0 | **Fail** |
| WD-S-41 | 1 | 0.0 | **Fail** |
| WD-S-42 | 1 | 1.0 | Pass |
| WD-S-43 | 1.001 | 1.0010000000000001 | Pass |
| WD-S-44 | 1.001 | 0.0 | **Fail** |
| WD-S-45 | 0.0 | 1.0010000000000001 | **Fail** |
| WD-S-46 | 0.0 | 1.0010000000000001 | **Fail** |
| WD-S-47 | 0.0 | 1.0010000000000001 | **Fail** |
| WD-S-48 | 1.001 | 0.0 | **Fail** |
| WD-S-49 | 1.001 | 1.0010000000000001 | Pass |

### Non-student

| ID | Expected fee | Actual fee | Result |
|---|---|---|---|
| WD-N-001 | 0.057 | 0.038 | **Fail** |
| WD-N-002 | 0.06 | 0.04 | **Fail** |
| WD-N-003 | 0.063 | 0.042 | **Fail** |
| WD-N-004 | 1.53 | 1.02 | **Fail** |
| WD-N-005 | 2.997 | 1.998 | **Fail** |
| WD-N-006 | 3 | 2.0 | **Fail** |
| WD-N-007 | 3.003 | 2.0020000000000002 | **Fail** |
| WD-N-008 | 0.057 | 0.038 | **Fail** |
| WD-N-009 | 0.06 | 0.04 | **Fail** |
| WD-N-010 | 0.063 | 0.042 | **Fail** |
| WD-N-011 | 1.53 | 1.02 | **Fail** |
| WD-N-012 | 2.997 | 1.998 | **Fail** |
| WD-N-013 | 3 | 2.0 | **Fail** |
| WD-N-014 | 3.003 | 2.0020000000000002 | **Fail** |
| WD-N-015 | 0.057 | 0.038 | **Fail** |
| WD-N-016 | 0.06 | 0.04 | **Fail** |
| WD-N-017 | 0.063 | 0.042 | **Fail** |
| WD-N-018 | 1.53 | 1.02 | **Fail** |
| WD-N-019 | 2.997 | 1.998 | **Fail** |
| WD-N-020 | 3 | 2.0 | **Fail** |
| WD-N-021 | 3.003 | 2.0020000000000002 | **Fail** |
| WD-N-022 | 0.057 | 0.038 | **Fail** |
| WD-N-023 | 0.06 | 0.04 | **Fail** |
| WD-N-024 | 0.063 | 0.042 | **Fail** |
| WD-N-025 | 1.53 | 1.02 | **Fail** |
| WD-N-026 | 2.997 | 1.998 | **Fail** |
| WD-N-027 | 3 | 2.0 | **Fail** |
| WD-N-028 | 3.003 | 2.0020000000000002 | **Fail** |
| WD-N-029 | 0.057 | 0.038 | **Fail** |
| WD-N-030 | 0.06 | 0.04 | **Fail** |
| WD-N-031 | 0.063 | 0.042 | **Fail** |
| WD-N-032 | 1.53 | 1.02 | **Fail** |
| WD-N-033 | 2.997 | 1.998 | **Fail** |
| WD-N-034 | 3 | 2.0 | **Fail** |
| WD-N-035 | 3.003 | 2.0020000000000002 | **Fail** |
| WD-N-036 | 0.019 | 0.019 | Pass |
| WD-N-037 | 0.02 | 0.02 | Pass |
| WD-N-038 | 0.021 | 0.021 | Pass |
| WD-N-039 | 0.51 | 0.51 | Pass |
| WD-N-040 | 0.999 | 0.999 | Pass |
| WD-N-041 | 1 | 1.0 | Pass |
| WD-N-042 | 1.001 | 1.0010000000000001 | Pass |
| WD-N-043 | 0.019 | 0.019 | Pass |
| WD-N-044 | 0.02 | 0.02 | Pass |
| WD-N-045 | 0.021 | 0.021 | Pass |
| WD-N-046 | 0.51 | 0.51 | Pass |
| WD-N-047 | 0.999 | 0.999 | Pass |
| WD-N-048 | 1 | 1.0 | Pass |
| WD-N-049 | 1.001 | 1.0010000000000001 | Pass |
| WD-N-050 | 0.019 | 0.019 | Pass |
| WD-N-051 | 0.02 | 0.02 | Pass |
| WD-N-052 | 0.021 | 0.021 | Pass |
| WD-N-053 | 0.51 | 0.51 | Pass |
| WD-N-054 | 0.999 | 0.999 | Pass |
| WD-N-055 | 1 | 1.0 | Pass |
| WD-N-056 | 1.001 | 1.0010000000000001 | Pass |
| WD-N-057 | 0.019 | 0.019 | Pass |
| WD-N-058 | 0.02 | 0.02 | Pass |
| WD-N-059 | 0.021 | 0.021 | Pass |
| WD-N-060 | 0.51 | 0.51 | Pass |
| WD-N-061 | 0.999 | 0.999 | Pass |
| WD-N-062 | 1 | 1.0 | Pass |
| WD-N-063 | 1.001 | 1.0010000000000001 | Pass |
| WD-N-064 | 0.0 | 0.019 | **Fail** |
| WD-N-065 | 0.0 | 0.02 | **Fail** |
| WD-N-066 | 0.0 | 0.021 | **Fail** |
| WD-N-067 | 0.0 | 0.51 | **Fail** |
| WD-N-068 | 0.0 | 0.999 | **Fail** |
| WD-N-069 | 0.0 | 1.0 | **Fail** |
| WD-N-070 | 0.0 | 1.0010000000000001 | **Fail** |
| WD-N-071 | 0.0 | 0.019 | **Fail** |
| WD-N-072 | 0.0 | 0.02 | **Fail** |
| WD-N-073 | 0.0 | 0.021 | **Fail** |
| WD-N-074 | 0.0 | 0.51 | **Fail** |
| WD-N-075 | 0.0 | 0.999 | **Fail** |
| WD-N-076 | 0.0 | 1.0 | **Fail** |
| WD-N-077 | 0.0 | 1.0010000000000001 | **Fail** |
| WD-N-078 | 0.0 | 0.0 | Pass |
| WD-N-079 | 0.0 | 0.0 | Pass |
| WD-N-080 | 0.0 | 0.0 | Pass |
| WD-N-081 | 0.0 | 0.0 | Pass |
| WD-N-082 | 0.0 | 0.0 | Pass |
| WD-N-083 | 0.0 | 0.0 | Pass |
| WD-N-084 | 0.0 | 0.0 | Pass |
| WD-N-085 | 0.0 | 0.0 | Pass |
| WD-N-086 | 0.0 | 0.0 | Pass |
| WD-N-087 | 0.0 | 0.0 | Pass |
| WD-N-088 | 0.0 | 0.0 | Pass |
| WD-N-089 | 0.0 | 0.0 | Pass |
| WD-N-090 | 0.0 | 0.0 | Pass |
| WD-N-091 | 0.0 | 0.0 | Pass |
| WD-N-092 | 0.0 | 0.0 | Pass |
| WD-N-093 | 0.0 | 0.0 | Pass |
| WD-N-094 | 0.0 | 0.0 | Pass |
| WD-N-095 | 0.0 | 0.0 | Pass |
| WD-N-096 | 0.0 | 0.0 | Pass |
| WD-N-097 | 0.0 | 0.0 | Pass |
| WD-N-098 | 0.0 | 0.0 | Pass |
| WD-N-099 | 0.0 | 0.0 | Pass |
| WD-N-100 | 0.0 | 0.0 | Pass |
| WD-N-101 | 0.0 | 0.0 | Pass |
| WD-N-102 | 0.0 | 0.0 | Pass |
| WD-N-103 | 0.0 | 0.0 | Pass |
| WD-N-104 | 0.0 | 0.0 | Pass |
| WD-N-105 | 0.0 | 0.0 | Pass |

## Fault localization

| Fault | Failing tests | Suspected fault (file, method, line) | Justification |
|---|---|---|---|
| W-F1 | 35 tests: WD-S-02, WD-S-03, WD-S-04, WD-S-05, WD-S-06, WD-S-09, WD-S-10, WD-S-11, WD-S-12, WD-S-13, WD-S-16, WD-S-17, WD-S-18, WD-S-19, WD-S-20, WD-S-23, WD-S-24, WD-S-25, WD-S-26, WD-S-27, WD-S-30, WD-S-31, WD-S-32, WD-S-33, WD-S-34, WD-S-37, WD-S-38, WD-S-39, WD-S-40, WD-S-41, WD-S-44, WD-S-45, WD-S-46, WD-S-47, WD-S-48 | `FeesCalculator.java`, `calculateWithdrawalFee`, line 12: `if ((dayOfWeek == Calendar.SATURDAY) \|\| (dayOfWeek == Calendar.SUNDAY))` gives 0% on weekends and 0.1% otherwise | Every student weekday case (days 2, 4, 6) expected no fee but was charged 0.1%, and every weekend case (days 1, 7) expected 0.1% but was charged nothing; only the out-of-range days 0 and 8 pass, so the weekday/weekend branches are swapped. |
| W-F2 | 35 tests: WD-N-001, WD-N-002, WD-N-003, WD-N-004, WD-N-005, WD-N-006, WD-N-007, WD-N-008, WD-N-009, WD-N-010, WD-N-011, WD-N-012, WD-N-013, WD-N-014, WD-N-015, WD-N-016, WD-N-017, WD-N-018, WD-N-019, WD-N-020, WD-N-021, WD-N-022, WD-N-023, WD-N-024, WD-N-025, WD-N-026, WD-N-027, WD-N-028, WD-N-029, WD-N-030, WD-N-031, WD-N-032, WD-N-033, WD-N-034, WD-N-035 | `FeesCalculator.java`, `calculateWithdrawalFee`, line 19: `feePercentage = 0.002;` | Every non-student case with balance below $1,000 (including -$0.01) returned 0.2% of the amount instead of the specified 0.3%, while the balance boundaries themselves behave correctly, so the rate constant is wrong. |
| W-F3 | 14 tests: WD-N-064, WD-N-065, WD-N-066, WD-N-067, WD-N-068, WD-N-069, WD-N-070, WD-N-071, WD-N-072, WD-N-073, WD-N-074, WD-N-075, WD-N-076, WD-N-077 | `FeesCalculator.java`, `calculateWithdrawalFee`, line 20: `else if (accountBalance < 10000.00)` | Balances of $5,000.00 and $5,000.01 were charged 0.1% instead of no fee, while $4,999.99 (0.1%) and $52,500.00 and above (no fee) are correct, so the upper threshold of the 0.1% band is set above $5,000: the code uses $10,000 instead of $5,000. |

Limit of this suite for W-F3: the black box cases show that the 0.1% band wrongly continues past $5,000 and has ended by $52,500 (the next sampled balance), but they cannot pin down where it ends. The $10,000 value comes from reading the code during fault localization. Moving the nominal value to catch it would mean choosing test values from the code, which black box testing does not allow.

## Failure log (keep for Assignment 3)

Fill in the times from your own testing session.

| Failure ID | Exposed by (first failing test) | Time spent testing before found | Time spent diagnosing |
|---|---|---|---|
| W-F1 | WD-S-02 | | |
| W-F2 | WD-N-001 | | |
| W-F3 | WD-N-064 | | |
