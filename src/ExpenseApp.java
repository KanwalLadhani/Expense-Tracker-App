import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class ExpenseApp extends JFrame {

    // ── Colors & Fonts ────────────────────────────────────────────────
    static final Color BG       = new Color(18,  18,  27);
    static final Color PANEL_BG = new Color(28,  28,  40);
    static final Color CARD_BG  = new Color(38,  38,  55);
    static final Color ACCENT   = new Color(99,  102, 241);
    static final Color SUCCESS  = new Color(34,  197, 94);
    static final Color DANGER   = new Color(239, 68,  68);
    static final Color WARNING  = new Color(251, 191, 36);
    static final Color TEXT     = new Color(226, 226, 240);
    static final Color TEXT_DIM = new Color(140, 140, 170);
    static final Color BORDER   = new Color(55,  55,  78);

    static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  22);
    static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD,  14);
    static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);

    // ── State ─────────────────────────────────────────────────────────
    private DatabaseManager  db = new DatabaseManager();
    private DefaultTableModel tableModel;
    private CardLayout        cardLayout;
    private JPanel            contentPanel;
    private ChartPanel        chartPanel;

    // ── Stat labels ───────────────────────────────────────────────────
    private JLabel statIncome, statExpense, statBalance;

    // ── Income categories ─────────────────────────────────────────────
    private static final String[] INCOME_CATS  = {"Salary", "Freelance", "Business", "Investment", "Gift", "Other"};
    private static final String[] EXPENSE_CATS = {"Food", "Transport", "Shopping", "Bills", "Health", "Education", "Entertainment", "Other"};

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ExpenseApp().setVisible(true));
    }

    public ExpenseApp() {
        db.connect();
        buildFrame();
    }

    // ── Frame ─────────────────────────────────────────────────────────
    private void buildFrame() {
        setTitle("💰 Expense Tracker");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { db.disconnect(); System.exit(0); }
        });
        add(buildSidebar(), BorderLayout.WEST);
        add(buildContent(), BorderLayout.CENTER);
    }

    // ── Sidebar ───────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(220, 0));
        side.setBackground(PANEL_BG);
        side.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));

        JPanel logo = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        logo.setBackground(PANEL_BG);
        JLabel t = new JLabel("💰 Expense Tracker");
        t.setFont(FONT_HEADING); t.setForeground(TEXT);
        logo.add(t);

        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(PANEL_BG);
        nav.setBorder(new EmptyBorder(10, 0, 0, 0));

        String[] pages = {"📊  Dashboard", "➕  Add Transaction", "📋  Transactions", "📈  Chart"};
        for (String page : pages) {
            JButton btn = sidebarButton(page);
            String name = page.substring(3).trim();
            btn.addActionListener(e -> switchPage(name));
            nav.add(btn);
            nav.add(Box.createVerticalStrut(4));
        }

        side.add(logo, BorderLayout.NORTH);
        side.add(nav,  BorderLayout.CENTER);
        return side;
    }

    private JButton sidebarButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(FONT_BODY); btn.setForeground(TEXT_DIM);
        btn.setBackground(PANEL_BG); btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(CARD_BG); btn.setForeground(TEXT); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(PANEL_BG); btn.setForeground(TEXT_DIM); }
        });
        return btn;
    }

    // ── Content ───────────────────────────────────────────────────────
    private JPanel buildContent() {
        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG);
        contentPanel.add(buildDashboard(),    "Dashboard");
        contentPanel.add(buildAddForm(),      "Add Transaction");
        contentPanel.add(buildTransactions(), "Transactions");
        contentPanel.add(buildChart(),        "Chart");
        return contentPanel;
    }

    private void switchPage(String name) {
        if (name.equals("Transactions")) refreshTable();
        if (name.equals("Dashboard"))    refreshStats();
        if (name.equals("Chart"))        { chartPanel.refresh(); chartPanel.repaint(); }
        cardLayout.show(contentPanel, name);
    }

    // ── Dashboard ─────────────────────────────────────────────────────
    private JPanel buildDashboard() {
        JPanel p = page();
        p.add(pageTitle("Dashboard"), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 3, 20, 0));
        cards.setBackground(BG);
        cards.setBorder(new EmptyBorder(24, 30, 20, 30));

        statIncome  = new JLabel("0.00");
        statExpense = new JLabel("0.00");
        statBalance = new JLabel("0.00");

        cards.add(statCard("💚  Total Income",  statIncome,  SUCCESS));
        cards.add(statCard("❤  Total Expense", statExpense, DANGER));
        cards.add(statCard("💰  Balance",        statBalance, ACCENT));

        // Recent transactions preview
        JPanel recent = new JPanel(new BorderLayout());
        recent.setBackground(BG);
        recent.setBorder(new EmptyBorder(0, 30, 20, 30));

        JLabel recentTitle = new JLabel("Recent Transactions");
        recentTitle.setFont(FONT_HEADING); recentTitle.setForeground(TEXT);
        recentTitle.setBorder(new EmptyBorder(0, 0, 10, 0));

        String[] cols = {"Date", "Type", "Category", "Description", "Amount"};
        DefaultTableModel recentModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable recentTable = styledTable(recentModel);

        // Load last 5 transactions
        ArrayList<Transaction> all = db.getAllTransactions();
        int limit = Math.min(5, all.size());
        for (int i = 0; i < limit; i++) {
            Transaction t = all.get(i);
            recentModel.addRow(new Object[]{
                    t.getDateStr(), t.getType().name(), t.getCategory(),
                    t.getDescription(),
                    (t.isIncome() ? "+ " : "- ") + String.format("%.2f", t.getAmount())
            });
        }

        JScrollPane scroll = new JScrollPane(recentTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        recent.add(recentTitle, BorderLayout.NORTH);
        recent.add(scroll,      BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG);
        center.add(cards,  BorderLayout.NORTH);
        center.add(recent, BorderLayout.CENTER);

        p.add(center, BorderLayout.CENTER);
        refreshStats();
        return p;
    }

    private JPanel statCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));
        JLabel t = new JLabel(title); t.setFont(FONT_SMALL); t.setForeground(TEXT_DIM);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accent);
        card.add(t, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void refreshStats() {
        double income  = db.getTotal(Transaction.Type.INCOME);
        double expense = db.getTotal(Transaction.Type.EXPENSE);
        double balance = income - expense;
        statIncome.setText(String.format("%.2f", income));
        statExpense.setText(String.format("%.2f", expense));
        statBalance.setText(String.format("%.2f", balance));
        statBalance.setForeground(balance >= 0 ? SUCCESS : DANGER);
    }

    // ── Add Transaction form ──────────────────────────────────────────
    private JPanel buildAddForm() {
        JPanel p = page();
        p.add(pageTitle("Add Transaction"), BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG);
        form.setBorder(new EmptyBorder(30, 80, 30, 80));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(30, 30, 30, 30)
        ));

        // Type selector
        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        typeRow.setBackground(CARD_BG);
        JToggleButton incomeBtn  = toggleButton("💚  Income",  true);
        JToggleButton expenseBtn = toggleButton("❤  Expense", false);
        ButtonGroup   group      = new ButtonGroup();
        group.add(incomeBtn); group.add(expenseBtn);
        typeRow.add(incomeBtn); typeRow.add(Box.createHorizontalStrut(10)); typeRow.add(expenseBtn);

        // Fields
        JComboBox<String> categoryBox = new JComboBox<>(INCOME_CATS);
        styleCombo(categoryBox);

        JTextField descField   = styledField("e.g. Monthly salary", 0);
        JTextField amountField = styledField("e.g. 50000", 0);
        JTextField dateField   = styledField(LocalDate.now().toString(), 0);

        // Switch categories when type changes
        incomeBtn.addActionListener(e -> {
            categoryBox.setModel(new DefaultComboBoxModel<>(INCOME_CATS));
        });
        expenseBtn.addActionListener(e -> {
            categoryBox.setModel(new DefaultComboBoxModel<>(EXPENSE_CATS));
        });

        JButton saveBtn = accentButton("Save Transaction");
        saveBtn.setAlignmentX(LEFT_ALIGNMENT);
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        saveBtn.addActionListener(e -> {
            try {
                String desc   = descField.getText().trim();
                double amount = Double.parseDouble(amountField.getText().trim());
                LocalDate date = LocalDate.parse(dateField.getText().trim());
                String cat    = (String) categoryBox.getSelectedItem();
                Transaction.Type type = incomeBtn.isSelected()
                        ? Transaction.Type.INCOME : Transaction.Type.EXPENSE;

                if (amount <= 0) { toast("Amount must be greater than 0."); return; }

                db.addTransaction(type, cat, desc, amount, date);
                descField.setText("");
                amountField.setText("");
                dateField.setText(LocalDate.now().toString());
                refreshStats();
                toast("Transaction saved successfully!");
            } catch (NumberFormatException ex) {
                toast("Amount must be a valid number.");
            } catch (DateTimeParseException ex) {
                toast("Date must be in format yyyy-MM-dd");
            }
        });

        addRow(card, "Type",        typeRow);
        addRow(card, "Category",    categoryBox);
        addRow(card, "Description", descField);
        addRow(card, "Amount (Rs)", amountField);
        addRow(card, "Date",        dateField);
        card.add(Box.createVerticalStrut(20));
        card.add(saveBtn);

        form.add(card);
        p.add(form, BorderLayout.CENTER);
        return p;
    }

    private JToggleButton toggleButton(String label, boolean selected) {
        JToggleButton btn = new JToggleButton(label, selected);
        btn.setFont(FONT_BODY); btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setBackground(selected ? ACCENT : CARD_BG);
        btn.setForeground(selected ? Color.WHITE : TEXT_DIM);
        btn.addItemListener(e -> {
            btn.setBackground(btn.isSelected() ? ACCENT : CARD_BG);
            btn.setForeground(btn.isSelected() ? Color.WHITE : TEXT_DIM);
        });
        return btn;
    }

    // ── Transactions page ─────────────────────────────────────────────
    private JPanel buildTransactions() {
        JPanel p = page();
        p.add(pageTitle("All Transactions"), BorderLayout.NORTH);

        String[] cols = {"ID", "Date", "Type", "Category", "Description", "Amount"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = styledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setBackground(BG);

        JButton allBtn     = smallButton("All");
        JButton incomeBtn  = smallButton("Income Only");
        JButton expenseBtn = smallButton("Expense Only");
        JButton deleteBtn  = dangerButton("Delete Selected");

        allBtn.addActionListener(e     -> loadTable(db.getAllTransactions()));
        incomeBtn.addActionListener(e  -> loadTable(db.getByType(Transaction.Type.INCOME)));
        expenseBtn.addActionListener(e -> loadTable(db.getByType(Transaction.Type.EXPENSE)));

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast("Select a transaction first."); return; }
            int id = (int) tableModel.getValueAt(row, 0);
            db.deleteTransaction(id);
            refreshTable();
            refreshStats();
            if (chartPanel != null) { chartPanel.refresh(); chartPanel.repaint(); }
        });

        toolbar.add(allBtn); toolbar.add(incomeBtn); toolbar.add(expenseBtn);
        toolbar.add(Box.createHorizontalStrut(20)); toolbar.add(deleteBtn);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(styledScroll(table), BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        refreshTable();
        return p;
    }

    private void refreshTable() {
        loadTable(db.getAllTransactions());
    }

    private void loadTable(ArrayList<Transaction> list) {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        for (Transaction t : list) {
            tableModel.addRow(new Object[]{
                    t.getId(), t.getDateStr(), t.getType().name(),
                    t.getCategory(), t.getDescription(),
                    (t.isIncome() ? "+ " : "- ") + String.format("%.2f", t.getAmount())
            });
        }
    }

    // ── Chart page ────────────────────────────────────────────────────
    private JPanel buildChart() {
        JPanel p = page();
        p.add(pageTitle("Spending Chart"), BorderLayout.NORTH);

        chartPanel = new ChartPanel(db);

        JPanel typeBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        typeBar.setBackground(BG);
        typeBar.setBorder(new EmptyBorder(0, 30, 0, 0));
        JButton showIncome  = smallButton("Income by Category");
        JButton showExpense = smallButton("Expenses by Category");
        showIncome.addActionListener(e  -> { chartPanel.setType(Transaction.Type.INCOME);  chartPanel.refresh(); chartPanel.repaint(); });
        showExpense.addActionListener(e -> { chartPanel.setType(Transaction.Type.EXPENSE); chartPanel.refresh(); chartPanel.repaint(); });
        typeBar.add(showIncome); typeBar.add(showExpense);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(0, 30, 20, 30));
        wrapper.add(chartPanel, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG);
        center.add(typeBar,  BorderLayout.NORTH);
        center.add(wrapper,  BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    // ── Chart panel (Java2D) ──────────────────────────────────────────
    static class ChartPanel extends JPanel {
        private DatabaseManager      db;
        private Transaction.Type     type = Transaction.Type.EXPENSE;
        private ArrayList<String[]>  data = new ArrayList<>();

        static final Color BG       = new Color(28, 28, 40);
        static final Color BORDER   = new Color(55, 55, 78);
        static final Color TEXT     = new Color(226, 226, 240);
        static final Color TEXT_DIM = new Color(140, 140, 170);
        static final Color SUCCESS  = new Color(34,  197, 94);
        static final Color DANGER   = new Color(239, 68,  68);
        static final Color WARNING  = new Color(251, 191, 36);
        static final Color ACCENT   = new Color(99,  102, 241);

        static final Color[] BAR_COLORS = {
                new Color(99,102,241), new Color(34,197,94), new Color(239,68,68),
                new Color(251,191,36), new Color(20,184,166), new Color(249,115,22),
                new Color(168,85,247), new Color(236,72,153)
        };

        public ChartPanel(DatabaseManager db) {
            this.db = db;
            setBackground(BG);
            setBorder(new LineBorder(BORDER, 1, true));
            refresh();
        }

        public void setType(Transaction.Type type) { this.type = type; }
        public void refresh() { data = db.getCategoryTotals(type); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 80, padR = 30, padT = 50, padB = 70;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            // Title
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(TEXT);
            String title = (type == Transaction.Type.EXPENSE ? "Expenses" : "Income") + " by Category";
            g2.drawString(title, padL, padT - 15);

            if (data == null || data.isEmpty()) {
                g2.setColor(TEXT_DIM);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                g2.drawString("No data yet — add some transactions first.", padL + 20, padT + chartH / 2);
                return;
            }

            // Find max value for scaling
            double max = data.stream().mapToDouble(d -> Double.parseDouble(d[1])).max().orElse(1);

            // Y-axis grid lines
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            for (int i = 0; i <= 5; i++) {
                int y = padT + chartH - (i * chartH / 5);
                g2.setColor(BORDER);
                g2.drawLine(padL, y, padL + chartW, y);
                g2.setColor(TEXT_DIM);
                String label = String.format("%.0f", max * i / 5);
                g2.drawString(label, padL - 70, y + 4);
            }

            int barW    = Math.min(70, (chartW / data.size()) - 12);
            int spacing = chartW / data.size();

            for (int i = 0; i < data.size(); i++) {
                String cat   = data.get(i)[0];
                double total = Double.parseDouble(data.get(i)[1]);
                int barH = (int) (total / max * chartH);
                int x    = padL + i * spacing + (spacing - barW) / 2;
                int y    = padT + chartH - barH;

                Color color = BAR_COLORS[i % BAR_COLORS.length];

                // Filled bar
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
                g2.fill(new RoundRectangle2D.Float(x, y, barW, barH, 8, 8));
                g2.setColor(color);
                g2.draw(new RoundRectangle2D.Float(x, y, barW, barH, 8, 8));

                // Amount label above bar
                FontMetrics fm = g2.getFontMetrics();
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.setColor(TEXT);
                String amtStr = String.format("%.0f", total);
                g2.drawString(amtStr, x + (barW - fm.stringWidth(amtStr)) / 2, y - 5);

                // Category label below
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(TEXT_DIM);
                String shortCat = cat.length() > 8 ? cat.substring(0, 7) + "." : cat;
                g2.drawString(shortCat, x + (barW - fm.stringWidth(shortCat)) / 2, padT + chartH + 18);
            }
        }
    }

    // ── Layout helpers ────────────────────────────────────────────────
    private void addRow(JPanel parent, String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setBackground(CARD_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(new Dimension(110, 30));
        lbl.setFont(FONT_SMALL); lbl.setForeground(TEXT_DIM);
        row.add(lbl,   BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        parent.add(row);
        parent.add(Box.createVerticalStrut(12));
    }

    private JPanel page() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG); return p;
    }

    private JPanel pageTitle(String text) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 20));
        bar.setBackground(BG);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_TITLE); lbl.setForeground(TEXT);
        bar.add(lbl); return bar;
    }

    private JTextField styledField(String placeholder, int cols) {
        JTextField f = cols > 0 ? new JTextField(cols) : new JTextField();
        f.setFont(FONT_BODY); f.setForeground(TEXT_DIM);
        f.setBackground(new Color(50, 50, 70)); f.setCaretColor(TEXT);
        f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(6, 10, 6, 10)));
        f.setText(placeholder);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(TEXT); } }
            public void focusLost(FocusEvent e)   { if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(TEXT_DIM); } }
        });
        return f;
    }

    private void styleCombo(JComboBox<String> box) {
        box.setFont(FONT_BODY); box.setBackground(new Color(50, 50, 70));
        box.setForeground(TEXT); box.setBorder(new LineBorder(BORDER, 1));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    private JButton accentButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(FONT_BODY); btn.setForeground(Color.WHITE);
        btn.setBackground(ACCENT); btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        return btn;
    }

    private JButton dangerButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(FONT_BODY); btn.setForeground(Color.WHITE);
        btn.setBackground(DANGER); btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    private JButton smallButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(FONT_SMALL); btn.setForeground(TEXT);
        btn.setBackground(new Color(50, 50, 70)); btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        return btn;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(FONT_BODY); table.setForeground(TEXT);
        table.setBackground(CARD_BG); table.setRowHeight(36);
        table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ACCENT); table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_SMALL); header.setForeground(TEXT_DIM);
        header.setBackground(PANEL_BG); header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBackground(sel ? ACCENT : (r % 2 == 0 ? CARD_BG : new Color(33, 33, 48)));
                setBorder(new EmptyBorder(0, 12, 0, 12));
                if (!sel && v != null) {
                    String val = v.toString();
                    if      (val.startsWith("+")) setForeground(SUCCESS);
                    else if (val.startsWith("-")) setForeground(DANGER);
                    else if (val.equals("INCOME"))  setForeground(SUCCESS);
                    else if (val.equals("EXPENSE")) setForeground(DANGER);
                    else setForeground(TEXT);
                } else if (sel) setForeground(Color.WHITE);
                return this;
            }
        };
        for (int i = 0; i < model.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        return table;
    }

    private JScrollPane styledScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(10, 30, 20, 30));
        scroll.getViewport().setBackground(CARD_BG);
        return scroll;
    }

    private void toast(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Notice", JOptionPane.INFORMATION_MESSAGE);
    }
}