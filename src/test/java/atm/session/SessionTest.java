package atm.session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import atm.ATM;
import atm.dispatcher.MessageDispatcher;
import atm.exceptions.InvalidAmountException;
import atm.exceptions.InvalidPinFormatException;
import atm.fixtures.SeededAccount;
import atm.session.transactions.ATMTransaction;
import atm.ui.panels.MainPanel;
import bank.transactions.utils.TransactionType;

/**
 * Tests for the ATM Session part of the assignment: the PIN format and the
 * withdrawal amount. The technique is Robust Worst Case Boundary Value Analysis
 * with the single fault assumption.
 *
 * The values we test:
 * <pre>
 *          too low lowest  next    middle   next    highest too high
 * PIN      0000    00000   00001   55555    99998   99999   100000
 * Amount   10      20      40      500      990     1000    1010
 * </pre>
 *
 * Amounts go up in steps of $10. The next amount after $20 is $40 because $30
 * cannot be made from $20 and $50 bills.
 *
 * Trying every PIN with every amount gives 7 x 7 = 49 cases. We only allow one
 * bad input per test, so the 4 cases with a bad PIN and a bad amount are
 * dropped, which leaves 45:
 * <ul>
 * <li>25 with a good PIN and a good amount: {@link #validPinAndAmountCompleteWithdrawal}</li>
 * <li>10 with a good PIN and a bad amount: {@link #outOfRangeAmountIsRejected}</li>
 * <li>10 with a bad PIN and a good amount: {@link #outOfRangePinIsRejected}. The
 * ATM stops at a bad PIN and never asks for an amount, so changing the amount
 * makes no difference. We run each bad PIN once, which is 2 tests.</li>
 * </ul>
 *
 * The other tests cover cases the table misses: PINs with non-digits, and
 * amounts between the lowest and highest values.
 *
 * Session does not check the PIN format. ATM.checkPin does, so the PIN tests go
 * through ATM. The amount tests use Session directly.
 *
 * Team assumptions:
 * <ul>
 * <li>Withdrawal amounts go from $20 to $1,000. Anything else throws
 * InvalidAmountException.</li>
 * <li>Anything a test is not checking stays the same: the balance is always
 * $5,000, and tests outside the table use PIN 55555.</li>
 * <li>The $100,000 balance maximum is not used here, because the assignment
 * sets this account's balance to $5,000.</li>
 * </ul>
 */
class SessionTest {
	private static final List<String> VALID_PINS = Arrays.asList("00000", "00001", "55555", "99998", "99999");
	private static final List<Integer> VALID_AMOUNTS = Arrays.asList(20, 40, 500, 990, 1000);
	private static final List<Integer> OUT_OF_RANGE_AMOUNTS = Arrays.asList(10, 1010);

	private MainPanel mainPanel;
	private MessageDispatcher dispatcher;

	@BeforeEach
	void setUp() {
		mainPanel = SeededAccount.mainPanel();
		dispatcher = SeededAccount.dispatcher();
	}

	static Stream<String> validPins() {
		return VALID_PINS.stream();
	}

	static Stream<Arguments> validPinsAndAmounts() {
		return pinsCrossedWith(VALID_AMOUNTS);
	}

	static Stream<Arguments> validPinsAndOutOfRangeAmounts() {
		return pinsCrossedWith(OUT_OF_RANGE_AMOUNTS);
	}

	private static Stream<Arguments> pinsCrossedWith(List<Integer> amounts) {
		return VALID_PINS.stream().flatMap(pin -> amounts.stream().map(amount -> Arguments.of(pin, amount)));
	}

	// Spec, ATM Session: "A valid PIN is 5 digits."
	@ParameterizedTest(name = "PIN {0} is accepted")
	@MethodSource("validPins")
	void validPinIsAccepted(String pin) throws Exception {
		ATM atm = SeededAccount.atmAtPinPrompt(mainPanel, dispatcher);

		assertDoesNotThrow(() -> atm.checkPin(pin.toCharArray()));

		Mockito.verify(dispatcher).checkCredentials(SeededAccount.CARD, pin.toCharArray());
		Mockito.verify(mainPanel).changeLayout("Transaction");
	}

	// Spec, ATM Session: "Invalid PIN format: throws InvalidPinFormatException."
	@ParameterizedTest(name = "PIN {0} is rejected")
	@ValueSource(strings = {"0000", "100000"})
	void outOfRangePinIsRejected(String pin) throws Exception {
		assertPinRejected(pin);
	}

	// Spec, ATM Session: a PIN must have exactly 5 digits, so any other length is rejected.
	@ParameterizedTest(name = "PIN \"{0}\" has the wrong length")
	@ValueSource(strings = {"", "5", "5555", "9999", "555555", "999999"})
	void pinOfWrongLengthIsRejected(String pin) throws Exception {
		assertPinRejected(pin);
	}

	// Spec, ATM Session: a PIN can only contain digits. '/' comes right before '0'
	// and ':' comes right after '9' in the character table.
	@ParameterizedTest(name = "PIN {0} contains a non-digit")
	@ValueSource(strings = {"/5555", "5555/", ":5555", "5555:", "a5555", "5555a"})
	void pinWithNonDigitIsRejected(String pin) throws Exception {
		assertPinRejected(pin);
	}

	// Spec, ATM Session and Testing Targets: valid amounts up to the $1,000 daily limit are paid out.
	@ParameterizedTest(name = "PIN {0}, amount {1}")
	@MethodSource("validPinsAndAmounts")
	void validPinAndAmountCompleteWithdrawal(String pin, int amount) {
		assertWithdrawalCompletes(pin, amount);
	}

	// Spec, ATM Session: "Invalid amount: throws InvalidAmountException."
	@ParameterizedTest(name = "PIN {0}, amount {1}")
	@MethodSource("validPinsAndOutOfRangeAmounts")
	void outOfRangeAmountIsRejected(String pin, int amount) {
		assertAmountRejected(pin, amount);
	}

	// Spec, ATM Session: all of these can be made from $20 and $50 bills, using
	// only 20s (60, 80), only 50s (50) or both (70, 110, 130, 960, 980).
	@ParameterizedTest(name = "amount {0}")
	@ValueSource(ints = {50, 60, 70, 80, 110, 130, 960, 980})
	void dispensableAmountCompletesWithdrawal(int amount) {
		assertWithdrawalCompletes(SeededAccount.PIN, amount);
	}

	// Spec, ATM Session: these amounts cannot be made from $20 and $50 bills.
	@ParameterizedTest(name = "amount {0}")
	@ValueSource(ints = {19, 21, 30, 999, 1001})
	void undispensableAmountIsRejected(int amount) {
		assertAmountRejected(SeededAccount.PIN, amount);
	}

	// Spec, Testing Targets: "The daily limit of $1,000 is your upper boundary."
	// These can be made from bills, so only a daily limit check can reject them.
	@ParameterizedTest(name = "amount {0}")
	@ValueSource(ints = {1020, 1050})
	void dispensableAmountOverDailyLimitIsRejected(int amount) {
		assertAmountRejected(SeededAccount.PIN, amount);
	}

	// Spec, ATM Session: these are below the team's $20 minimum.
	@ParameterizedTest(name = "amount {0}")
	@ValueSource(ints = {0, -20, -50})
	void nonPositiveAmountIsRejected(int amount) {
		assertAmountRejected(SeededAccount.PIN, amount);
	}

	private void assertPinRejected(String pin) throws Exception {
		ATM atm = SeededAccount.atmAtPinPrompt(mainPanel, dispatcher);

		assertThrows(InvalidPinFormatException.class, () -> atm.checkPin(pin.toCharArray()));

		Mockito.verify(dispatcher, Mockito.never()).checkCredentials(ArgumentMatchers.any(), ArgumentMatchers.any());
	}

	private void assertWithdrawalCompletes(String pin, int amount) {
		Session session = SeededAccount.sessionAtAmountPrompt(mainPanel, dispatcher, pin);

		assertDoesNotThrow(() -> session.setAmount(amount));

		ArgumentCaptor<ATMTransaction> sent = ArgumentCaptor.forClass(ATMTransaction.class);
		Mockito.verify(dispatcher).performTransaction(ArgumentMatchers.eq(SeededAccount.CARD),
				AdditionalMatchers.aryEq(pin.toCharArray()), sent.capture());
		assertEquals(TransactionType.Withdrawal, sent.getValue().getTransactionType());
		assertEquals(amount, sent.getValue().getAmount());
		Mockito.verify(mainPanel).changeLayout("Result");
	}

	private void assertAmountRejected(String pin, int amount) {
		Session session = SeededAccount.sessionAtAmountPrompt(mainPanel, dispatcher, pin);

		assertThrows(InvalidAmountException.class, () -> session.setAmount(amount));

		Mockito.verify(dispatcher, Mockito.never()).performTransaction(ArgumentMatchers.any(),
				ArgumentMatchers.any(), ArgumentMatchers.any());
	}
}
