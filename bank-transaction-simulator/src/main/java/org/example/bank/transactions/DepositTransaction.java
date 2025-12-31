package org.example.bank.transactions;

import org.example.bank.dao.AccountDAO;
import org.example.bank.model.Account;

public class DepositTransaction implements Transaction {

    private final Account account;
    private final double amount;
    private final AccountDAO accountDAO = new AccountDAO();

    public DepositTransaction(Account account, double amount) {
        this.account = account;
        this.amount = amount;
    }

    @Override
    public void execute() {

        try {
            // LOCK account
            account.getSemaphore().acquire();

            double newBalance = account.getBalance() + amount;

            // update DB
            accountDAO.updateBalance(account.getId(), newBalance);

            // update in-memory object
            account.setBalance(newBalance);

            System.out.println("✅ Deposit successful. New balance: " + newBalance);

        } catch (InterruptedException e) {
            System.out.println("❌ Deposit interrupted");
        } finally {
            account.getSemaphore().release();
        }
    }
}
