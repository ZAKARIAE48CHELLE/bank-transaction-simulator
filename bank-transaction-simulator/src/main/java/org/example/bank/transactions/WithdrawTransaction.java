package org.example.bank.transactions;

import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.model.Account;

public class WithdrawTransaction implements Transaction {

    private final Account account;
    private final double amount;
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    public WithdrawTransaction(Account account, double amount) {
        this.account = account;
        this.amount = amount;
    }

    @Override
    public void execute() {

        try {
            account.getSemaphore().acquire();

            if (account.getBalance() < amount) {
                System.out.println("❌ Insufficient balance");
                return;
            }

            double newBalance = account.getBalance() - amount;

            accountDAO.updateBalance(account.getId(), newBalance);
            account.setBalance(newBalance);

            // ✅ LOG TRANSACTION
            transactionDAO.logWithdraw(account.getAccountRef(), amount);

            System.out.println("Withdrawal successful from "
                    + account.getAccountRef()
                    + ". New balance: " + newBalance);

        } catch (InterruptedException e) {
            System.out.println("Withdrawal interrupted");
        } finally {
            account.getSemaphore().release();
        }
    }
}
