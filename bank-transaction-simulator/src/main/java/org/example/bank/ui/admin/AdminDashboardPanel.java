package org.example.bank.ui.admin;

import org.example.bank.auth.User;
import org.example.bank.concurrency.BankEngine;
import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminDashboardPanel extends JPanel {

    public AdminDashboardPanel(JFrame owner,
                               User user,
                               AccountDAO accountDAO,
                               TransactionDAO transactionDAO,
                               BankEngine engine,
                               Runnable onExit) {

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(14, 18, 14, 18));
        top.setBackground(Color.WHITE);

        JLabel title = new JLabel("Admin Dashboard (WIP)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton exit = new JButton("Exit");
        exit.addActionListener(e -> onExit.run());

        top.add(title, BorderLayout.WEST);
        top.add(exit, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Users", new UsersManagementPanel());
        tabs.addTab("Accounts", new AccountsManagementPanel());
        tabs.addTab("Transactions", new TransactionsManagementPanel());

        add(tabs, BorderLayout.CENTER);
    }
}
