package org.example.bank.transactions;

import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.model.Account;

import java.util.List;

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

        // Log transaction as PENDING
        int txId = transactionDAO.log(
                "TRANSFER",
                from.getAccountRef(),
                to.getAccountRef(),
                amount
        );

        Account first = from.getId() < to.getId() ? from : to;
        Account second = from.getId() < to.getId() ? to : from;

        try {
            first.getSemaphore().acquire();
            second.getSemaphore().acquire();

            if (from.getBalance() < amount) {
                System.out.println("Transfer failed: insufficient balance");
                transactionDAO.markFailed(txId);
                return;
            }

            double newFromBalance = from.getBalance() - amount;
            double newToBalance = to.getBalance() + amount;

            accountDAO.updateBalance(from.getId(), newFromBalance);
            accountDAO.updateBalance(to.getId(), newToBalance);

            from.setBalance(newFromBalance);
            to.setBalance(newToBalance);

            //  Mark transaction as DONE
            transactionDAO.markDone(txId);

            System.out.println("Transfer successful: "
                    + amount + " from "
                    + from.getAccountRef()
                    + " to "
                    + to.getAccountRef());

        } catch (InterruptedException e) {
            System.out.println("Transfer interrupted");
            transactionDAO.markFailed(txId);
        } finally {
            second.getSemaphore().release();
            first.getSemaphore().release();
        }
    }

    @Override
    public List<String> lockKeys() {
        return List.of(from.getAccountRef(), to.getAccountRef());
    }
}
