package org.example.bank.model;
import java.util.concurrent.Semaphore;

public class Account {


    private int id;
    private int userId;
    private String accountRef;
    private double balance;

    // semaphore unchanged
    private final Semaphore semaphore = new Semaphore(1);

    public Account(int id, int userId, String accountRef, double balance) {
        this.id = id;
        this.userId = userId;
        this.accountRef = accountRef;
        this.balance = balance;
    }

    public int getId() {
        return id;
    }

    public String getAccountRef() {
        return accountRef;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }
}
