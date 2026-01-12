package org.example.bank.ui.admin;

import org.example.bank.auth.User;
import org.example.bank.concurrency.BankEngine;
import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.dao.UserDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class UsersManagementPanel extends JPanel {

    private final JFrame owner;
    private final User currentUser;
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final BankEngine engine;

    private JTable table;
    private DefaultTableModel model;

    // Colors for button style (same look as screenshot)
    private static final Color TEXT = new Color(33, 37, 41);
    private static final Color MUTED = new Color(108, 117, 125);
    private static final Color BORDER = new Color(222, 226, 230);
    private static final Color ACCENT = new Color(0, 122, 255);
    private static final Color DANGER = new Color(255, 59, 48);
    private static final Color PURPLE = new Color(88, 86, 214);
    private static final String FONT = "Segoe UI";
    private static final String EMOJI_FONT = "Segoe UI Emoji";

    public UsersManagementPanel(JFrame owner, User currentUser, AccountDAO accountDAO,
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

        refresh();
    }

    private JComponent buildTop() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("👥 Users Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton refresh = styledBtn("Refresh", "🔄", ACCENT);
        refresh.addActionListener(e -> refresh());
        actions.add(refresh);

        // ✅ Only ADMIN can create/delete users
        boolean isAdmin = (currentUser.getRole() != null)
                && currentUser.getRole().toString().equalsIgnoreCase("ADMIN");

        if (isAdmin) {
            JButton createClient = styledBtn("Create Client", "📝", ACCENT);
            JButton createAdmin  = styledBtn("Create Admin", "🛡️", PURPLE);
            JButton deleteUser   = styledBtn("Delete User", "🗑️", DANGER);

            createClient.addActionListener(e -> createUserDialog(false));
            createAdmin.addActionListener(e -> createUserDialog(true));
            deleteUser.addActionListener(e -> deleteSelectedUser());

            actions.add(createClient);
            actions.add(createAdmin);
            actions.add(deleteUser);
        }

        top.add(title, BorderLayout.WEST);
        top.add(actions, BorderLayout.EAST);
        return top;
    }

    private JComponent buildTable() {
        model = new DefaultTableModel(new Object[]{"ID", "USERNAME", "ROLE"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(26);

        JScrollPane sp = new JScrollPane(table);
        return sp;
    }

    private void refresh() {
        model.setRowCount(0);

        List<User> users = new UserDAO().findAll();
        for (User u : users) {
            model.addRow(new Object[]{u.getId(), u.getUsername(), u.getRole()});
        }
    }

    private void createUserDialog(boolean admin) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10,10,10,10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel uLbl = new JLabel("Username");
        uLbl.setFont(new Font(FONT, Font.BOLD, 13));
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=0;
        panel.add(uLbl, gbc);

        JTextField uField = new JTextField(18);
        gbc.gridx=1; gbc.gridy=0; gbc.weightx=1;
        panel.add(uField, gbc);

        JLabel pLbl = new JLabel("Password");
        pLbl.setFont(new Font(FONT, Font.BOLD, 13));
        gbc.gridx=0; gbc.gridy=1; gbc.weightx=0;
        panel.add(pLbl, gbc);

        JPasswordField pField = new JPasswordField(18);
        gbc.gridx=1; gbc.gridy=1; gbc.weightx=1;
        panel.add(pField, gbc);

        JLabel info = new JLabel(admin ? "Création d'un compte ADMIN (privilèges élevés)" : "Création d'un compte CLIENT");
        info.setFont(new Font(FONT, Font.PLAIN, 12));
        info.setForeground(MUTED);
        gbc.gridx=0; gbc.gridy=2; gbc.gridwidth=2;
        panel.add(info, gbc);

        int res = JOptionPane.showConfirmDialog(
                owner,
                panel,
                admin ? "Create Admin" : "Create Client",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (res != JOptionPane.OK_OPTION) return;

        String username = uField.getText().trim();
        String password = new String(pField.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Username and password required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UserDAO userDAO = new UserDAO();
        if (userDAO.usernameExists(username)) {
            JOptionPane.showMessageDialog(owner, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int newId = admin ? userDAO.createAdminReturnId(username, password)
                : userDAO.createClientReturnId(username, password);

        if (newId <= 0) {
            JOptionPane.showMessageDialog(owner, "Failed to create user.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ✅ default bank account for BOTH (client/admin) to avoid empty dashboard
        boolean okAcc = accountDAO.createDefaultAccountForUser(newId);

        if (okAcc) {
            JOptionPane.showMessageDialog(owner, "User + default account created ✅", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(owner, "User created but default account failed.", "Warning", JOptionPane.WARNING_MESSAGE);
        }

        refresh();
    }

    // ✅ Delete selected user + all his accounts (+ transactions if possible)
    private void deleteSelectedUser() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Sélectionne un utilisateur dans le tableau.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int userId = (int) model.getValueAt(row, 0);
        String username = String.valueOf(model.getValueAt(row, 1));
        String role = String.valueOf(model.getValueAt(row, 2));

        // 🔒 protection: do not allow deleting yourself
        if (currentUser.getId() == userId) {
            JOptionPane.showMessageDialog(owner, "Tu ne peux pas supprimer ton propre compte.", "Blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Optional: prevent deleting the last admin
        // (simple check: if role is ADMIN and only 1 admin exists)
        if ("ADMIN".equalsIgnoreCase(role)) {
            long admins = new UserDAO().findAll().stream()
                    .filter(u -> u.getRole() != null && u.getRole().toString().equalsIgnoreCase("ADMIN"))
                    .count();
            if (admins <= 1) {
                JOptionPane.showMessageDialog(owner, "Impossible de supprimer le dernier ADMIN.", "Blocked", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int confirm = JOptionPane.showConfirmDialog(
                owner,
                "Supprimer l'utilisateur '" + username + "' (ID=" + userId + ") + tous ses comptes ?\nCette action est irréversible.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        boolean ok = new UserDAO().deleteUserCascade(userId);

        if (ok) {
            JOptionPane.showMessageDialog(owner, "Utilisateur supprimé ✅", "Success", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        } else {
            JOptionPane.showMessageDialog(owner, "Échec de suppression (vérifie contraintes DB / transactions).", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ✅ Button style like your screenshot (white + border + emoji icon left)
    private JButton styledBtn(String text, String icon, Color color) {
        JButton b = new JButton();
        b.setLayout(new BorderLayout(10,0));
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER,1),
                new EmptyBorder(10,14,10,14)
        ));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setFont(new Font(FONT, Font.BOLD, 13));
        b.setPreferredSize(new Dimension(150, 44));

        JLabel ic = new JLabel(icon);
        ic.setFont(new Font(EMOJI_FONT, Font.PLAIN, 14));
        ic.setForeground(color);

        JLabel tx = new JLabel(text);
        tx.setFont(new Font(FONT, Font.BOLD, 13));
        tx.setForeground(TEXT);

        b.add(ic, BorderLayout.WEST);
        b.add(tx, BorderLayout.CENTER);

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(245, 247, 250));
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(color,1),
                        new EmptyBorder(10,14,10,14)
                ));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(Color.WHITE);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER,1),
                        new EmptyBorder(10,14,10,14)
                ));
            }
        });

        return b;
    }
}
