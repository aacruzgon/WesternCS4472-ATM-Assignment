package bank.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DepositFeesTest {
	private static final double DELTA = 0.000001;
	private final FeesCalculator calculator = new FeesCalculator();

	// Tests every valid deposit fee group from the assignment.
	@ParameterizedTest(name = "{0}")
	@MethodSource("validDepositFeeCases")
	void calculatesFeeForValidEquivalenceClasses(
			String testCase,
			double amount,
			double balance,
			boolean student,
			double expectedFee) {
		double actualFee = calculator.calculateDepositInterest(amount, balance, student);

		assertEquals(expectedFee, actualFee, DELTA);
	}
	
	// Tests that a negative deposit amount is rejected.
	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidAmountCases")
	void rejectsInvalidAmountEquivalenceClass(
			String testCase,
			double amount,
			double balance,
			boolean student) {
		assertThrows(
				IllegalArgumentException.class,
				() -> calculator.calculateDepositInterest(amount, balance, student));
	}

	// Tests that a negative account balance is rejected.
	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidBalanceCases")
	void rejectsInvalidBalanceEquivalenceClass(
			String testCase,
			double amount,
			double balance,
			boolean student) {
		assertThrows(
				IllegalArgumentException.class,
				() -> calculator.calculateDepositInterest(amount, balance, student));
	}

	private static Stream<Arguments> validDepositFeeCases() {
		return Stream.of(
				Arguments.of("DEP-WREC-01", 100.00, 1000.00, true, 0.50),
				Arguments.of("DEP-WREC-02", 100.00, 500.00, true, 0.25),
				Arguments.of("DEP-WREC-03", 25.00, 6000.00, true, 0.125),
				Arguments.of("DEP-WREC-04", 25.00, 5000.00, true, 0.00),
				Arguments.of("DEP-WREC-05", 500.00, 3000.00, false, 4.00),
				Arguments.of("DEP-WREC-06", 500.00, 2500.00, false, 2.00),
				Arguments.of("DEP-WREC-07", 100.00, 11000.00, false, 0.00),
				Arguments.of("DEP-WREC-08", 100.00, 10000.00, false, 0.10));
	}

	private static Stream<Arguments> invalidAmountCases() {
		return Stream.of(
				Arguments.of("DEP-WREC-09", -1.00, 1000.00, true));
	}

	private static Stream<Arguments> invalidBalanceCases() {
		return Stream.of(
				Arguments.of("DEP-WREC-10", 100.00, -1.00, true));
	}
}
