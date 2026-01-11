package org.example.bank.ui.admin;

import org.example.bank.auth.User;
import org.example.bank.concurrency.BankEngine;
import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TransactionsManagementPanel extends JPanel {

    private final JFrame owner;
    private final User user;
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final BankEngine engine;

    private JTable table;
    private DefaultTableModel model;

    public TransactionsManagementPanel(JFrame owner, User user, AccountDAO accountDAO,
                                       TransactionDAO transactionDAO, BankEngine engine) {
        this.owner = owner;
        this.user = user;
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.engine = engine;

        setLayout(new BorderLayout(12, 12));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        add(buildTop(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);

        refresh();
    }

    private JComponent buildTop() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("🧾 Transactions Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());

        top.add(title, BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);
        return top;
    }

    private JComponent buildTable() {
        model = new DefaultTableModel(new Object[]{"CREATED_AT", "TYPE", "ROUTE", "AMOUNT", "STATUS"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(26);

        return new JScrollPane(table);
    }

    private void refresh() {
        model.setRowCount(0);

        List<String> lines = transactionDAO.findAllHistoryLines();
        for (String line : lines) {
            // expected: created_at | type | from -> to | amount | status
            String[] p = line.split("\\s\\|\\s");
            if (p.length >= 5) {
                model.addRow(new Object[]{p[0], p[1], p[2], p[3], p[4]});
            } else {
                model.addRow(new Object[]{line, "", "", "", ""});
            }
        }
    }
}
