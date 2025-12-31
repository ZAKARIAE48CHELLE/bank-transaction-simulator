package org.example.bank;

import org.example.bank.auth.LoginService;
import org.example.bank.auth.User;
import org.example.bank.dao.AccountDAO;
import org.example.bank.model.Account;
import org.example.bank.transactions.DepositTransaction;
import org.example.bank.transactions.WithdrawTransaction;
import org.example.bank.transactions.TransferTransaction;
import org.example.bank.transactions.Transaction;
import org.example.bank.concurrency.BankEngine;
import org.example.bank.concurrency.Worker;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        System.out.println("=== Bank Transaction Simulator ===");

        Scanner scanner = new Scanner(System.in);
        LoginService loginService = new LoginService();

        // ================= LOGIN =================
        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        User user = loginService.login(username, password);

        if (user == null) {
            System.out.println("❌ Login failed!");
            return;
        }

        System.out.println("✅ Login successful!");
        System.out.println("Welcome " + user.getUsername()
                + " (" + user.getRole() + ")");

        // ================= START ENGINE & WORKERS =================
        BankEngine engine = new BankEngine();

        Worker[] workers = new Worker[3];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(engine, i + 1);
            workers[i].start();
        }

        AccountDAO accountDAO = new AccountDAO();
        boolean running = true;

        // ================= MENU LOOP =================
        while (running) {

            List<Account> accounts = accountDAO.findByUserId(user.getId());

            if (accounts.isEmpty()) {
                System.out.println("❌ You have no accounts.");
                break;
            }

            System.out.println("\nYour accounts:");
            for (Account acc : accounts) {
                System.out.println("Account " + acc.getAccountRef()
                        + " | Balance = " + acc.getBalance());
            }

            System.out.println("\nChoose an option:");
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.println("3. Transfer");
            System.out.println("4. Exit");
            System.out.print("Your choice: ");

            int choice = Integer.parseInt(scanner.nextLine());

            if (choice == 4) {
                running = false;
                break;
            }

            // ---------- SOURCE ACCOUNT ----------
            System.out.print("Choose source account reference: ");
            String fromRef = scanner.nextLine();

            Account fromAccount = null;
            for (Account acc : accounts) {
                if (acc.getAccountRef().equals(fromRef)) {
                    fromAccount = acc;
                    break;
                }
            }

            if (fromAccount == null) {
                System.out.println("❌ Invalid account reference.");
                continue;
            }

            Account toAccount = null;

            // ---------- TARGET ACCOUNT (TRANSFER ONLY) ----------
            if (choice == 3) {
                System.out.print("Choose target account reference: ");
                String toRef = scanner.nextLine();

                for (Account acc : accounts) {
                    if (acc.getAccountRef().equals(toRef)) {
                        toAccount = acc;
                        break;
                    }
                }

                if (toAccount == null || toAccount == fromAccount) {
                    System.out.println("❌ Invalid target account reference.");
                    continue;
                }
            }

            // ---------- AMOUNT ----------
            System.out.print("Amount: ");
            double amount = Double.parseDouble(scanner.nextLine());

            Transaction transaction;

            if (choice == 1) {
                transaction = new DepositTransaction(fromAccount, amount);
            } else if (choice == 2) {
                transaction = new WithdrawTransaction(fromAccount, amount);
            } else if (choice == 3) {
                transaction = new TransferTransaction(fromAccount, toAccount, amount);
            } else {
                System.out.println("❌ Invalid choice.");
                continue;
            }

            // ---------- SUBMIT TRANSACTION ----------
            engine.submit(transaction);
            System.out.println("🕒 Transaction submitted...");
        }

        // ================= SHUTDOWN =================
        shutdownWorkers(workers);
        System.out.println("Application finished.");
    }

    // ================= CLEAN SHUTDOWN =================
    private static void shutdownWorkers(Worker[] workers) {
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
    }
}
