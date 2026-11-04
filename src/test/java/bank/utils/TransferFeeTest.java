package bank.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the transfer fee, made with a decision table.
 * Each row below is one rule (R1 to R16) from the table in docs/assignment1.md.
 * The rules come from the "Transfer" part of the assignment.
 */
class TransferFeeTest {
	// How close the answer must be, since decimal numbers are not exact.
	private static final double DELTA = 0.0001;

	private final FeesCalculator calculator = new FeesCalculator();

	@ParameterizedTest(name = "{0}: amount={1}, from={2}, to={3}, student={4} -> fee {5}")
	@CsvSource({
		// Student, sends less than $200. Limits: from $2000, to $1000
		"R1,  150, 1000,  500, true,  0.15",
		"R2,  150, 1000, 1500, true,  0.075",
		"R3,  150, 3000,  500, true,  0.075",
		"R4,  150, 3000, 1500, true,  0.0375",
		// Student, sends $200 or more. Limits: from $2000, to $1000
		"R5,  500, 1000,  500, true,  0.25",
		"R6,  500, 1000, 1500, true,  0.125",
		"R7,  500, 3000,  500, true,  1.25",
		"R8,  500, 3000, 1500, true,  0.625",
		// Not a student, sends less than $100. Limits: from $4000, to $2000
		"R9,   50, 3000, 1500, false, 0.1",
		"R10,  50, 3000, 2500, false, 0.05",
		"R11,  50, 5000, 1500, false, 0.5",
		"R12,  50, 5000, 2500, false, 0.25",
		// Not a student, sends $100 or more. Limits: from $2000, to $1000
		"R13, 150, 1000,  500, false, 0.3",
		"R14, 150, 1000, 1500, false, 0.15",
		"R15, 150, 3000,  500, false, 0.75",
		"R16, 150, 3000, 1500, false, 0.375"
	})
	void transferFeeDecisionTable(String rule, double amount, double fromBalance, double toBalance,
			boolean student, double expectedFee) {
		assertEquals(expectedFee, calculator.calculateTransferFee(amount, fromBalance, toBalance, student), DELTA,
				"Rule " + rule);
	}
}
