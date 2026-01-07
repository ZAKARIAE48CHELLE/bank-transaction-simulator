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
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Main extends JFrame {

    // ===== Backend (same logic) =====
    private User user;
    private final LoginService loginService = new LoginService();
    private final AccountDAO accountDAO = new AccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    private final BankEngine engine = new BankEngine();
    private Worker[] workers;

    // ===== UI components =====
    private JComboBox<Account> accountBox;
    private JLabel userLabel;
    private JLabel roleLabel;
    private JLabel balanceLabel;

    private DefaultListModel<HistoryItem> historyModel;
    private JList<HistoryItem> historyList;

    // ===== Theme =====
    private static final Color BG = Color.WHITE;
    private static final Color PANEL = new Color(248, 249, 250);
    private static final Color TEXT = new Color(33, 37, 41);
    private static final Color MUTED = new Color(108, 117, 125);
    private static final Color ACCENT = new Color(0, 122, 255);
    private static final Color ACCENT_2 = new Color(88, 86, 214);
    private static final Color DANGER = new Color(255, 59, 48);
    private static final Color WARNING = new Color(255, 149, 0);
    private static final Color SUCCESS = new Color(52, 199, 89);
    private static final Color BORDER = new Color(222, 226, 230);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ===== Fonts (clean + consistent) =====
    private static final String FONT = "Segoe UI";
    private static final String EMOJI_FONT = "Segoe UI Emoji";
    private static final int H1 = 20;
    private static final int H2 = 16;
    private static final int BASE = 13;
    private static final int SMALL = 12;

    private static Font f(int style, int size) { return new Font(FONT, style, size); }
    private static Font e(int style, int size) { return new Font(EMOJI_FONT, style, size); }

    // ================== CONSTRUCTOR ==================
    public Main() {
        setSystemLookAndFeel();

        setTitle("Transaction Simulator");
        setSize(1120, 720);
        setMinimumSize(new Dimension(1040, 660));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exit(); }
        });

        loginDialog();
        buildUI();
        startWorkers();

        refreshAccounts(true);
        refreshHistory();

        setVisible(true);
    }

    // ================== LOGIN ==================
    private void loginDialog() {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(24, 24, 24, 24));
        root.setBackground(Color.WHITE);

        // Title
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

        // Form
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

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        JButton cancelBtn = createPrimaryButton("Exit", "🚪", DANGER, false);
        JButton loginBtn = createPrimaryButton("Login", "🔒", ACCENT, true);
        buttonPanel.add(cancelBtn);
        buttonPanel.add(loginBtn);

        root.add(titlePanel, BorderLayout.NORTH);
        root.add(formPanel, BorderLayout.CENTER);
        root.add(buttonPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(this, "Login", true);
        dialog.setContentPane(root);
        dialog.setSize(520, 380);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);

        Runnable doLogin = () -> {
            user = loginService.login(
                    userField.getText().trim(),
                    new String(passField.getPassword())
            );
            if (user != null) {
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
        cancelBtn.addActionListener(e -> System.exit(0));
        dialog.getRootPane().setDefaultButton(loginBtn);

        dialog.setVisible(true);
    }

    // ================== UI ==================
    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        add(createTopBar(), BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(340);
        mainSplit.setDividerSize(1);
        mainSplit.setContinuousLayout(true);
        mainSplit.setBorder(null);
        mainSplit.setBackground(BG);

        mainSplit.setLeftComponent(createSidebar());
        mainSplit.setRightComponent(createMainContent());

        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel createTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(14, 18, 14, 18)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setBackground(Color.WHITE);

        JLabel icon = new JLabel("💳");
        icon.setFont(e(Font.PLAIN, 18));

        JLabel title = new JLabel("Transaction Dashboard");
        title.setFont(f(Font.BOLD, H1));
        title.setForeground(TEXT);

        left.add(icon);
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(Color.WHITE);

        JLabel userIco = new JLabel("👤");
        userIco.setFont(e(Font.PLAIN, 16));

        userLabel = new JLabel(user.getUsername());
        userLabel.setFont(f(Font.BOLD, BASE));
        userLabel.setForeground(TEXT);

        roleLabel = new JLabel("(" + user.getRole() + ")");
        roleLabel.setFont(f(Font.PLAIN, BASE));
        roleLabel.setForeground(MUTED);

        right.add(userIco);
        right.add(userLabel);
        right.add(roleLabel);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(PANEL);
        sidebar.setBorder(new EmptyBorder(18, 18, 18, 18));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(340, 0));

        // ---- Account Card (FIXED: no weird big empty box, balanced spacing) ----
        JPanel accountCard = createCardPanel("Account", "👤");

        JLabel selLbl = new JLabel("Selected account");
        selLbl.setFont(f(Font.BOLD, BASE));
        selLbl.setForeground(TEXT);
        selLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        accountBox = new JComboBox<>(new Account[0]);
        styleComboBox(accountBox);
        setupAccountBoxRenderer();
        accountBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountBox.addActionListener(e -> {
            updateBalanceDisplay();
            refreshHistory();
        });

        JPanel balanceRow = new JPanel(new BorderLayout());
        balanceRow.setOpaque(false);
        balanceRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel balTitle = new JLabel("Available balance");
        balTitle.setFont(f(Font.PLAIN, SMALL));
        balTitle.setForeground(MUTED);

        balanceLabel = new JLabel("0.00 DH");
        balanceLabel.setFont(f(Font.BOLD, 22));
        balanceLabel.setForeground(ACCENT);

        balanceRow.add(balTitle, BorderLayout.WEST);
        balanceRow.add(balanceLabel, BorderLayout.EAST);

        accountCard.add(selLbl);
        accountCard.add(Box.createVerticalStrut(8));
        accountCard.add(accountBox);
        accountCard.add(Box.createVerticalStrut(12));
        accountCard.add(balanceRow);

        sidebar.add(accountCard);
        sidebar.add(Box.createVerticalStrut(14));

        // ---- Quick Actions ----
        JPanel actionsCard = createCardPanel("Quick actions", "💡");

        JButton refreshBtn = createSidebarButton("Refresh data", "🔄", ACCENT);
        JButton exitBtn = createSidebarButton("Exit", "🚪", DANGER);

        refreshBtn.addActionListener(e -> {
            refreshAccounts(false);
            refreshHistory();
        });
        exitBtn.addActionListener(e -> exit());

        actionsCard.add(refreshBtn);
        actionsCard.add(Box.createVerticalStrut(8));
        actionsCard.add(exitBtn);

        sidebar.add(actionsCard);
        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JPanel createMainContent() {
        JPanel main = new JPanel(new BorderLayout(14, 14));
        main.setBackground(BG);
        main.setBorder(new EmptyBorder(18, 18, 18, 18));

        main.add(createHistorySection(), BorderLayout.CENTER);
        main.add(createActionButtons(), BorderLayout.SOUTH);

        return main;
    }

    private JPanel createHistorySection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);

        JPanel titles = new JPanel();
        titles.setBackground(BG);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("Transaction History");
        t.setFont(f(Font.BOLD, 18));
        t.setForeground(TEXT);

        JLabel sub = new JLabel("Easy-to-read history for normal users");
        sub.setFont(f(Font.PLAIN, BASE));
        sub.setForeground(MUTED);

        titles.add(t);
        titles.add(Box.createVerticalStrut(4));
        titles.add(sub);

        header.add(titles, BorderLayout.WEST);

        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setBackground(Color.WHITE);
        historyList.setCellRenderer(new FriendlyHistoryRenderer());
        historyList.setFixedCellHeight(76);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setBorder(BorderFactory.createEmptyBorder());

        JScrollPane scroll = new JScrollPane(historyList);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JScrollBar vertical = scroll.getVerticalScrollBar();
        vertical.setUI(new ModernScrollBarUI());

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionButtons() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        row.setBackground(BG);

        JButton depositBtn = createActionButton("Deposit", "➕", ACCENT);
        JButton withdrawBtn = createActionButton("Withdraw", "➖", WARNING);
        JButton transferBtn = createActionButton("Transfer", "🔁", ACCENT_2);
        JButton historyBtn = createActionButton("Refresh History", "📜", MUTED);

        depositBtn.addActionListener(e -> deposit());
        withdrawBtn.addActionListener(e -> withdraw());
        transferBtn.addActionListener(e -> transfer());
        historyBtn.addActionListener(e -> refreshHistory());

        row.add(depositBtn);
        row.add(withdrawBtn);
        row.add(transferBtn);
        row.add(historyBtn);

        return row;
    }

    // ================== COMPONENT FACTORIES ==================
    private JPanel createCardPanel(String title, String icon) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(14, 14, 14, 14)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);

        JLabel left = new JLabel(icon + "  " + title);
        left.setFont(f(Font.BOLD, H2));
        left.setForeground(TEXT);

        head.add(left, BorderLayout.WEST);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(head);
        card.add(Box.createVerticalStrut(12));

        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JButton createSidebarButton(String text, String icon, Color color) {
        JButton b = new JButton();
        b.setLayout(new BorderLayout(10, 0));
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ic = new JLabel(icon);
        ic.setFont(e(Font.PLAIN, 14));
        ic.setForeground(color);

        JLabel tx = new JLabel(text);
        tx.setFont(f(Font.PLAIN, BASE));
        tx.setForeground(TEXT);

        b.add(ic, BorderLayout.WEST);
        b.add(tx, BorderLayout.CENTER);

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(new Color(245, 247, 250)); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(Color.WHITE); }
        });

        return b;
    }

    private JButton createActionButton(String text, String icon, Color border) {
        JButton b = new JButton();
        b.setLayout(new BorderLayout(10, 0));
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                new EmptyBorder(12, 18, 12, 18)
        ));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel ic = new JLabel(icon);
        ic.setFont(e(Font.PLAIN, 14));
        ic.setForeground(border);

        JLabel tx = new JLabel(text);
        tx.setFont(f(Font.BOLD, BASE));
        tx.setForeground(TEXT);

        b.add(ic, BorderLayout.WEST);
        b.add(tx, BorderLayout.CENTER);

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setBackground(new Color(border.getRed(), border.getGreen(), border.getBlue(), 12));
            }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(Color.WHITE); }
        });

        return b;
    }

    private JButton createPrimaryButton(String text, String icon, Color color, boolean filled) {
        JButton b = new JButton();
        b.setLayout(new BorderLayout(10, 0));
        b.setFont(f(Font.BOLD, BASE));
        b.setBackground(filled ? color : Color.WHITE);
        b.setForeground(filled ? Color.WHITE : color);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, filled ? 0 : 2),
                new EmptyBorder(10, 14, 10, 14)
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

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (filled) b.setBackground(color.darker());
                else b.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 12));
            }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(filled ? color : Color.WHITE); }
        });

        return b;
    }

    // ================== STYLING HELPERS ==================
    private void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            UIManager.put("Label.font", f(Font.PLAIN, BASE));
            UIManager.put("Button.font", f(Font.BOLD, BASE));
            UIManager.put("TextField.font", f(Font.PLAIN, BASE));
            UIManager.put("PasswordField.font", f(Font.PLAIN, BASE));
            UIManager.put("ComboBox.font", f(Font.PLAIN, BASE));
            UIManager.put("List.font", f(Font.PLAIN, BASE));
            UIManager.put("OptionPane.messageFont", f(Font.PLAIN, BASE));
            UIManager.put("OptionPane.buttonFont", f(Font.BOLD, BASE));
        } catch (Exception ignored) {}
    }

    private void styleTextField(JTextField field) {
        field.setFont(f(Font.PLAIN, BASE));
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        field.setPreferredSize(new Dimension(320, 42));
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(f(Font.PLAIN, BASE));
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXT);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        combo.setPreferredSize(new Dimension(260, 40));
    }

    // ================== ACTIONS ==================
    private void deposit() {
        Account acc = getSelectedAccountOrWarn();
        if (acc == null) return;

        double amount = askAmount("Enter deposit amount (DH):");
        if (amount <= 0) return;

        engine.submit(new DepositTransaction(acc, amount));
        showMessage("Deposit submitted successfully.");
        delayedRefresh();
    }

    private void withdraw() {
        Account acc = getSelectedAccountOrWarn();
        if (acc == null) return;

        double amount = askAmount("Enter withdrawal amount (DH):");
        if (amount <= 0) return;

        engine.submit(new WithdrawTransaction(acc, amount));
        showMessage("Withdrawal submitted successfully.");
        delayedRefresh();
    }

    private void transfer() {
        Account from = getSelectedAccountOrWarn();
        if (from == null) return;

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel toLabel = new JLabel("Target account number");
        toLabel.setFont(f(Font.BOLD, BASE));
        toLabel.setForeground(TEXT);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(toLabel, gbc);

        JTextField toRefField = new JTextField(18);
        styleTextField(toRefField);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        panel.add(toRefField, gbc);

        JLabel amountLabel = new JLabel("Amount (DH)");
        amountLabel.setFont(f(Font.BOLD, BASE));
        amountLabel.setForeground(TEXT);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(amountLabel, gbc);

        JTextField amountField = new JTextField(18);
        styleTextField(amountField);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        panel.add(amountField, gbc);

        JLabel infoLabel = new JLabel("From: " + from.getAccountRef());
        infoLabel.setFont(f(Font.PLAIN, SMALL));
        infoLabel.setForeground(MUTED);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(infoLabel, gbc);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Transfer",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) return;

        String toRef = toRefField.getText().trim();
        if (toRef.isEmpty()) { showError("Please enter target account number."); return; }

        Account to = accountDAO.findByAccountRef(toRef);
        if (to == null) { showError("Account not found: " + toRef); return; }

        if (to.getAccountRef().equals(from.getAccountRef())) {
            showError("Cannot transfer to the same account.");
            return;
        }

        double amount;
        try { amount = Double.parseDouble(amountField.getText().trim()); }
        catch (Exception e) { showError("Please enter a valid amount."); return; }

        if (amount <= 0) { showError("Amount must be greater than 0."); return; }

        engine.submit(new TransferTransaction(from, to, amount));
        showMessage("Transfer submitted successfully.");
        delayedRefresh();
    }

    private void exit() {
        int confirm = JOptionPane.showConfirmDialog(
                this, "Are you sure you want to exit?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        shutdownWorkers();
        dispose();
        System.exit(0);
    }

    // ================== DATA REFRESH ==================
    private void refreshAccounts(boolean firstTime) {
        List<Account> list = accountDAO.findByUserId(user.getId());
        Account previouslySelected = (Account) accountBox.getSelectedItem();

        DefaultComboBoxModel<Account> model = new DefaultComboBoxModel<>();
        if (list != null) for (Account a : list) model.addElement(a);

        accountBox.setModel(model);
        setupAccountBoxRenderer();

        if (!firstTime && previouslySelected != null && list != null) {
            for (Account a : list) {
                if (a.getAccountRef().equals(previouslySelected.getAccountRef())) {
                    accountBox.setSelectedItem(a);
                    break;
                }
            }
        }

        updateBalanceDisplay();
    }

    private void refreshHistory() {
        historyModel.clear();

        Account acc = (Account) accountBox.getSelectedItem();
        if (acc == null) {
            historyModel.addElement(HistoryItem.raw("Select an account to view transactions."));
            return;
        }

        List<String> history = transactionDAO.findHistoryByAccountRef(acc.getAccountRef());
        if (history == null || history.isEmpty()) {
            historyModel.addElement(HistoryItem.raw("No transactions found yet."));
            return;
        }

        for (String line : history) historyModel.addElement(parseHistory(line));
    }

    private HistoryItem parseHistory(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length >= 5) {
                String createdAt = parts[0].trim();
                String type = parts[1].trim();
                String route = parts[2].trim();
                String amountStr = parts[3].trim();
                String status = parts[4].trim();

                String from = "";
                String to = "";

                // route is like: "ACC-1 -> ACC-3" or "null -> ACC-1"
                String[] rt = route.split("->");
                if (rt.length == 2) {
                    from = safeEndpoint(rt[0]);
                    to = safeEndpoint(rt[1]);
                } else {
                    from = safeEndpoint(route);
                }

                double amount = 0.0;
                try { amount = Double.parseDouble(amountStr); } catch (Exception ignored) {}

                // Friendly cleanup for non-dev people
                if ("DEPOSIT".equalsIgnoreCase(type)) {
                    if (from.isBlank()) from = "Cash";
                    if (to.isBlank()) to = "Your account";
                } else if ("WITHDRAW".equalsIgnoreCase(type)) {
                    if (to.isBlank()) to = "Cash";
                    if (from.isBlank()) from = "Your account";
                }

                return new HistoryItem(createdAt, type, from, to, amount, status, line);
            }
        } catch (Exception ignored) {}

        return HistoryItem.raw(line);
    }

    private String safeEndpoint(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.equalsIgnoreCase("null") || t.equalsIgnoreCase("none")) return "";
        return t;
    }

    private void delayedRefresh() {
        new Timer(500, e -> {
            ((Timer) e.getSource()).stop();
            refreshAccounts(false);
            refreshHistory();
        }).start();
    }

    // ================== HELPERS ==================
    private Account getSelectedAccountOrWarn() {
        Account acc = (Account) accountBox.getSelectedItem();
        if (acc == null) showError("Please select an account first.");
        return acc;
    }

    private double askAmount(String title) {
        JTextField amountField = new JTextField();
        styleTextField(amountField);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));
        panel.setBackground(Color.WHITE);

        JLabel label = new JLabel(title);
        label.setFont(f(Font.BOLD, BASE));
        label.setForeground(TEXT);

        panel.add(label, BorderLayout.NORTH);
        panel.add(amountField, BorderLayout.CENTER);

        int res = JOptionPane.showConfirmDialog(
                this, panel, "Amount",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (res != JOptionPane.OK_OPTION) return -1;

        try {
            double a = Double.parseDouble(amountField.getText().trim());
            if (a <= 0) {
                showError("Amount must be greater than 0.");
                return -1;
            }
            return a;
        } catch (Exception e) {
            showError("Please enter a valid amount.");
            return -1;
        }
    }

    private void updateBalanceDisplay() {
        Account selected = (Account) accountBox.getSelectedItem();
        if (selected == null) {
            balanceLabel.setText("0.00 DH");
            balanceLabel.setForeground(MUTED);
            return;
        }

        Account fresh = accountDAO.findByAccountRef(selected.getAccountRef());
        if (fresh == null) {
            balanceLabel.setText("0.00 DH");
            balanceLabel.setForeground(MUTED);
            return;
        }

        double bal = fresh.getBalance();
        balanceLabel.setText(String.format("%.2f DH", bal));

        if (bal < 0) balanceLabel.setForeground(DANGER);
        else if (bal < 100) balanceLabel.setForeground(WARNING);
        else balanceLabel.setForeground(SUCCESS);
    }

    private void setupAccountBoxRenderer() {
        if (accountBox == null) return;

        accountBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof Account acc) {
                    setText("💳" + acc.getAccountRef() + "   •   " + String.format("%.2f DH", acc.getBalance()));
                }
                setFont(f(Font.PLAIN, BASE));
                setBorder(new EmptyBorder(8, 10, 8, 10));
                return this;
            }
        });
    }

    private void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
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
        if (workers == null) return;
        for (Worker w : workers) w.shutdown();
        for (Worker w : workers) {
            try { w.join(800); } catch (InterruptedException ignored) {}
        }
    }

    // ================== MAIN ==================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }

    // ================== INNER CLASSES ==================
    private static class HistoryItem {
        final String createdAt;
        final String type;
        final String from;
        final String to;
        final double amount;
        final String status;
        final String raw;

        HistoryItem(String createdAt, String type, String from, String to, double amount, String status, String raw) {
            this.createdAt = createdAt;
            this.type = type;
            this.from = from;
            this.to = to;
            this.amount = amount;
            this.status = status;
            this.raw = raw;
        }

        static HistoryItem raw(String raw) {
            return new HistoryItem("", "", "", "", 0.0, "", raw);
        }

        boolean isRawOnly() {
            return (type == null || type.isBlank()) && (raw != null && !raw.isBlank());
        }
    }

    /**
     * Transaction renderer for normal users:
     * - clear icon + label
     * - readable "From → To"
     * - big amount, smaller time
     */
    private class FriendlyHistoryRenderer extends JPanel implements ListCellRenderer<HistoryItem> {

        private final JLabel titleLbl = new JLabel();
        private final JLabel routeLbl = new JLabel();
        private final JLabel amountLbl = new JLabel();
        private final JLabel timeLbl = new JLabel();

        FriendlyHistoryRenderer() {
            setLayout(new BorderLayout(12, 4));
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(10, 14, 10, 14));

            titleLbl.setFont(f(Font.BOLD, BASE));
            routeLbl.setFont(f(Font.PLAIN, SMALL));
            routeLbl.setForeground(MUTED);

            amountLbl.setFont(f(Font.BOLD, 17));
            timeLbl.setFont(f(Font.PLAIN, SMALL));
            timeLbl.setForeground(MUTED);

            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.add(titleLbl);
            left.add(Box.createVerticalStrut(6));
            left.add(routeLbl);

            JPanel right = new JPanel();
            right.setOpaque(false);
            right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
            right.setAlignmentX(Component.RIGHT_ALIGNMENT);
            amountLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
            timeLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
            right.add(amountLbl);
            right.add(Box.createVerticalStrut(6));
            right.add(timeLbl);

            add(left, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends HistoryItem> list,
                HistoryItem value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            setBackground(isSelected ? new Color(240, 245, 255) : Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    new EmptyBorder(10, 14, 10, 14)
            ));

            if (value == null || value.isRawOnly()) {
                titleLbl.setText(value == null ? "" : value.raw);
                titleLbl.setForeground(MUTED);
                routeLbl.setText("");
                amountLbl.setText("");
                timeLbl.setText("");
                return this;
            }

            String t = value.type == null ? "" : value.type.toUpperCase();

            String icon = "•";
            String label = "Transaction";
            Color c = MUTED;

            switch (t) {
                case "DEPOSIT" -> { icon = "➕"; label = "Deposit"; c = SUCCESS; }
                case "WITHDRAW" -> { icon = "➖"; label = "Withdraw"; c = WARNING; }
                case "TRANSFER" -> { icon = "🔁"; label = "Transfer"; c = ACCENT; }
                default -> { icon = "•"; label = value.type; c = MUTED; }
            }

            titleLbl.setText(icon + "  " + label);
            titleLbl.setForeground(c);

            String from = (value.from == null || value.from.isBlank()) ? "—" : value.from;
            String to = (value.to == null || value.to.isBlank()) ? "—" : value.to;

            // Make Deposit/Withdraw clearer
            if ("DEPOSIT".equalsIgnoreCase(t)) {
                routeLbl.setText("To: " + to);
            } else if ("WITHDRAW".equalsIgnoreCase(t)) {
                routeLbl.setText("From: " + from);
            } else {
                routeLbl.setText(from + "  →  " + to);
            }

            amountLbl.setText(String.format("%.2f DH", value.amount));
            amountLbl.setForeground(c);

            String when = value.createdAt;
            if (when == null || when.isBlank()) when = LocalDateTime.now().format(TIME_FORMAT);
            timeLbl.setText(when);

            return this;
        }
    }

    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(200, 205, 210);
            this.trackColor = new Color(245, 247, 250);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) { return invisible(); }

        @Override
        protected JButton createIncreaseButton(int orientation) { return invisible(); }

        private JButton invisible() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 10, 10);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(trackColor);
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.dispose();
        }
    }
}
