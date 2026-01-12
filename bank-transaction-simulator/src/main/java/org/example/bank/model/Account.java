package org.example.bank.model;

import java.util.concurrent.Semaphore;

public class Account {

    private int id;
    private int userId;
    private String accountRef;
    private double balance;

    // 🔐 Semaphore pour la concurrence (transactions parallèles)
    private final Semaphore semaphore = new Semaphore(1, true);

    public Account(int id, int userId, String accountRef, double balance) {
        this.id = id;
        this.userId = userId;
        this.accountRef = accountRef;
        this.balance = balance;
    }

    // ---------- GETTERS ----------
    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getAccountRef() {
        return accountRef;
    }

    public double getBalance() {
        return balance;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    // ---------- SETTERS ----------
    public void setBalance(double balance) {
        this.balance = balance;
    }

    // ✅ utilisé par Deposit / Withdraw
    public void updateBalance(double amount) {
        this.balance += amount;
    }

    // ✅ compatibilité avec anciens appels (TransferTransaction)
    public void updateBalance(int accountId, double newBalance) {
        if (this.id == accountId) {
            this.balance = newBalance;
        }
    }
}
