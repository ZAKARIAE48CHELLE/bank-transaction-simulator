package org.example.bank.test;

import org.example.bank.concurrency.BankEngine;
import org.example.bank.transactions.Transaction;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ParallelAccessDemo {

    private static final ConcurrentHashMap<String, Integer> balances = new ConcurrentHashMap<>();

    // ✅ MUST be public to avoid reflection access issues
    public static class DemoTransaction implements Transaction {
        private final String name;
        private final List<String> keysToLock;
        private final String accountToUpdate;
        private final int delta;
        private final int workMs;
        private final CountDownLatch startGate;
        private final CountDownLatch doneGate;

        public DemoTransaction(String name,
                               List<String> keysToLock,
                               String accountToUpdate,
                               int delta,
                               int workMs,
                               CountDownLatch startGate,
                               CountDownLatch doneGate) {
            this.name = name;
            this.keysToLock = keysToLock;
            this.accountToUpdate = accountToUpdate;
            this.delta = delta;
            this.workMs = workMs;
            this.startGate = startGate;
            this.doneGate = doneGate;
        }

        // ✅ BankEngine will call this via reflection
        public List<String> lockKeys() {
            return keysToLock;
        }

        @Override
        public void execute() {
            try {
                startGate.await();

                long start = System.nanoTime();
                System.out.println("START  " + name + "   thread=" + Thread.currentThread().getName());

                Thread.sleep(workMs);

                Integer old = balances.get(accountToUpdate);
                balances.put(accountToUpdate, old + delta);

                long end = System.nanoTime();
                System.out.println("END    " + name
                        + "   took=" + ((end - start) / 1_000_000) + " ms"
                        + "   newBalance(" + accountToUpdate + ")=" + balances.get(accountToUpdate));

            } catch (Exception e) {
                System.err.println("ERROR " + name + " : " + e.getMessage());
            } finally {
                doneGate.countDown();
            }
        }
    }

    private static void runScenario(String title,
                                    BankEngine engine,
                                    DemoTransaction t1,
                                    DemoTransaction t2,
                                    CountDownLatch startGate,
                                    CountDownLatch doneGate) throws Exception {

        System.out.println("\n==============================================");
        System.out.println("SCENARIO: " + title);
        System.out.println("==============================================");

        long t0 = System.nanoTime();

        engine.submit(t1);
        engine.submit(t2);

        // release both at the same time
        startGate.countDown();

        // wait finish
        doneGate.await();

        long t1End = System.nanoTime();
        System.out.println("TOTAL time = " + ((t1End - t0) / 1_000_000) + " ms");
    }

    public static void main(String[] args) throws Exception {

        balances.put("ACC-A", 0);
        balances.put("ACC-B", 0);

        // 2 threads to make the effect obvious
        BankEngine engine = new BankEngine(2);

        // 1) Parallel (different accounts)
        CountDownLatch startGate1 = new CountDownLatch(1);
        CountDownLatch doneGate1 = new CountDownLatch(2);

        DemoTransaction p1 = new DemoTransaction(
                "Tx-1 (locks ACC-A, updates ACC-A)",
                List.of("ACC-A"),
                "ACC-A",
                +10,
                800,
                startGate1,
                doneGate1
        );

        DemoTransaction p2 = new DemoTransaction(
                "Tx-2 (locks ACC-B, updates ACC-B)",
                List.of("ACC-B"),
                "ACC-B",
                +20,
                800,
                startGate1,
                doneGate1
        );

        runScenario("PARALLEL (different accounts) => expected ~800ms total",
                engine, p1, p2, startGate1, doneGate1);

        // 2) Serialized (same account)
        CountDownLatch startGate2 = new CountDownLatch(1);
        CountDownLatch doneGate2 = new CountDownLatch(2);

        DemoTransaction s1 = new DemoTransaction(
                "Tx-3 (locks ACC-A, updates ACC-A)",
                List.of("ACC-A"),
                "ACC-A",
                +1,
                800,
                startGate2,
                doneGate2
        );

        DemoTransaction s2 = new DemoTransaction(
                "Tx-4 (locks ACC-A, updates ACC-A)",
                List.of("ACC-A"),
                "ACC-A",
                +1,
                800,
                startGate2,
                doneGate2
        );

        runScenario("SERIALIZED (same account) => expected ~1600ms total",
                engine, s1, s2, startGate2, doneGate2);

        System.out.println("\nFinal balances:");
        System.out.println("ACC-A = " + balances.get("ACC-A"));
        System.out.println("ACC-B = " + balances.get("ACC-B"));

        engine.shutdown();
    }
}
