package bank.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Assignment 1: black box tests for {@link FeesCalculator#calculateWithdrawalFee}.
 *
 * <p>Technique: Robust Worst Case Boundary Value Analysis.
 * Specification: the "Withdrawal" section of the Assignment 1 handout.
 * Test case design, assumptions and the expected-value derivation are in
 * docs/assignment1/withdrawal.md; every row below is one test case from those tables.
 *
 * <p>Row format: id, amount, accountBalance, student, dayOfWeek (java.util.Calendar), expectedFee.
 */
class FeesCalculatorWithdrawalTest {

	/** Fees are compared as doubles; the smallest spec difference between two rates is $0.019. */
	private static final double DELTA = 1e-6;

	private final FeesCalculator calculator = new FeesCalculator();

	/**
	 * Student client: the fee depends only on the day of the week (spec: weekday, no fee;
	 * otherwise 0.1%). Robust worst case over amount x dayOfWeek, balance held at nominal.
	 */
	@ParameterizedTest(name = "{0}: amount={1}, balance={2}, student={3}, day={4} -> fee {5}")
	@CsvSource({
		"WD-S-01, 19, 3000.00, true, 0, 0.019",
		"WD-S-02, 19, 3000.00, true, 1, 0.019",
		"WD-S-03, 19, 3000.00, true, 2, 0.0",
		"WD-S-04, 19, 3000.00, true, 4, 0.0",
		"WD-S-05, 19, 3000.00, true, 6, 0.0",
		"WD-S-06, 19, 3000.00, true, 7, 0.019",
		"WD-S-07, 19, 3000.00, true, 8, 0.019",
		"WD-S-08, 20, 3000.00, true, 0, 0.02",
		"WD-S-09, 20, 3000.00, true, 1, 0.02",
		"WD-S-10, 20, 3000.00, true, 2, 0.0",
		"WD-S-11, 20, 3000.00, true, 4, 0.0",
		"WD-S-12, 20, 3000.00, true, 6, 0.0",
		"WD-S-13, 20, 3000.00, true, 7, 0.02",
		"WD-S-14, 20, 3000.00, true, 8, 0.02",
		"WD-S-15, 21, 3000.00, true, 0, 0.021",
		"WD-S-16, 21, 3000.00, true, 1, 0.021",
		"WD-S-17, 21, 3000.00, true, 2, 0.0",
		"WD-S-18, 21, 3000.00, true, 4, 0.0",
		"WD-S-19, 21, 3000.00, true, 6, 0.0",
		"WD-S-20, 21, 3000.00, true, 7, 0.021",
		"WD-S-21, 21, 3000.00, true, 8, 0.021",
		"WD-S-22, 510, 3000.00, true, 0, 0.51",
		"WD-S-23, 510, 3000.00, true, 1, 0.51",
		"WD-S-24, 510, 3000.00, true, 2, 0.0",
		"WD-S-25, 510, 3000.00, true, 4, 0.0",
		"WD-S-26, 510, 3000.00, true, 6, 0.0",
		"WD-S-27, 510, 3000.00, true, 7, 0.51",
		"WD-S-28, 510, 3000.00, true, 8, 0.51",
		"WD-S-29, 999, 3000.00, true, 0, 0.999",
		"WD-S-30, 999, 3000.00, true, 1, 0.999",
		"WD-S-31, 999, 3000.00, true, 2, 0.0",
		"WD-S-32, 999, 3000.00, true, 4, 0.0",
		"WD-S-33, 999, 3000.00, true, 6, 0.0",
		"WD-S-34, 999, 3000.00, true, 7, 0.999",
		"WD-S-35, 999, 3000.00, true, 8, 0.999",
		"WD-S-36, 1000, 3000.00, true, 0, 1",
		"WD-S-37, 1000, 3000.00, true, 1, 1",
		"WD-S-38, 1000, 3000.00, true, 2, 0.0",
		"WD-S-39, 1000, 3000.00, true, 4, 0.0",
		"WD-S-40, 1000, 3000.00, true, 6, 0.0",
		"WD-S-41, 1000, 3000.00, true, 7, 1",
		"WD-S-42, 1000, 3000.00, true, 8, 1",
		"WD-S-43, 1001, 3000.00, true, 0, 1.001",
		"WD-S-44, 1001, 3000.00, true, 1, 1.001",
		"WD-S-45, 1001, 3000.00, true, 2, 0.0",
		"WD-S-46, 1001, 3000.00, true, 4, 0.0",
		"WD-S-47, 1001, 3000.00, true, 6, 0.0",
		"WD-S-48, 1001, 3000.00, true, 7, 1.001",
		"WD-S-49, 1001, 3000.00, true, 8, 1.001"
	})
	void studentWithdrawalFee(String id, double amount, double balance, boolean student, int dayOfWeek,
			double expectedFee) {
		assertEquals(expectedFee, calculator.calculateWithdrawalFee(amount, balance, student, dayOfWeek), DELTA, id);
	}

	/**
	 * Non-student client: the fee depends only on the account balance (spec: below $1,000 0.3%;
	 * $1,000 to below $5,000 0.1%; $5,000 or more no fee). Robust worst case over amount x balance,
	 * day held at nominal.
	 */
	@ParameterizedTest(name = "{0}: amount={1}, balance={2}, student={3}, day={4} -> fee {5}")
	@CsvSource({
		"WD-N-001, 19, -0.01, false, 4, 0.057",
		"WD-N-002, 20, -0.01, false, 4, 0.06",
		"WD-N-003, 21, -0.01, false, 4, 0.063",
		"WD-N-004, 510, -0.01, false, 4, 1.53",
		"WD-N-005, 999, -0.01, false, 4, 2.997",
		"WD-N-006, 1000, -0.01, false, 4, 3",
		"WD-N-007, 1001, -0.01, false, 4, 3.003",
		"WD-N-008, 19, 0.00, false, 4, 0.057",
		"WD-N-009, 20, 0.00, false, 4, 0.06",
		"WD-N-010, 21, 0.00, false, 4, 0.063",
		"WD-N-011, 510, 0.00, false, 4, 1.53",
		"WD-N-012, 999, 0.00, false, 4, 2.997",
		"WD-N-013, 1000, 0.00, false, 4, 3",
		"WD-N-014, 1001, 0.00, false, 4, 3.003",
		"WD-N-015, 19, 0.01, false, 4, 0.057",
		"WD-N-016, 20, 0.01, false, 4, 0.06",
		"WD-N-017, 21, 0.01, false, 4, 0.063",
		"WD-N-018, 510, 0.01, false, 4, 1.53",
		"WD-N-019, 999, 0.01, false, 4, 2.997",
		"WD-N-020, 1000, 0.01, false, 4, 3",
		"WD-N-021, 1001, 0.01, false, 4, 3.003",
		"WD-N-022, 19, 500.00, false, 4, 0.057",
		"WD-N-023, 20, 500.00, false, 4, 0.06",
		"WD-N-024, 21, 500.00, false, 4, 0.063",
		"WD-N-025, 510, 500.00, false, 4, 1.53",
		"WD-N-026, 999, 500.00, false, 4, 2.997",
		"WD-N-027, 1000, 500.00, false, 4, 3",
		"WD-N-028, 1001, 500.00, false, 4, 3.003",
		"WD-N-029, 19, 999.99, false, 4, 0.057",
		"WD-N-030, 20, 999.99, false, 4, 0.06",
		"WD-N-031, 21, 999.99, false, 4, 0.063",
		"WD-N-032, 510, 999.99, false, 4, 1.53",
		"WD-N-033, 999, 999.99, false, 4, 2.997",
		"WD-N-034, 1000, 999.99, false, 4, 3",
		"WD-N-035, 1001, 999.99, false, 4, 3.003",
		"WD-N-036, 19, 1000.00, false, 4, 0.019",
		"WD-N-037, 20, 1000.00, false, 4, 0.02",
		"WD-N-038, 21, 1000.00, false, 4, 0.021",
		"WD-N-039, 510, 1000.00, false, 4, 0.51",
		"WD-N-040, 999, 1000.00, false, 4, 0.999",
		"WD-N-041, 1000, 1000.00, false, 4, 1",
		"WD-N-042, 1001, 1000.00, false, 4, 1.001",
		"WD-N-043, 19, 1000.01, false, 4, 0.019",
		"WD-N-044, 20, 1000.01, false, 4, 0.02",
		"WD-N-045, 21, 1000.01, false, 4, 0.021",
		"WD-N-046, 510, 1000.01, false, 4, 0.51",
		"WD-N-047, 999, 1000.01, false, 4, 0.999",
		"WD-N-048, 1000, 1000.01, false, 4, 1",
		"WD-N-049, 1001, 1000.01, false, 4, 1.001",
		"WD-N-050, 19, 3000.00, false, 4, 0.019",
		"WD-N-051, 20, 3000.00, false, 4, 0.02",
		"WD-N-052, 21, 3000.00, false, 4, 0.021",
		"WD-N-053, 510, 3000.00, false, 4, 0.51",
		"WD-N-054, 999, 3000.00, false, 4, 0.999",
		"WD-N-055, 1000, 3000.00, false, 4, 1",
		"WD-N-056, 1001, 3000.00, false, 4, 1.001",
		"WD-N-057, 19, 4999.99, false, 4, 0.019",
		"WD-N-058, 20, 4999.99, false, 4, 0.02",
		"WD-N-059, 21, 4999.99, false, 4, 0.021",
		"WD-N-060, 510, 4999.99, false, 4, 0.51",
		"WD-N-061, 999, 4999.99, false, 4, 0.999",
		"WD-N-062, 1000, 4999.99, false, 4, 1",
		"WD-N-063, 1001, 4999.99, false, 4, 1.001",
		"WD-N-064, 19, 5000.00, false, 4, 0.0",
		"WD-N-065, 20, 5000.00, false, 4, 0.0",
		"WD-N-066, 21, 5000.00, false, 4, 0.0",
		"WD-N-067, 510, 5000.00, false, 4, 0.0",
		"WD-N-068, 999, 5000.00, false, 4, 0.0",
		"WD-N-069, 1000, 5000.00, false, 4, 0.0",
		"WD-N-070, 1001, 5000.00, false, 4, 0.0",
		"WD-N-071, 19, 5000.01, false, 4, 0.0",
		"WD-N-072, 20, 5000.01, false, 4, 0.0",
		"WD-N-073, 21, 5000.01, false, 4, 0.0",
		"WD-N-074, 510, 5000.01, false, 4, 0.0",
		"WD-N-075, 999, 5000.01, false, 4, 0.0",
		"WD-N-076, 1000, 5000.01, false, 4, 0.0",
		"WD-N-077, 1001, 5000.01, false, 4, 0.0",
		"WD-N-078, 19, 52500.00, false, 4, 0.0",
		"WD-N-079, 20, 52500.00, false, 4, 0.0",
		"WD-N-080, 21, 52500.00, false, 4, 0.0",
		"WD-N-081, 510, 52500.00, false, 4, 0.0",
		"WD-N-082, 999, 52500.00, false, 4, 0.0",
		"WD-N-083, 1000, 52500.00, false, 4, 0.0",
		"WD-N-084, 1001, 52500.00, false, 4, 0.0",
		"WD-N-085, 19, 99999.99, false, 4, 0.0",
		"WD-N-086, 20, 99999.99, false, 4, 0.0",
		"WD-N-087, 21, 99999.99, false, 4, 0.0",
		"WD-N-088, 510, 99999.99, false, 4, 0.0",
		"WD-N-089, 999, 99999.99, false, 4, 0.0",
		"WD-N-090, 1000, 99999.99, false, 4, 0.0",
		"WD-N-091, 1001, 99999.99, false, 4, 0.0",
		"WD-N-092, 19, 100000.00, false, 4, 0.0",
		"WD-N-093, 20, 100000.00, false, 4, 0.0",
		"WD-N-094, 21, 100000.00, false, 4, 0.0",
		"WD-N-095, 510, 100000.00, false, 4, 0.0",
		"WD-N-096, 999, 100000.00, false, 4, 0.0",
		"WD-N-097, 1000, 100000.00, false, 4, 0.0",
		"WD-N-098, 1001, 100000.00, false, 4, 0.0",
		"WD-N-099, 19, 100000.01, false, 4, 0.0",
		"WD-N-100, 20, 100000.01, false, 4, 0.0",
		"WD-N-101, 21, 100000.01, false, 4, 0.0",
		"WD-N-102, 510, 100000.01, false, 4, 0.0",
		"WD-N-103, 999, 100000.01, false, 4, 0.0",
		"WD-N-104, 1000, 100000.01, false, 4, 0.0",
		"WD-N-105, 1001, 100000.01, false, 4, 0.0"
	})
	void nonStudentWithdrawalFee(String id, double amount, double balance, boolean student, int dayOfWeek,
			double expectedFee) {
		assertEquals(expectedFee, calculator.calculateWithdrawalFee(amount, balance, student, dayOfWeek), DELTA, id);
	}
}
