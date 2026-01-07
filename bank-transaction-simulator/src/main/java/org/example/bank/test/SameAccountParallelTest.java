package org.example.bank.test;

import org.example.bank.concurrency.BankEngine;
import org.example.bank.transactions.Transaction;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class SameAccountParallelTest {

    private static final ConcurrentHashMap<String, Integer> balances = new ConcurrentHashMap<>();
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static String now() {
        return LocalTime.now().format(F);
    }

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

        public List<String> lockKeys() {
            return List.of(acc); // lock on same account => serialized
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

        String ACC = "ACC-1";
        balances.put(ACC, 100);

        BankEngine engine = new BankEngine(2);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(2);

        DemoTx tx1 = new DemoTx("Tx-A (+50)", ACC, +50, 1000, startGate, doneGate);
        DemoTx tx2 = new DemoTx("Tx-B (-30)", ACC, -30, 1000, startGate, doneGate);

        System.out.println("Initial balance(" + ACC + ") = " + balances.get(ACC));

        long t0 = System.nanoTime();

        engine.submit(tx1);
        engine.submit(tx2);

        // release both tasks "at the same time"
        startGate.countDown();

        doneGate.await();

        long t1 = System.nanoTime();
        System.out.println("\nTOTAL time = " + ((t1 - t0) / 1_000_000) + " ms");
        System.out.println("Final balance(" + ACC + ") = " + balances.get(ACC));

        engine.shutdown();
    }
}
