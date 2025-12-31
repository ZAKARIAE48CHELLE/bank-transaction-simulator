package org.example.bank.transactions;

import org.example.bank.dao.AccountDAO;
import org.example.bank.model.Account;

public class TransferTransaction implements Transaction {

    private final Account from;
    private final Account to;
    private final double amount;
    private final AccountDAO accountDAO = new AccountDAO();

    public TransferTransaction(Account from, Account to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public void execute() {

        // Deadlock-safe ordering
        Account first = from.getId() < to.getId() ? from : to;
        Account second = from.getId() < to.getId() ? to : from;

        try {
            // Lock both accounts in order
            first.getSemaphore().acquire();
            second.getSemaphore().acquire();

            if (from.getBalance() < amount) {
                System.out.println("❌ Transfer failed: insufficient balance");
                return;
            }

            double newFromBalance = from.getBalance() - amount;
            double newToBalance = to.getBalance() + amount;

            accountDAO.updateBalance(from.getId(), newFromBalance);
            accountDAO.updateBalance(to.getId(), newToBalance);

            from.setBalance(newFromBalance);
            to.setBalance(newToBalance);

            System.out.println("✅ Transfer successful: "
                    + amount + " from Account #" + from.getId()
                    + " to Account #" + to.getId());

        } catch (InterruptedException e) {
            System.out.println("❌ Transfer interrupted");
        } finally {
            // Always release in reverse order
            second.getSemaphore().release();
            first.getSemaphore().release();
        }
    }
}
