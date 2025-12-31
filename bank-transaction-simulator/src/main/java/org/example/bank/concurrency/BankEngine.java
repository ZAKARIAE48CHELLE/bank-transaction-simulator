package org.example.bank.concurrency;

import org.example.bank.transactions.Transaction;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
public class BankEngine {

    private final BlockingQueue<Transaction> transactionQueue =
            new LinkedBlockingQueue<>();

    public void submit(Transaction transaction) {
        transactionQueue.add(transaction);
    }

    public Transaction take() throws InterruptedException {
        return transactionQueue.take();
    }
}
