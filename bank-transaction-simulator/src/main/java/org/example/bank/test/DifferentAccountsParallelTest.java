package org.example.bank.test;

import org.example.bank.concurrency.BankEngine;
import org.example.bank.transactions.Transaction;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class DifferentAccountsParallelTest {

    private static final ConcurrentHashMap<String, Integer> balances = new ConcurrentHashMap<>();
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static String now() {
        return LocalTime.now().format(F);
    }

    // Transaction that locks ONE account
    public static class DemoTx implements Transaction {
        private final String name;
        private final String acc;
        private final int delta;
        private final int workMs;
        private final CountDownLatch startGate;
        private final CountDownLatch doneGate;

        public DemoTx(String name, String acc, int delta, int workMs,
                      CountDownLatch startGate, CountDownLatch doneGate) {
            this.name = name;
            this.acc = acc;
            this.delta = delta;
            this.workMs = workMs;
            this.startGate = startGate;
            this.doneGate = doneGate;
        }

        // ✅ different accounts => different locks => real parallel
        public List<String> lockKeys() {
            return List.of(acc);
        }

        @Override
        public void execute() {
            try {
                startGate.await();

                long start = System.nanoTime();
                System.out.println(now() + "  START  " + name + "   thread=" + Thread.currentThread().getName());

                Thread.sleep(workMs);

                int old = balances.get(acc);
                balances.put(acc, old + delta);

                long end = System.nanoTime();
                System.out.println(now() + "  END    " + name
                        + "   took=" + ((end - start) / 1_000_000) + " ms"
                        + "   balance(" + acc + ")=" + balances.get(acc));

            } catch (Exception e) {
                System.err.println(now() + "  ERROR " + name + " : " + e.getMessage());
            } finally {
                doneGate.countDown();
            }
        }
    }

    public static void main(String[] args) throws Exception {

        String A = "ACC-A";
        String B = "ACC-B";
        balances.put(A, 100);
        balances.put(B, 200);

        // 2 threads => can execute truly in parallel
        BankEngine engine = new BankEngine(2);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(2);

        // ✅ Different accounts
        DemoTx tx1 = new DemoTx("Tx-1 (+50 on ACC-A)", A, +50, 1000, startGate, doneGate);
        DemoTx tx2 = new DemoTx("Tx-2 (-30 on ACC-B)", B, -30, 1000, startGate, doneGate);

        System.out.println("Initial balances: " + A + "=" + balances.get(A) + " | " + B + "=" + balances.get(B));

        long t0 = System.nanoTime();

        engine.submit(tx1);
        engine.submit(tx2);

        // release both at the same time
        startGate.countDown();

        doneGate.await();

        long t1 = System.nanoTime();
        System.out.println("\nTOTAL time = " + ((t1 - t0) / 1_000_000) + " ms");
        System.out.println("Final balances: " + A + "=" + balances.get(A) + " | " + B + "=" + balances.get(B));

        engine.shutdown();
    }
}
