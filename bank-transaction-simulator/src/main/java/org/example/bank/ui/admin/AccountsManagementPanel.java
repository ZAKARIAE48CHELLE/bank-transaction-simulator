package org.example.bank.ui.admin;

import org.example.bank.auth.User;
import org.example.bank.concurrency.BankEngine;
import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.model.Account;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AccountsManagementPanel extends JPanel {

    private final JFrame owner;
    private final User currentUser;
    private final AccountDAO accountDAO;

    private JTable table;
    private DefaultTableModel model;

    public AccountsManagementPanel(JFrame owner, User currentUser, AccountDAO accountDAO,
                                   TransactionDAO transactionDAO, BankEngine engine) {
        this.owner = owner;
        this.currentUser = currentUser;
        this.accountDAO = accountDAO;

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

        // placeholders (tu peux brancher vers tes vraies transactions ensuite)
        deposit.addActionListener(e -> JOptionPane.showMessageDialog(owner, "Deposit action (admin)"));
        withdraw.addActionListener(e -> JOptionPane.showMessageDialog(owner, "Withdraw action (admin)"));
        transfer.addActionListener(e -> JOptionPane.showMessageDialog(owner, "Transfer action (admin)"));

        bottom.add(deposit);
        bottom.add(withdraw);
        bottom.add(transfer);
        return bottom;
    }

    private void refresh() {
        model.setRowCount(0);

        List<Account> list = accountDAO.findAll();
        for (Account a : list) {
            model.addRow(new Object[]{
                    a.getId(),
                    a.getUserId(),        // ✅ CORRECTION ICI
                    a.getAccountRef(),
                    a.getBalance()
            });
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
