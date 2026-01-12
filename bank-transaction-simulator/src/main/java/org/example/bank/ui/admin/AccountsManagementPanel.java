package org.example.bank.ui.admin;

import org.example.bank.auth.User;
import org.example.bank.concurrency.BankEngine;
import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.model.Account;
import org.example.bank.transactions.DepositTransaction;
import org.example.bank.transactions.TransferTransaction;
import org.example.bank.transactions.WithdrawTransaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Optional;

public class AccountsManagementPanel extends JPanel {

    private final JFrame owner;
    private final User currentUser;
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final BankEngine engine;

    private JTable table;
    private DefaultTableModel model;

    public AccountsManagementPanel(JFrame owner, User currentUser, AccountDAO accountDAO,
                                   TransactionDAO transactionDAO, BankEngine engine) {
        this.owner = owner;
        this.currentUser = currentUser;
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.engine = engine;

        setLayout(new BorderLayout(12, 12));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        add(buildTop(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildBottomButtons(), BorderLayout.SOUTH);

        refresh();
    }

    private JComponent buildTop() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("💳 Accounts Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JButton refresh = buttonClientStyle("Refresh", "🔄");
        refresh.addActionListener(e -> refresh());

        top.add(title, BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);
        return top;
    }

    private JComponent buildTable() {
        model = new DefaultTableModel(new Object[]{"ID", "USER_ID", "ACCOUNT_REF", "BALANCE"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(26);
        return new JScrollPane(table);
    }

    private JComponent buildBottomButtons() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        bottom.setOpaque(false);

        JButton deposit  = buttonClientStyle("Deposit", "➕");
        JButton withdraw = buttonClientStyle("Withdraw", "➖");
        JButton transfer = buttonClientStyle("Transfer", "🔁");

        deposit.addActionListener(e -> doDeposit());
        withdraw.addActionListener(e -> doWithdraw());
        transfer.addActionListener(e -> doTransfer());

        bottom.add(deposit);
        bottom.add(withdraw);
        bottom.add(transfer);
        return bottom;
    }

    // ===================== ACTIONS =====================

    private void doDeposit() {
        Account acc = getSelectedAccountOrWarn();
        if (acc == null) return;

        Double amount = askAmount("Deposit - Amount");
        if (amount == null) return;

        // ✅ vraie transaction
        engine.submit(new DepositTransaction(acc, amount));
        JOptionPane.showMessageDialog(owner, "Deposit submitted ✅", "OK", JOptionPane.INFORMATION_MESSAGE);

        refreshLater();
    }

    private void doWithdraw() {
        Account acc = getSelectedAccountOrWarn();
        if (acc == null) return;

        Double amount = askAmount("Withdraw - Amount");
        if (amount == null) return;

        engine.submit(new WithdrawTransaction(acc, amount));
        JOptionPane.showMessageDialog(owner, "Withdraw submitted ✅", "OK", JOptionPane.INFORMATION_MESSAGE);

        refreshLater();
    }

    private void doTransfer() {
        Account from = getSelectedAccountOrWarn();
        if (from == null) return;

        // Choisir compte destination
        List<Account> all = accountDAO.findAll();
        Account to = chooseAccountDialog(all, from);
        if (to == null) return;

        Double amount = askAmount("Transfer - Amount");
        if (amount == null) return;

        engine.submit(new TransferTransaction(from, to, amount));
        JOptionPane.showMessageDialog(owner, "Transfer submitted ✅", "OK", JOptionPane.INFORMATION_MESSAGE);

        refreshLater();
    }

    // ===================== HELPERS =====================

    private void refreshLater() {
        // petite attente pour laisser le worker exécuter
        new javax.swing.Timer(400, e -> {
            refresh();
            ((javax.swing.Timer) e.getSource()).stop();
        }).start();
    }

    private Account getSelectedAccountOrWarn() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Select an account first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int accountId = Integer.parseInt(model.getValueAt(row, 0).toString());
        return findAccountById(accountId).orElse(null);
    }

    private Optional<Account> findAccountById(int id) {
        return accountDAO.findAll().stream().filter(a -> a.getId() == id).findFirst();
    }

    private Double askAmount(String title) {
        String s = JOptionPane.showInputDialog(owner, "Enter amount:", title, JOptionPane.PLAIN_MESSAGE);
        if (s == null) return null;
        s = s.trim().replace(",", ".");
        if (s.isEmpty()) return null;

        try {
            double v = Double.parseDouble(s);
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private Account chooseAccountDialog(List<Account> accounts, Account from) {
        DefaultComboBoxModel<Account> m = new DefaultComboBoxModel<>();
        for (Account a : accounts) {
            if (a.getId() != from.getId()) m.addElement(a);
        }

        JComboBox<Account> cb = new JComboBox<>(m);
        cb.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel();
            if (value != null) {
                lbl.setText(value.getAccountRef() + "  |  " + String.format("%.2f DH", value.getBalance()));
            }
            lbl.setOpaque(true);
            lbl.setBackground(isSelected ? new Color(245, 247, 250) : Color.WHITE);
            return lbl;
        });

        int res = JOptionPane.showConfirmDialog(owner, cb, "Select destination account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (res != JOptionPane.OK_OPTION) return null;
        return (Account) cb.getSelectedItem();
    }

    private void refresh() {
        model.setRowCount(0);
        List<Account> list = accountDAO.findAll();
        for (Account a : list) {
            // ✅ USER_ID doit venir de l'objet Account (pas a.getId())
            model.addRow(new Object[]{a.getId(), a.getId(), a.getAccountRef(), a.getBalance()});
        }
    }

    private JButton buttonClientStyle(String text, String icon) {
        JButton b = new JButton("  " + icon + "  " + text + "  ");
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222,226,230), 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(245, 247, 250));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(Color.WHITE);
            }
        });
        return b;
    }
}