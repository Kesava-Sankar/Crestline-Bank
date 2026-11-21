package com.crestlinebank.ui;

import com.crestlinebank.dao.FamilyPayDao;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Independent child experience. Payments debit the linked parent's account. */
public class ChildDashboardFrame extends JFrame {
    private final FamilyPayDao dao = new FamilyPayDao();
    private final FamilyPayDao.ChildSession child;
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final JLabel spentValue = metricValue();
    private final JLabel remainingValue = metricValue();
    private final JLabel limitValue = metricValue();
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel transactionModel = new DefaultTableModel(
            new Object[]{"Recipient / Merchant", "Amount", "Status", "Purpose", "Date & time"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable transactionTable = new JTable(transactionModel);

    public ChildDashboardFrame(FamilyPayDao.ChildSession child) {
        this.child = child;
        setTitle("Crestline Pay | Child Banking");
        setSize(1080, 720);
        setMinimumSize(new Dimension(850, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        build();
        refreshDashboard();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(new EmptyBorder(0, 0, 18, 0));

        JPanel hero = new JPanel(new BorderLayout(18, 10));
        hero.setBackground(Theme.NAVY);
        hero.setBorder(new EmptyBorder(24, 30, 24, 30));
        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("CRESTLINE PAY  /  FAMILY BANKING");
        brand.setForeground(Theme.GOLD);
        brand.setFont(Theme.font(Font.BOLD, 12));
        JLabel greeting = new JLabel("Welcome, " + child.childName());
        greeting.setForeground(Color.WHITE);
        greeting.setFont(Theme.font(Font.BOLD, 27));
        JLabel upi = new JLabel(child.upi());
        upi.setForeground(new Color(211, 220, 232));
        upi.setFont(Theme.font(Font.PLAIN, 14));
        identity.add(brand);
        identity.add(Box.createVerticalStrut(9));
        identity.add(greeting);
        identity.add(Box.createVerticalStrut(4));
        identity.add(upi);

        JButton send = Ui.button("＋  Send money", Theme.GREEN);
        JButton notifications = Ui.button(NotificationDialog.buttonLabel("CHILD", child.profileId()), Theme.NAVY_LIGHT);
        notifications.addActionListener(e -> NotificationDialog.show(this, "CHILD", child.profileId()));
        JButton signOut = Ui.button("Sign out", Theme.NAVY_LIGHT);
        signOut.addActionListener(e -> dispose());
        send.addActionListener(e -> makePayment());
        JPanel heroActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        heroActions.setOpaque(false);
        heroActions.add(send);
        heroActions.add(notifications);
        heroActions.add(signOut);
        hero.add(identity, BorderLayout.CENTER);
        hero.add(heroActions, BorderLayout.EAST);
        root.add(hero, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 28, 0, 28));
        JPanel intro = new JPanel(new BorderLayout());
        intro.setOpaque(false);
        JLabel overview = new JLabel("Your spending overview");
        overview.setFont(Theme.font(Font.BOLD, 21));
        overview.setForeground(Theme.TEXT);
        JLabel funded = new JLabel("PARENT-FUNDED  •  INDEPENDENT CHILD ACCESS");
        funded.setFont(Theme.font(Font.BOLD, 10));
        funded.setForeground(Theme.MUTED);
        intro.add(overview, BorderLayout.WEST);
        intro.add(funded, BorderLayout.EAST);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0));
        metrics.setOpaque(false);
        metrics.add(metricCard("DAILY SPENDING LIMIT", limitValue, "Set by your parent"));
        metrics.add(metricCard("SPENT TODAY", spentValue, "Successful payments today"));
        metrics.add(metricCard("AVAILABLE TODAY", remainingValue, "Available within your limit"));

        JPanel topContent = new JPanel(new BorderLayout(0, 14));
        topContent.setOpaque(false);
        topContent.add(intro, BorderLayout.NORTH);
        topContent.add(metrics, BorderLayout.CENTER);

        JPanel activity = Ui.card();
        activity.setLayout(new BorderLayout(0, 12));
        JPanel activityHeading = new JPanel(new BorderLayout());
        activityHeading.setOpaque(false);
        JLabel activityTitle = new JLabel("Recent payments");
        activityTitle.setFont(Theme.font(Font.BOLD, 17));
        activityTitle.setForeground(Theme.TEXT);
        JButton refresh = Ui.button("↻  Refresh", Theme.GOLD);
        refresh.setForeground(Theme.NAVY);
        refresh.addActionListener(e -> refreshDashboard());
        activityHeading.add(activityTitle, BorderLayout.WEST);
        activityHeading.add(refresh, BorderLayout.EAST);

        transactionTable.setRowHeight(34);
        transactionTable.setFont(Theme.font(Font.PLAIN, 12));
        transactionTable.setForeground(Theme.TEXT);
        transactionTable.setGridColor(Theme.BORDER);
        transactionTable.setSelectionBackground(new Color(228, 237, 247));
        transactionTable.setFillsViewportHeight(true);
        transactionTable.getTableHeader().setFont(Theme.font(Font.BOLD, 12));
        transactionTable.getTableHeader().setBackground(new Color(242, 245, 249));
        transactionTable.setAutoCreateRowSorter(true);
        activity.add(activityHeading, BorderLayout.NORTH);
        activity.add(new JScrollPane(transactionTable), BorderLayout.CENTER);

        status.setFont(Theme.font(Font.PLAIN, 11));
        status.setForeground(Theme.MUTED);
        JPanel lower = new JPanel(new BorderLayout(0, 8));
        lower.setOpaque(false);
        lower.add(activity, BorderLayout.CENTER);
        lower.add(status, BorderLayout.SOUTH);
        content.add(topContent, BorderLayout.NORTH);
        content.add(lower, BorderLayout.CENTER);

        JLabel footer = new JLabel("Payments use the linked parent's bank account. Every child payment is also visible in the parent's transaction history.");
        footer.setFont(Theme.font(Font.PLAIN, 11));
        footer.setForeground(Theme.MUTED);
        footer.setBorder(new EmptyBorder(0, 28, 0, 28));
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(content, BorderLayout.CENTER);
        center.add(footer, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel metricCard(String title, JLabel value, String hint) {
        JPanel card = Ui.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel heading = Ui.label(title);
        JLabel caption = new JLabel(hint);
        caption.setFont(Theme.font(Font.PLAIN, 11));
        caption.setForeground(Theme.MUTED);
        card.add(heading);
        card.add(Box.createVerticalStrut(12));
        card.add(value);
        card.add(Box.createVerticalStrut(8));
        card.add(caption);
        return card;
    }

    private static JLabel metricValue() {
        JLabel label = new JLabel("₹0.00");
        label.setFont(Theme.font(Font.BOLD, 25));
        label.setForeground(Theme.NAVY);
        return label;
    }

    private void refreshDashboard() {
        try {
            BigDecimal spent = dao.childSpentToday(child.profileId());
            BigDecimal limit = child.dailyLimit();
            BigDecimal remaining = limit.subtract(spent).max(BigDecimal.ZERO);
            limitValue.setText(money.format(limit));
            spentValue.setText(money.format(spent));
            remainingValue.setText(money.format(remaining));
            transactionModel.setRowCount(0);
            List<Object[]> rows = dao.childTransactions(child.profileId());
            for (Object[] row : rows) {
                transactionModel.addRow(new Object[]{row[0], money.format((BigDecimal) row[1]),
                        row[2], row[3], row[4]});
            }
            status.setText("Showing your latest " + rows.size()
                    + " payments. Your parent can also view these transactions.");
        } catch (Exception ex) {
            status.setText("Could not refresh dashboard: " + ex.getMessage());
        }
    }

    private void makePayment() {
        JTextField recipient = Ui.field(22);
        JTextField amount = Ui.field(22);
        JTextField note = Ui.field(22);
        note.setText("Child UPI payment");
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(Ui.label("RECIPIENT UPI ID / MERCHANT"));
        form.add(recipient);
        form.add(Ui.label("AMOUNT (INR)"));
        form.add(amount);
        form.add(Ui.label("PAYMENT NOTE"));
        form.add(note);
        if (JOptionPane.showConfirmDialog(this, form, "Send money",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            dao.childPayment(child, recipient.getText().trim(),
                    new BigDecimal(amount.getText().trim()), note.getText().trim());
            JOptionPane.showMessageDialog(this,
                    "Payment successful. The amount was debited from the linked parent account.\n"
                            + "Your parent can see this payment in Family UPI history.",
                    "Payment completed", JOptionPane.INFORMATION_MESSAGE);
            refreshDashboard();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Payment could not be completed", JOptionPane.WARNING_MESSAGE);
            refreshDashboard();
        }
    }
}
