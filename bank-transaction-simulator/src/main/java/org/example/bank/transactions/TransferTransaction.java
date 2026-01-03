package org.example.bank.transactions;

import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.model.Account;

public class TransferTransaction implements Transaction {

    private final Account from;
    private final Account to;
    private final double amount;
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    public TransferTransaction(Account from, Account to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public void execute() {

        Account first = from.getId() < to.getId() ? from : to;
        Account second = from.getId() < to.getId() ? to : from;

        try {
            first.getSemaphore().acquire();
            second.getSemaphore().acquire();

            if (from.getBalance() < amount) {
                System.out.println("Transfer failed: insufficient balance");
                return;
            }

            double newFromBalance = from.getBalance() - amount;
            double newToBalance = to.getBalance() + amount;

            accountDAO.updateBalance(from.getId(), newFromBalance);
            accountDAO.updateBalance(to.getId(), newToBalance);

            from.setBalance(newFromBalance);
            to.setBalance(newToBalance);

            // ✅ LOG TRANSACTION
            transactionDAO.logTransfer(
                    from.getAccountRef(),
                    to.getAccountRef(),
                    amount
            );

            System.out.println("Transfer successful: "
                    + amount + " from "
                    + from.getAccountRef()
                    + " to "
                    + to.getAccountRef());

        } catch (InterruptedException e) {
            System.out.println("❌ Transfer interrupted");
        } finally {
            second.getSemaphore().release();
            first.getSemaphore().release();
        }
    }
}
