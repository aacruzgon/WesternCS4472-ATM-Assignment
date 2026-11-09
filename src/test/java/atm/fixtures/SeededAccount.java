package atm.fixtures;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import atm.ATM;
import atm.dispatcher.MessageDispatcher;
import atm.exceptions.InvalidCardNumberException;
import atm.session.Session;
import atm.session.transactions.ATMTransaction;
import atm.session.transactions.ATMTransactionFactory;
import atm.ui.panels.MainPanel;
import atm.utils.CredentialsCheck;
import atm.utils.FormatChecker;
import bank.transactions.utils.AccountType;
import bank.transactions.utils.TransactionResult;
import bank.transactions.utils.TransactionType;

/**
 * Shared test setup: the account the assignment asks for ($1,000 daily limit,
 * $5,000 balance) and the fake objects needed to run an ATM session.
 *
 * The card number is the same as in bank-seed.sql. The PIN is different,
 * because the PIN in that file has 4 digits and the assignment needs 5. The
 * database has nowhere to store a daily limit, so it is only a value here.
 *
 * The client is not a student, so with a $5,000 balance the withdrawal fee is $0.
 *
 * MIN_AMOUNT and MAX_BALANCE are team assumptions, not from the assignment.
 * Amounts go from MIN_AMOUNT to DAILY_LIMIT. MAX_BALANCE is the highest balance
 * to use in tests that change the balance.
 *
 * We do not use MockitoExtension here. It reports an error when a fake answer
 * is set up but never used, which happens in tests where the input is rejected.
 *
 * How to use it:
 * <pre>
 * MainPanel mainPanel = SeededAccount.mainPanel();
 * MessageDispatcher dispatcher = SeededAccount.dispatcher();
 * Session session = SeededAccount.sessionAtAmountPrompt(mainPanel, dispatcher, SeededAccount.PIN);
 * session.setAmount(100);
 * </pre>
 */
public final class SeededAccount {
	public static final String CARD = "4000000000000000";
	public static final String PIN = "55555";
	public static final int MIN_AMOUNT = 20;
	public static final int DAILY_LIMIT = 1000;
	public static final double BALANCE = 5000.0;
	public static final double MAX_BALANCE = 100000.0;
	public static final boolean STUDENT = false;
	public static final double WITHDRAWAL_FEE = 0.0;

	private SeededAccount() {
	}

	public static MainPanel mainPanel() {
		return Mockito.mock(MainPanel.class);
	}

	/**
	 * A fake bank. It accepts any card and PIN, because the PIN format is checked
	 * before the bank is contacted. It also approves every transaction. It does
	 * not check the daily limit, so the ATM has to reject those amounts itself.
	 */
	public static MessageDispatcher dispatcher() {
		MessageDispatcher dispatcher = Mockito.mock(MessageDispatcher.class);
		Mockito.doReturn(true).when(dispatcher).checkCredentials(ArgumentMatchers.any(), ArgumentMatchers.any());
		Mockito.doAnswer(invocation -> {
			ATMTransaction transaction = invocation.getArgument(2);
			double[] balances = {BALANCE - transaction.getAmount() - WITHDRAWAL_FEE};
			return new TransactionResult(true, "", WITHDRAWAL_FEE, balances);
		}).when(dispatcher).performTransaction(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
		return dispatcher;
	}

	/** An ATM that has accepted the card and is waiting for a PIN. It uses the real FormatChecker. */
	public static ATM atmAtPinPrompt(MainPanel mainPanel, MessageDispatcher dispatcher)
			throws InvalidCardNumberException {
		ATM atm = new ATM(new FormatChecker(), new CredentialsCheck(dispatcher), dispatcher);
		atm.setMainPanel(mainPanel);
		atm.createSession();
		atm.checkCardNumber(CARD);
		return atm;
	}

	/**
	 * A session that has gone through the card, PIN, withdrawal and account steps
	 * and is waiting for an amount. Session does not check the PIN, so a bug in
	 * the PIN check cannot affect the amount tests.
	 */
	public static Session sessionAtAmountPrompt(MainPanel mainPanel, MessageDispatcher dispatcher, String pin) {
		Session session = new Session(mainPanel, dispatcher);
		session.insertCard();
		session.addCard(CARD);
		session.addPin(pin.toCharArray());
		session.setTransaction(ATMTransactionFactory.createTransaction(TransactionType.Withdrawal));
		session.setFromAccount(AccountType.Chequing);
		return session;
	}
}
