package org.example.bank.transactions;

import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.model.Account;

public class DepositTransaction implements Transaction {

    private final Account account;
    private final double amount;
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    public DepositTransaction(Account account, double amount) {
        this.account = account;
        this.amount = amount;
    }

    @Override
    public void execute() {

        try {
            account.getSemaphore().acquire();

            double newBalance = account.getBalance() + amount;

            accountDAO.updateBalance(account.getId(), newBalance);
            account.setBalance(newBalance);

            // ✅ LOG TRANSACTION
            transactionDAO.logDeposit(account.getAccountRef(), amount);

            System.out.println("Deposit successful on "
                    + account.getAccountRef()
                    + ". New balance: " + newBalance);

        } catch (InterruptedException e) {
            System.out.println("Deposit interrupted");
        } finally {
            account.getSemaphore().release();
        }
    }
}
