package org.example.bank.test;

import org.example.bank.concurrency.BankEngine;
import org.example.bank.concurrency.Worker;
import org.example.bank.dao.AccountDAO;
import org.example.bank.model.Account;
import org.example.bank.transactions.TransferTransaction;

import java.util.List;
import java.util.Random;

public class StressTest {

    public static void main(String[] args) {

        System.out.println("=== STRESS TEST: 50 Concurrent Transfers ===");

        AccountDAO accountDAO = new AccountDAO();

        // Load ALL accounts (admin-level)
        List<Account> accounts = accountDAO.findAll();

        if (accounts.size() < 2) {
            System.out.println("Not enough accounts for stress test.");
            return;
        }

        // Start engine and workers
        BankEngine engine = new BankEngine();

        int workerCount = 5;
        Worker[] workers = new Worker[workerCount];

        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Worker(engine, i + 1);
            workers[i].start();
        }

        Random random = new Random();

        // Submit 50 concurrent transfers
        for (int i = 1; i <= 50; i++) {

            Account from;
            Account to;

            do {
                from = accounts.get(random.nextInt(accounts.size()));
                to = accounts.get(random.nextInt(accounts.size()));
            } while (from.getId() == to.getId());

            double amount = 10 + random.nextInt(200);

            TransferTransaction tx =
                    new TransferTransaction(from, to, amount);

            engine.submit(tx);
            System.out.println("Submitted transfer #" + i
                    + " | " + from.getAccountRef()
                    + " -> " + to.getAccountRef()
                    + " | " + amount);
        }

        // Give workers time to finish
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {}

        // Shutdown workers
        System.out.println("\nShutting down workers...");
        for (Worker w : workers) {
            w.shutdown();
        }

        for (Worker w : workers) {
            try {
                w.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("=== STRESS TEST COMPLETED ===");
    }
}
