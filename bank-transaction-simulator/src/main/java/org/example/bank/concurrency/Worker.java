package org.example.bank.concurrency;

import org.example.bank.transactions.Transaction;

public class Worker extends Thread {

    private final BankEngine engine;
    private volatile boolean running = true;

    public Worker(BankEngine engine, int id) {
        super("Worker-" + id);
        this.engine = engine;
    }

    public void shutdown() {
        running = false;
        interrupt(); // unblock queue.take()
    }

    @Override
    public void run() {

        while (running) {
            try {
                Transaction transaction = engine.take();
                System.out.println("[" + getName() + "] processing transaction");
                transaction.execute();
            } catch (InterruptedException e) {
                // Thread interrupted for shutdown
                break;
            }
        }

        System.out.println(getName() + " stopped.");
    }
}
