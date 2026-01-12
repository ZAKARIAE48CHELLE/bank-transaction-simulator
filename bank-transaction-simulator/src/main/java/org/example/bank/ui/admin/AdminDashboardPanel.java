package org.example.bank.ui.admin;

import org.example.bank.auth.User;
import org.example.bank.concurrency.BankEngine;
import org.example.bank.dao.AccountDAO;
import org.example.bank.dao.TransactionDAO;
import org.example.bank.dao.UserDAO;
import org.example.bank.model.Account;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AdminDashboardPanel extends JPanel {

    private final JFrame owner;
    private final User user;
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final BankEngine engine;

    private final Runnable onExit;
    private final Runnable onSignOut;

    // ===== Theme (SAME AS CLIENT) =====
    private static final Color BG = Color.WHITE;
    private static final Color PANEL = new Color(248, 249, 250);
    private static final Color TEXT = new Color(33, 37, 41);
    private static final Color MUTED = new Color(108, 117, 125);
    private static final Color ACCENT = new Color(0, 122, 255);
    private static final Color ACCENT_2 = new Color(88, 86, 214);
    private static final Color DANGER = new Color(255, 59, 48);
    private static final Color BORDER = new Color(222, 226, 230);

    private static final String FONT = "Segoe UI";
    private static final String EMOJI_FONT = "Segoe UI Emoji";
    private static final int H1 = 20;
    private static final int H2 = 16;
    private static final int BASE = 13;
    private static final int SMALL = 12;

    private static Font f(int style, int size) { return new Font(FONT, style, size); }
    private static Font e(int style, int size) { return new Font(EMOJI_FONT, style, size); }

    // Sidebar labels (KPIs)
    private JLabel totalUsers;
    private JLabel totalAccounts;
    private JLabel totalBalance;
    private JLabel totalTx;

    // Top clients
    private DefaultListModel<String> topClientsModel;

    // Charts
    private PieChartPanel pieByType;
    private BarChartPanel barByStatus;

    public AdminDashboardPanel(JFrame owner,
                               User user,
                               AccountDAO accountDAO,
                               TransactionDAO transactionDAO,
                               BankEngine engine,
                               Runnable onExit,
                               Runnable onSignOut) {
        this.owner = owner;
        this.user = user;
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.engine = engine;
        this.onExit = onExit;
        this.onSignOut = onSignOut;

        setLayout(new BorderLayout());
        setBackground(BG);

        add(createTopBar(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);

        refreshStats();
    }

    // ================= TOP BAR =================
    private JPanel createTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(14, 18, 14, 18)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setBackground(Color.WHITE);

        JLabel icon = new JLabel("🏦");
        icon.setFont(e(Font.PLAIN, 18));

        JLabel title = new JLabel("Admin Dashboard");
        title.setFont(f(Font.BOLD, H1));
        title.setForeground(TEXT);

        left.add(icon);
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(Color.WHITE);

        JLabel userIco = new JLabel("👤");
        userIco.setFont(e(Font.PLAIN, 16));

        JLabel userLabel = new JLabel(user.getUsername());
        userLabel.setFont(f(Font.BOLD, BASE));
        userLabel.setForeground(TEXT);

        JLabel roleLabel = new JLabel("(" + user.getRole() + ")");
        roleLabel.setFont(f(Font.PLAIN, BASE));
        roleLabel.setForeground(MUTED);

        JButton signOutBtn = createActionButton("Sign out", "📋", ACCENT_2, false);
        JButton exitBtn = createActionButton("Exit", "⛔", DANGER, false);

        signOutBtn.addActionListener(ev -> onSignOut.run());
        exitBtn.addActionListener(ev -> onExit.run());

        right.add(userIco);
        right.add(userLabel);
        right.add(roleLabel);
        right.add(signOutBtn);
        right.add(exitBtn);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        return top;
    }

    // ================= BODY (SIDEBAR + MAIN) =================
    private JComponent createBody() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(340);
        split.setDividerSize(1);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setBackground(BG);

        split.setLeftComponent(createSidebar());
        split.setRightComponent(createMain());

        return split;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(PANEL);
        sidebar.setBorder(new EmptyBorder(18, 18, 18, 18));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(340, 0));

        JPanel kpiCard = createCardPanel("Overview", "📌");

        totalUsers = new JLabel(kpiHtml("Total Users", "—"));
        totalAccounts = new JLabel(kpiHtml("Total Accounts", "—"));
        totalBalance = new JLabel(kpiHtml("Total Balance", "—"));
        totalTx = new JLabel(kpiHtml("Total Transactions", "—"));

        kpiCard.add(totalUsers);
        kpiCard.add(Box.createVerticalStrut(8));
        kpiCard.add(totalAccounts);
        kpiCard.add(Box.createVerticalStrut(8));
        kpiCard.add(totalBalance);
        kpiCard.add(Box.createVerticalStrut(8));
        kpiCard.add(totalTx);

        sidebar.add(kpiCard);
        sidebar.add(Box.createVerticalStrut(14));

        JPanel topCard = createCardPanel("Top Clients", "🏅");
        topClientsModel = new DefaultListModel<>();
        JList<String> list = new JList<>(topClientsModel);
        list.setBackground(Color.WHITE);
        list.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        list.setFont(f(Font.PLAIN, BASE));

        JScrollPane sc = new JScrollPane(list);
        sc.setBorder(BorderFactory.createLineBorder(BORDER));
        sc.getViewport().setBackground(Color.WHITE);
        sc.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sc.getVerticalScrollBar().setUI(new ModernScrollBarUI());

        topCard.add(sc);
        sidebar.add(topCard);
        sidebar.add(Box.createVerticalStrut(14));

        JPanel actions = createCardPanel("Quick actions", "💡");
        JButton refreshBtn = createActionButton("Refresh stats", "🔄", ACCENT, false);
        refreshBtn.addActionListener(e -> refreshStats());
        actions.add(refreshBtn);

        sidebar.add(actions);
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel createMain() {
        JPanel main = new JPanel(new BorderLayout(14, 14));
        main.setBackground(BG);
        main.setBorder(new EmptyBorder(18, 18, 18, 18));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(f(Font.BOLD, BASE));

        tabs.addTab("📊 Stats", buildStatsPage());
        tabs.addTab("👥 Users", new UsersManagementPanel(owner, user, accountDAO, transactionDAO, engine));
        tabs.addTab("💳 Accounts", new AccountsManagementPanel(owner, user, accountDAO, transactionDAO, engine));
        tabs.addTab("🧾 Transactions", new TransactionsManagementPanel(owner, user, accountDAO, transactionDAO, engine));

        main.add(tabs, BorderLayout.CENTER);
        return main;
    }

    // ================= STATS PAGE =================
    private JPanel buildStatsPage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel t = new JLabel("Statistics");
        t.setFont(f(Font.BOLD, 18));
        t.setForeground(TEXT);

        JLabel sub = new JLabel("Admin insights using Streams (aggregation + ranking)");
        sub.setFont(f(Font.PLAIN, BASE));
        sub.setForeground(MUTED);

        header.add(t);
        header.add(Box.createVerticalStrut(4));
        header.add(sub);

        page.add(header, BorderLayout.NORTH);

        // ✅ only TWO charts now (Pie + Status columns)
        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 12));
        charts.setOpaque(false);

        pieByType = new PieChartPanel("Transactions by Type (Pie)");
        barByStatus = new BarChartPanel("Transactions by Status (Columns)");

        charts.add(pieByType);
        charts.add(barByStatus);

        page.add(charts, BorderLayout.CENTER);

        return page;
    }

    // ================= REFRESH (STREAMS) =================
    private void refreshStats() {
        long usersCount = new UserDAO().countUsers();
        List<Account> accounts = accountDAO.findAll();
        List<String> history = transactionDAO.findAllHistoryLines();

        long accountsCount = accounts.size();
        double balanceSum = accounts.stream().mapToDouble(Account::getBalance).sum();
        long txCount = history.size();

        totalUsers.setText(kpiHtml("Total Users", String.valueOf(usersCount)));
        totalAccounts.setText(kpiHtml("Total Accounts", String.valueOf(accountsCount)));
        totalBalance.setText(kpiHtml("Total Balance", String.format("%.2f DH", balanceSum)));
        totalTx.setText(kpiHtml("Total Transactions", String.valueOf(txCount)));

        Map<String, Long> byType = history.stream()
                .map(this::extractType)
                .filter(s -> !s.isBlank())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        Map<String, Long> byStatus = history.stream()
                .map(this::extractStatus)
                .filter(s -> !s.isBlank())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        pieByType.setData(byType);
        barByStatus.setData(byStatus);

        pieByType.repaint();
        barByStatus.repaint();

        // ===== Top Clients (group by USER_ID) =====
        Map<Integer, String> userNames = new UserDAO().findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        Map<Integer, Double> totalByUser = accounts.stream()
                .collect(Collectors.groupingBy(Account::getId, Collectors.summingDouble(Account::getBalance)));

        Map<Integer, Long> countByUser = accounts.stream()
                .collect(Collectors.groupingBy(Account::getId, Collectors.counting()));

        topClientsModel.clear();
        totalByUser.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(8)
                .forEach(e -> {
                    int userId = e.getKey();
                    String name = userNames.getOrDefault(userId, "User#" + userId);
                    double sum = e.getValue();
                    long nb = countByUser.getOrDefault(userId, 0L);
                    topClientsModel.addElement("• " + name + "  |  " + String.format("%.2f DH", sum) + "  |  " + nb + " accounts");
                });
    }

    private String extractType(String line) {
        try {
            String[] p = line.split("\\s\\|\\s");
            if (p.length >= 2) return p[1].trim().toUpperCase();
        } catch (Exception ignored) {}
        return "";
    }

    private String extractStatus(String line) {
        try {
            String[] p = line.split("\\s\\|\\s");
            if (p.length >= 5) return p[4].trim().toUpperCase();
        } catch (Exception ignored) {}
        return "";
    }

    // ================= UI HELPERS =================
    private JPanel createCardPanel(String title, String icon) {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(14, 14, 14, 14)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel head = new JLabel(icon + "  " + title);
        head.setFont(f(Font.BOLD, H2));
        head.setForeground(TEXT);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(head);
        card.add(Box.createVerticalStrut(12));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JButton createActionButton(String text, String icon, Color color, boolean filled) {
        JButton b = new JButton();
        b.setLayout(new BorderLayout(10, 0));
        b.setFont(f(Font.BOLD, BASE));
        b.setBackground(filled ? color : Color.WHITE);
        b.setForeground(filled ? Color.WHITE : TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, filled ? 0 : 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(160, 44));

        JLabel ic = new JLabel(icon);
        ic.setFont(e(Font.PLAIN, 14));
        ic.setForeground(filled ? Color.WHITE : color);

        JLabel tx = new JLabel(text);
        tx.setFont(f(Font.BOLD, BASE));
        tx.setForeground(filled ? Color.WHITE : TEXT);

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

    private String kpiHtml(String title, String value) {
        return "<html><div style='font-size:12px;color:#6c757d;'>" + title +
                "</div><div style='font-size:20px;font-weight:700;color:#212529;'>" + value + "</div></html>";
    }

    // ================= CHARTS =================
    private static class BarChartPanel extends JPanel {
        private final String title;
        private Map<String, Long> data = new HashMap<>();

        BarChartPanel(String title) {
            this.title = title;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(BORDER));
        }

        void setData(Map<String, Long> data) {
            this.data = (data == null) ? new HashMap<>() : data;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(TEXT);
            g2.drawString(title, 14, 24);

            if (data.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(MUTED);
                g2.drawString("No data yet", 14, 50);
                return;
            }

            List<Map.Entry<String, Long>> list = data.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .toList();

            long max = list.stream().mapToLong(Map.Entry::getValue).max().orElse(1);

            int chartTop = 50;
            int chartBottom = h - 25;
            int chartHeight = chartBottom - chartTop;

            int barW = Math.max(80, (w - 40) / list.size());
            int x = 20;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            for (Map.Entry<String, Long> e : list) {
                int barH = (int) ((e.getValue() * 1.0 / max) * (chartHeight - 30));
                int y = chartBottom - barH;

                g2.setColor(ACCENT);
                g2.fillRoundRect(x, y, barW - 14, barH, 10, 10);

                g2.setColor(TEXT);
                g2.drawString(e.getKey(), x, chartBottom + 16);

                g2.setColor(MUTED);
                g2.drawString(String.valueOf(e.getValue()), x, y - 6);

                x += barW;
            }
        }
    }

    private static class PieChartPanel extends JPanel {
        private final String title;
        private Map<String, Long> data = new HashMap<>();

        private final Color[] palette = new Color[] {
                new Color(0,122,255),
                new Color(88,86,214),
                new Color(52,199,89),
                new Color(255,149,0),
                new Color(255,59,48),
                new Color(90,200,250)
        };

        PieChartPanel(String title) {
            this.title = title;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(BORDER));
        }

        void setData(Map<String, Long> data) {
            this.data = (data == null) ? new HashMap<>() : data;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(TEXT);
            g2.drawString(title, 14, 24);

            if (data.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(MUTED);
                g2.drawString("No data yet", 14, 50);
                return;
            }

            long total = data.values().stream().mapToLong(v -> v).sum();
            if (total <= 0) return;

            List<Map.Entry<String, Long>> list = data.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .toList();

            int size = Math.min(w - 180, h - 80);
            if (size < 140) size = Math.min(w - 40, h - 80);

            int px = 20;
            int py = 50;

            int startAngle = 0;
            for (int i = 0; i < list.size(); i++) {
                Map.Entry<String, Long> e = list.get(i);
                int angle = (int) Math.round((e.getValue() * 360.0) / total);

                g2.setColor(palette[i % palette.length]);
                g2.fillArc(px, py, size, size, startAngle, angle);

                startAngle += angle;
            }

            // legend with percentage
            int lx = px + size + 14;
            int ly = py + 18;

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            for (int i = 0; i < list.size(); i++) {
                Map.Entry<String, Long> e = list.get(i);
                double pct = (e.getValue() * 100.0) / total;

                g2.setColor(palette[i % palette.length]);
                g2.fillRoundRect(lx, ly - 10, 12, 12, 4, 4);

                g2.setColor(TEXT);
                g2.drawString(e.getKey() + "  •  " + String.format("%.1f", pct) + "%", lx + 18, ly);

                ly += 20;
            }
        }
    }

    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(200, 205, 210);
            this.trackColor = new Color(245, 247, 250);
        }
        @Override protected JButton createDecreaseButton(int orientation) { return invisible(); }
        @Override protected JButton createIncreaseButton(int orientation) { return invisible(); }

        private JButton invisible() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }
    }
}