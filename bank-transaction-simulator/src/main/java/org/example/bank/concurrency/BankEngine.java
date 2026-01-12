package org.example.bank.concurrency;

import org.example.bank.transactions.Transaction;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class BankEngine {

    private final ExecutorService pool;
    private final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();

    public BankEngine() {
        this(3);
    }

    public BankEngine(int threads) {
        this.pool = Executors.newFixedThreadPool(Math.max(1, threads));
    }

    public void submit(Transaction tx) {
        if (tx == null) return;

        pool.submit(() -> {
            try {
                executeSafely(tx);
            } catch (Exception e) {
                System.err.println("[BankEngine] Transaction failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void executeSafely(Transaction tx) {
        List<String> keys = getLockKeysIfExists(tx);

        // clean + unique
        Set<String> s = new HashSet<>();
        if (keys != null) {
            for (String k : keys) {
                if (k == null) continue;
                String t = k.trim();
                if (!t.isEmpty()) s.add(t);
            }
        }

        List<String> clean = new ArrayList<>(s);
        clean.sort(String::compareTo); // avoid deadlock

        List<ReentrantLock> locks = new ArrayList<>();
        for (String k : clean) {
            locks.add(accountLocks.computeIfAbsent(k, x -> new ReentrantLock(true)));
        }

        for (ReentrantLock L : locks) L.lock();
        try {
            tx.execute();
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) locks.get(i).unlock();
        }
    }

    // Robust reflection (works even for non-public inner classes)
    private List<String> getLockKeysIfExists(Transaction tx) {
        try {
            Method m;
            try {
                // most cases
                m = tx.getClass().getMethod("lockKeys");
            } catch (NoSuchMethodException ex) {
                // inner/non-public cases
                m = tx.getClass().getDeclaredMethod("lockKeys");
                m.setAccessible(true);
            }

            Object res = m.invoke(tx);
            if (res instanceof List<?>) {
                List<?> list = (List<?>) res;
                List<String> out = new ArrayList<>();
                for (Object o : list) {
                    if (o != null) out.add(o.toString());
                }
                return out;
            }
        } catch (NoSuchMethodException ignored) {
            // no lockKeys() => no locks
        } catch (Exception e) {
            System.err.println("[BankEngine] lockKeys() error: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
