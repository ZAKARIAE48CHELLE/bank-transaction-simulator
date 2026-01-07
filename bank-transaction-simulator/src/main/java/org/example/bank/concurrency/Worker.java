package org.example.bank.concurrency;

/**
 * Legacy class (not used anymore).
 * Your project now uses BankEngine ThreadPool instead of queue.take().
 */
@Deprecated
public class Worker extends Thread {

    public Worker(BankEngine engine, int id) {
        super("Worker-" + id);
        // no-op (kept only to avoid breaking old references)
    }

    public void shutdown() {
        interrupt();
    }

    @Override
    public void run() {
        // no-op
    }
}
