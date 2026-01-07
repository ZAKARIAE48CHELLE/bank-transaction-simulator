package org.example.bank.transactions;

import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.model.Account;

import java.util.List;

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

        // Log as PENDING
        int txId = transactionDAO.log(
                "WITHDRAW",
                account.getAccountRef(),
                null,
                amount
        );

        try {
            account.getSemaphore().acquire();

            if (account.getBalance() < amount) {
                System.out.println("Insufficient balance");
                return;
            }

            double newBalance = account.getBalance() - amount;

            accountDAO.updateBalance(account.getId(), newBalance);
            account.setBalance(newBalance);

            // Mark DONE
            transactionDAO.markDone(txId);

            System.out.println("Withdrawal successful from "
                    + account.getAccountRef()
                    + ". New balance: " + newBalance);

        } catch (InterruptedException e) {
            System.out.println("Withdrawal interrupted");
        } finally {
            account.getSemaphore().release();
        }
    }

    @Override
    public List<String> lockKeys() {
        return List.of(account.getAccountRef());
    }
}
