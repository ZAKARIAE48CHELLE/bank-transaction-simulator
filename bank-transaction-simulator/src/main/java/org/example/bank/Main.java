package org.example.bank;

import org.example.bank.auth.LoginService;
import org.example.bank.auth.User;
import org.example.bank.concurrency.BankEngine;
import org.example.bank.concurrency.Worker;
import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.dao.UserDAO;
import org.example.bank.ui.admin.AdminDashboardPanel;
import org.example.bank.ui.client.ClientDashboardPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main extends JFrame {

    private User user;

    private final LoginService loginService = new LoginService();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    private final BankEngine engine = new BankEngine();
    private Worker[] workers;

    public Main() {
        setSystemLookAndFeel();

        setTitle("Transaction Simulator");
        setSize(1120, 720);
        setMinimumSize(new Dimension(1040, 660));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exit(); }
        });

        // 1) Login
        user = showLoginDialog();
        if (user == null) {
            System.exit(0);
            return;
        }

        // 2) Start workers
        startWorkers();

        // 3) Route UI
        buildUI();

        setVisible(true);
    }

    private void buildUI() {
        Runnable onExit = this::exit;
        Runnable onSignOut = this::signOut;

        String role = (user.getRole() == null) ? "" : user.getRole().toString().toUpperCase();

        if (role.contains("ADMIN")) {
            setContentPane(new AdminDashboardPanel(this, user, accountDAO, transactionDAO, engine, onExit, onSignOut));
        } else {
            setContentPane(new ClientDashboardPanel(this, user, accountDAO, transactionDAO, engine, onExit , onSignOut));
        }

        revalidate();
        repaint();
    }

    private void signOut() {
        // stop UI, keep app open
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Do you want to sign out?",
                "Sign out",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        // show login again (same dialog)
        user = showLoginDialog();
        if (user == null) {
            exit();
            return;
        }
        buildUI();
    }


    private void startWorkers() {
        workers = new Worker[3];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(engine, i + 1);
            workers[i].start();
        }
    }

    private void shutdownWorkers() {
        if (workers == null) return;
        for (Worker w : workers) w.shutdown();
        for (Worker w : workers) {
            try { w.join(800); } catch (InterruptedException ignored) {}
        }
        workers = null;
    }

    private void exit() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        shutdownWorkers();
        dispose();
        System.exit(0);
    }

    // ---------- LOOK & FEEL ----------
    private void setSystemLookAndFeel() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
    }

    // ===== Theme =====
    private static final Color TEXT = new Color(33, 37, 41);
    private static final Color MUTED = new Color(108, 117, 125);
    private static final Color ACCENT = new Color(0, 122, 255);
    private static final Color DANGER = new Color(255, 59, 48);
    private static final Color BORDER = new Color(222, 226, 230);

    private static final String FONT = "Segoe UI";
    private static final String EMOJI_FONT = "Segoe UI Emoji";
    private static final int BASE = 13;
    private static final int SMALL = 12;

    private static Font f(int style, int size) { return new Font(FONT, style, size); }
    private static Font e(int style, int size) { return new Font(EMOJI_FONT, style, size); }

    private void styleTextField(JTextField field) {
        field.setFont(f(Font.PLAIN, BASE));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new javax.swing.border.EmptyBorder(10, 12, 10, 12)
        ));
        field.setPreferredSize(new Dimension(320, 42));
    }

    private JButton createPrimaryButton(String text, String icon, Color color, boolean filled) {
        JButton b = new JButton();
        b.setLayout(new BorderLayout(10, 0));
        b.setFont(f(Font.BOLD, BASE));
        b.setBackground(filled ? color : Color.WHITE);
        b.setForeground(filled ? Color.WHITE : color);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, filled ? 0 : 2),
                new javax.swing.border.EmptyBorder(10, 14, 10, 14)
        ));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel ic = new JLabel(icon);
        ic.setFont(e(Font.PLAIN, 14));
        ic.setForeground(filled ? Color.WHITE : color);

        JLabel tx = new JLabel(text);
        tx.setFont(f(Font.BOLD, BASE));
        tx.setForeground(filled ? Color.WHITE : color);

        b.add(ic, BorderLayout.WEST);
        b.add(tx, BorderLayout.CENTER);

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (filled) b.setBackground(color.darker());
                else b.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 12));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent evt) {
                b.setBackground(filled ? color : Color.WHITE);
            }
        });

        return b;
    }

    // ---------- LOGIN DIALOG ----------
    private User showLoginDialog() {

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new javax.swing.border.EmptyBorder(24, 24, 24, 24));
        root.setBackground(Color.WHITE);

        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Welcome back");
        title.setFont(f(Font.BOLD, 24));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Please sign in to continue");
        subtitle.setFont(f(Font.PLAIN, BASE));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitle);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(f(Font.BOLD, BASE));
        userLbl.setForeground(TEXT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(userLbl, gbc);

        JTextField userField = new JTextField(22);
        styleTextField(userField);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        formPanel.add(userField, gbc);

        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(f(Font.BOLD, BASE));
        passLbl.setForeground(TEXT);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(passLbl, gbc);

        JPasswordField passField = new JPasswordField(22);
        styleTextField(passField);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        formPanel.add(passField, gbc);

        JPanel hint = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hint.setBackground(Color.WHITE);
        JLabel hintLbl = new JLabel("Tip: press Enter to login");
        hintLbl.setFont(f(Font.PLAIN, SMALL));
        hintLbl.setForeground(MUTED);
        hint.add(hintLbl);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        formPanel.add(hint, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);

        JButton cancelBtn = createPrimaryButton("Exit", "🚪", DANGER, false);
        JButton signupBtn = createPrimaryButton("Create client account", "📝", MUTED, false);
        JButton loginBtn  = createPrimaryButton("Login", "🔒", ACCENT, true);

        buttonPanel.add(cancelBtn);
        buttonPanel.add(signupBtn);
        buttonPanel.add(loginBtn);

        root.add(titlePanel, BorderLayout.NORTH);
        root.add(formPanel, BorderLayout.CENTER);
        root.add(buttonPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(this, "Login", true);
        dialog.setContentPane(root);
        dialog.setSize(520, 380);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);

        final User[] result = { null };

        Runnable doLogin = () -> {
            User u = loginService.login(
                    userField.getText().trim(),
                    new String(passField.getPassword())
            );

            if (u != null) {
                result[0] = u;
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Invalid username or password. Please try again.",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE
                );
                passField.setText("");
                userField.requestFocusInWindow();
            }
        };

        loginBtn.addActionListener(e -> doLogin.run());

        cancelBtn.addActionListener(e -> {
            result[0] = null;
            dialog.dispose();
        });

        // ✅ Create CLIENT only + create default account
        signupBtn.addActionListener(e -> showSignupDialog(dialog));

        dialog.getRootPane().setDefaultButton(loginBtn);
        dialog.setVisible(true);

        return result[0];
    }

    private void showSignupDialog(JDialog parent) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new javax.swing.border.EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel uLbl = new JLabel("Username");
        uLbl.setFont(f(Font.BOLD, BASE));
        uLbl.setForeground(TEXT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(uLbl, gbc);

        JTextField uField = new JTextField(18);
        styleTextField(uField);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
        panel.add(uField, gbc);

        JLabel pLbl = new JLabel("Password");
        pLbl.setFont(f(Font.BOLD, BASE));
        pLbl.setForeground(TEXT);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(pLbl, gbc);

        JPasswordField pField = new JPasswordField(18);
        styleTextField(pField);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1;
        panel.add(pField, gbc);

        JLabel info = new JLabel("Note: only CLIENT accounts can be created here.");
        info.setFont(f(Font.PLAIN, SMALL));
        info.setForeground(MUTED);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(info, gbc);

        int res = JOptionPane.showConfirmDialog(
                parent,
                panel,
                "Create Client Account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (res != JOptionPane.OK_OPTION) return;

        String username = uField.getText().trim();
        String password = new String(pField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Username and password required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        UserDAO userDAO = new UserDAO();
        if (userDAO.usernameExists(username)) {
            JOptionPane.showMessageDialog(parent, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ✅ create client + get id
        int newUserId = userDAO.createClientReturnId(username, password);
        if (newUserId <= 0) {
            JOptionPane.showMessageDialog(parent, "Failed to create account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ✅ create default account
        boolean okAcc = accountDAO.createDefaultAccountForUser(newUserId);

        if (okAcc) {
            JOptionPane.showMessageDialog(parent, "Client account + default bank account created ✅", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent, "Client created but default account failed.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}