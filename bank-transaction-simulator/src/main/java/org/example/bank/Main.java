package org.example.bank;

import org.example.bank.auth.LoginService;
import org.example.bank.auth.User;
import org.example.bank.concurrency.BankEngine;
import org.example.bank.concurrency.Worker;
import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.model.Account;
import org.example.bank.transactions.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Main extends JFrame {

    private User user;
    private final LoginService loginService = new LoginService();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    private final BankEngine engine = new BankEngine();
    private Worker[] workers;

    private JComboBox<Account> accountBox;
    private JTextArea outputArea;

    // ================== CONSTRUCTOR ==================
    public Main() {

        setTitle("Bank Transaction Simulator");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        startWorkers();
        loginDialog();     // blocks until login success
        buildUI();

        setVisible(true); // 🔴 VERY IMPORTANT
    }

    // ================== LOGIN ==================
    private void loginDialog() {

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();

        Object[] fields = {
                "Username:", userField,
                "Password:", passField
        };

        int option = JOptionPane.showConfirmDialog(
                this, fields, "Login",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (option != JOptionPane.OK_OPTION) {
            System.exit(0);
        }

        user = loginService.login(
                userField.getText(),
                new String(passField.getPassword())
        );

        if (user == null) {
            JOptionPane.showMessageDialog(
                    this, "Login failed",
                    "Error", JOptionPane.ERROR_MESSAGE
            );
            System.exit(0);
        }
    }

    // ================== UI ==================
    private void buildUI() {

        setLayout(new BorderLayout());

        // ---- TOP PANEL ----
        JPanel top = new JPanel(new FlowLayout());
        top.add(new JLabel("Welcome " + user.getUsername()
                + " (" + user.getRole() + ")"));
        add(top, BorderLayout.NORTH);

        // ---- CENTER ----
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // ---- BOTTOM ----
        JPanel bottom = new JPanel(new GridLayout(2, 1));

        accountBox = new JComboBox<>(loadAccounts());
        bottom.add(accountBox);

        JPanel buttons = new JPanel();

        JButton depositBtn = new JButton("Deposit");
        JButton withdrawBtn = new JButton("Withdraw");
        JButton transferBtn = new JButton("Transfer");
        JButton historyBtn = new JButton("History");
        JButton exitBtn = new JButton("Exit");

        buttons.add(depositBtn);
        buttons.add(withdrawBtn);
        buttons.add(transferBtn);
        buttons.add(historyBtn);
        buttons.add(exitBtn);

        bottom.add(buttons);
        add(bottom, BorderLayout.SOUTH);

        // ---- ACTIONS ----
        depositBtn.addActionListener(e -> deposit());
        withdrawBtn.addActionListener(e -> withdraw());
        transferBtn.addActionListener(e -> transfer());
        historyBtn.addActionListener(e -> history());
        exitBtn.addActionListener(e -> exit());
    }

    // ================== ACTIONS ==================
    private void deposit() {
        Account acc = (Account) accountBox.getSelectedItem();
        double amount = askAmount();
        if (amount <= 0) return;

        engine.submit(new DepositTransaction(acc, amount));
        log("Deposit submitted");
    }

    private void withdraw() {
        Account acc = (Account) accountBox.getSelectedItem();
        double amount = askAmount();
        if (amount <= 0) return;

        engine.submit(new WithdrawTransaction(acc, amount));
        log("Withdraw submitted");
    }

    private void transfer() {
        Account from = (Account) accountBox.getSelectedItem();
        String toRef = JOptionPane.showInputDialog(this,
                "Target account reference:");

        if (toRef == null) return;

        Account to = accountDAO.findByAccountRef(toRef);
        if (to == null) {
            JOptionPane.showMessageDialog(this, "Account not found");
            return;
        }

        double amount = askAmount();
        if (amount <= 0) return;

        engine.submit(new TransferTransaction(from, to, amount));
        log("Transfer submitted");
    }

    private void history() {
        Account acc = (Account) accountBox.getSelectedItem();
        outputArea.setText("");

        transactionDAO.findHistoryByAccountRef(acc.getAccountRef())
                .forEach(this::log);
    }

    private void exit() {
        shutdownWorkers();
        System.exit(0);
    }

    // ================== HELPERS ==================
    private Account[] loadAccounts() {
        List<Account> list = accountDAO.findByUserId(user.getId());
        return list.toArray(new Account[0]);
    }

    private double askAmount() {
        try {
            return Double.parseDouble(
                    JOptionPane.showInputDialog(this, "Amount:")
            );
        } catch (Exception e) {
            return -1;
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() ->
                outputArea.append(msg + "\n")
        );
    }

    // ================== WORKERS ==================
    private void startWorkers() {
        workers = new Worker[3];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(engine, i + 1);
            workers[i].start();
        }
    }

    private void shutdownWorkers() {
        for (Worker w : workers) w.shutdown();
        for (Worker w : workers) {
            try { w.join(); } catch (InterruptedException ignored) {}
        }
    }

    // ================== MAIN ==================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
