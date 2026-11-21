package com.crestlinebank.ui;

import com.crestlinebank.dao.AdminManagementDao;
import com.crestlinebank.dao.SupportDao;
import com.crestlinebank.dao.AnalyticsDao;
import com.crestlinebank.dao.StaffDao;
import com.crestlinebank.model.Admin;
import com.crestlinebank.model.Loan;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class AdminFrame extends JFrame {
    private final Admin admin;
    private final AdminManagementDao dao = new AdminManagementDao();
    private final SupportDao supportDao = new SupportDao();
    private final AnalyticsDao analyticsDao = new AnalyticsDao();
    private final StaffDao staffDao = new StaffDao();
    private final DefaultTableModel staffModel = model("Staff ID","Name","Email","Role","Monthly salary","Loan approval limit","Status","Joined");
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final DefaultTableModel customers = model("ID", "Customer", "Email", "Account", "Balance", "Status", "Created");
    private final DefaultTableModel transactions = model("Txn ID", "Customer", "Account", "Type", "Amount", "Description", "Risk", "Date");
    private final DefaultTableModel repayments = model("Repayment ID", "Loan ID", "Customer", "Amount", "Paid at");
    private final DefaultTableModel loans = model("Loan ID", "Customer ID", "Customer", "Account", "Principal", "Outstanding", "Rate %", "Months", "Status", "Created");
    private final DefaultTableModel support = model("Request ID", "Customer", "Email", "Subject", "Message", "Admin reply", "Status", "Created");
    private final DefaultTableModel monthly = model("Month", "Deposits", "Withdrawals", "Transfers", "Transactions");
    private final DefaultTableModel interestDetails = model("Loan ID","Customer","Principal","Rate %","Total interest","Interest paid","Interest left","Next due","Status");
    private final JLabel analyticsSummary = new JLabel("Loading analytics…");
    private final JLabel summary = new JLabel("Loading overview…");
    private final JPanel pageCards = new JPanel(new CardLayout());
    private final JLabel pageTitle = new JLabel("Bank operations overview");

    public AdminFrame(Admin admin) {
        this.admin = admin;
        setTitle("Crestline Bank | Administration");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 850);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);
        build();
        refreshAll();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(Theme.BACKGROUND);
        JPanel header = new JPanel(new BorderLayout()); header.setBackground(Theme.NAVY);
        header.setBorder(new EmptyBorder(18, 28, 18, 28));
        JLabel brand = new JLabel("CRESTLINE  /  ADMINISTRATION");
        brand.setForeground(Theme.GOLD); brand.setFont(Theme.font(Font.BOLD, 20));
        JButton notifications = Ui.button(NotificationDialog.buttonLabel("ADMIN", 0), Theme.NAVY_LIGHT);
        notifications.addActionListener(e -> NotificationDialog.show(this, "ADMIN", 0));
        JButton logout = Ui.button("Sign out", Theme.NAVY_LIGHT);
        logout.addActionListener(e -> { new LoginFrame().setVisible(true); dispose(); });
        JLabel who = new JLabel("Administrator: " + admin.fullName() + "   "); who.setForeground(Color.WHITE);
        who.setFont(Theme.font(Font.PLAIN, 13));
        JPanel hr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)); hr.setOpaque(false); hr.add(who); hr.add(notifications); hr.add(logout);
        header.add(brand, BorderLayout.WEST); header.add(hr, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 16)); body.setOpaque(false);
        body.setBorder(new EmptyBorder(22, 26, 24, 26));
        pageTitle.setFont(Theme.font(Font.BOLD, 26)); pageTitle.setForeground(Theme.TEXT);
        summary.setFont(Theme.font(Font.PLAIN, 14)); summary.setForeground(Theme.MUTED);
        JPanel top = new JPanel(new BorderLayout(0, 8)); top.setOpaque(false); top.add(pageTitle, BorderLayout.NORTH); top.add(summary, BorderLayout.CENTER);
        pageCards.setOpaque(false);
        addPage("overview", analyticsPanel());
        addPage("customers", customerPanel());
        addPage("transactions", tablePanel(transactions, "Recent customer transactions and review flags"));
        addPage("loans", loanPanel());
        addPage("repayments", tablePanel(repayments, "Recorded loan repayments"));
        addPage("analytics", analyticsPanel());
        addPage("requests", supportPanel());
        addPage("staff", staffPanel());
        addPage("settings", settingsPanel());
        JPanel sidebar = new JPanel(); sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.NAVY); sidebar.setBorder(new EmptyBorder(18, 10, 18, 10));
        JLabel navBrand = new JLabel("  CRESTLINE"); navBrand.setForeground(Theme.GOLD); navBrand.setFont(Theme.font(Font.BOLD, 16));
        navBrand.setAlignmentX(Component.LEFT_ALIGNMENT); sidebar.add(navBrand); sidebar.add(Box.createVerticalStrut(22));
        addNav(sidebar, "⌂   Overview", "overview"); addNav(sidebar, "♙   Customers & Accounts", "customers");
        addNav(sidebar, "⇄   Transactions", "transactions"); addNav(sidebar, "▤   Loan Applications", "loans");
        addNav(sidebar, "◷   Repayment History", "repayments"); addNav(sidebar, "◉   Analytics & Reports", "analytics");
        addNav(sidebar, "✉   Customer Requests", "requests"); addNav(sidebar, "♟   Staff & Payroll", "staff"); addNav(sidebar, "⚙   Settings", "settings");
        sidebar.add(Box.createVerticalGlue());
        JButton refresh = Ui.button("↻  Refresh all data", Theme.GOLD); refresh.setForeground(Theme.NAVY);
        refresh.setMaximumSize(new Dimension(220, 42)); refresh.addActionListener(e -> refreshAll()); sidebar.add(refresh);
        JPanel workspace = new JPanel(new BorderLayout(0, 16)); workspace.setOpaque(false);
        workspace.add(top, BorderLayout.NORTH); workspace.add(pageCards, BorderLayout.CENTER);
        body.setBorder(new EmptyBorder(22, 24, 22, 24)); body.add(workspace, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH); root.add(sidebar, BorderLayout.WEST); root.add(body, BorderLayout.CENTER); setContentPane(root);
    }


    private void addPage(String key, JPanel panel) { pageCards.add(panel, key); }
    private void addNav(JPanel sidebar, String label, String key) {
        JButton b = new JButton(label); b.setHorizontalAlignment(SwingConstants.LEFT); b.setForeground(Color.WHITE);
        b.setBackground(Theme.NAVY); b.setBorder(new EmptyBorder(12, 12, 12, 8)); b.setFocusPainted(false);
        b.setFont(Theme.font(Font.BOLD, 12)); b.setMaximumSize(new Dimension(235, 46)); b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> { ((CardLayout) pageCards.getLayout()).show(pageCards, key); pageTitle.setText(label.trim()); });
        sidebar.add(b);
    }

    private JPanel staffPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12)); p.setBackground(Theme.BACKGROUND);
        JTable table = tableFor(staffModel); p.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bar = actionBar(new JLabel("Hire employees, set salaries and define each employee’s loan approval authority."));
        JButton hire = actionButton("＋  Hire staff", Theme.NAVY_LIGHT);
        JButton edit = actionButton("✎  Edit role / salary / limit", Theme.GOLD); edit.setForeground(Theme.NAVY);
        JButton toggle = actionButton("Activate / deactivate", Theme.RED);
        JButton payroll = actionButton("₹  Record salary paid", Theme.GREEN);
        JButton task = actionButton("＋  Assign task", Theme.NAVY_LIGHT);
        hire.addActionListener(e -> hireStaff()); edit.addActionListener(e -> editStaff());
        toggle.addActionListener(e -> toggleStaff()); payroll.addActionListener(e -> recordPayroll()); task.addActionListener(e -> assignStaffTask());
        bar.add(hire); bar.add(edit); bar.add(toggle); bar.add(payroll); bar.add(task); p.add(bar, BorderLayout.SOUTH); return p;
    }
    private void loadStaff() { try { staffModel.setRowCount(0); for(Object[] r:staffDao.all()) staffModel.addRow(r); } catch(Exception e) { error(e); } }
    private int selectedStaffRow() { JTable t=findTable(staffModel); int row=t.getSelectedRow(); if(row<0)throw new IllegalArgumentException("Select a staff member first."); return t.convertRowIndexToModel(row); }
    private void hireStaff() {
        JTextField name=new JTextField(), email=new JTextField(); JPasswordField pass=new JPasswordField(); JComboBox<String> role=new JComboBox<>(new String[]{"CASHIER","LOAN_OFFICER","CUSTOMER_SUPPORT","ACCOUNT_OFFICER","BRANCH_MANAGER","AUDITOR"});
        JTextField salary=new JTextField("25000"), limit=new JTextField("5000"); JPanel f=new JPanel(new GridLayout(0,2,8,8));
        f.add(new JLabel("Full name"));f.add(name);f.add(new JLabel("Email"));f.add(email);f.add(new JLabel("Temporary password"));f.add(pass);f.add(new JLabel("Role"));f.add(role);f.add(new JLabel("Monthly salary (₹)"));f.add(salary);f.add(new JLabel("Loan approval limit (₹)"));f.add(limit);
        if(JOptionPane.showConfirmDialog(this,f,"Hire Crestline staff",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try { staffDao.hire(name.getText(),email.getText(),new String(pass.getPassword()),(String)role.getSelectedItem(),new BigDecimal(salary.getText()),new BigDecimal(limit.getText())); loadStaff(); }
        catch(Exception e){error(e);}
    }
    private void editStaff() {
        try {int r=selectedStaffRow();int id=((Number)staffModel.getValueAt(r,0)).intValue(); JComboBox<String> role=new JComboBox<>(new String[]{"CASHIER","LOAN_OFFICER","CUSTOMER_SUPPORT","ACCOUNT_OFFICER","BRANCH_MANAGER","AUDITOR"});role.setSelectedItem(staffModel.getValueAt(r,3));JTextField salary=new JTextField(String.valueOf(staffModel.getValueAt(r,4))),limit=new JTextField(String.valueOf(staffModel.getValueAt(r,5)));JPanel f=new JPanel(new GridLayout(0,2,8,8));f.add(new JLabel("Role"));f.add(role);f.add(new JLabel("Monthly salary (₹)"));f.add(salary);f.add(new JLabel("Loan approval limit (₹)"));f.add(limit);if(JOptionPane.showConfirmDialog(this,f,"Update staff authority and salary",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;staffDao.update(id,(String)role.getSelectedItem(),new BigDecimal(salary.getText()),new BigDecimal(limit.getText()));loadStaff();}catch(Exception e){error(e);}
    }
    private void toggleStaff() { try {int r=selectedStaffRow();int id=((Number)staffModel.getValueAt(r,0)).intValue();String status=String.valueOf(staffModel.getValueAt(r,6));String next="ACTIVE".equals(status)?"INACTIVE":"ACTIVE";String reason="";if("INACTIVE".equals(next)){reason=JOptionPane.showInputDialog(this,"Reason for staff removal / deactivation (required):");if(reason==null)return;}if(JOptionPane.showConfirmDialog(this,"Set this staff member to "+next+"? Their employment and activity history will be retained.","Confirm staff status",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;staffDao.setStatus(id,next,reason);loadStaff();}catch(Exception e){error(e);} }
    private void assignStaffTask() { try {int r=selectedStaffRow();int id=((Number)staffModel.getValueAt(r,0)).intValue();JTextField title=new JTextField(),details=new JTextField();JPanel f=new JPanel(new GridLayout(0,1,6,6));f.add(new JLabel("Task title"));f.add(title);f.add(new JLabel("Instructions"));f.add(details);if(JOptionPane.showConfirmDialog(this,f,"Assign staff task",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;staffDao.assignTask(id,title.getText(),details.getText());JOptionPane.showMessageDialog(this,"Task assigned to "+staffModel.getValueAt(r,1)+".");}catch(Exception e){error(e);} }
    private void recordPayroll() { try {int r=selectedStaffRow();int id=((Number)staffModel.getValueAt(r,0)).intValue();String month=java.time.YearMonth.now().toString();BigDecimal amount=new BigDecimal(String.valueOf(staffModel.getValueAt(r,4)));try(java.sql.Connection c=com.crestlinebank.config.Database.getConnection();java.sql.PreparedStatement q=c.prepareStatement("INSERT INTO staff_payroll(staff_id,pay_month,amount,status,paid_at) VALUES(?,?,?,'PAID',CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE amount=VALUES(amount),status='PAID',paid_at=CURRENT_TIMESTAMP")){q.setInt(1,id);q.setString(2,month);q.setBigDecimal(3,amount);q.executeUpdate();}JOptionPane.showMessageDialog(this,"Salary payment recorded for "+month+". This records a demo payroll entry; it does not transfer real money.");}catch(Exception e){error(e);} }

    private JPanel settingsPanel() {
        JPanel p=new JPanel(new BorderLayout(0,12)); p.setBackground(Theme.BACKGROUND);
        JLabel heading=new JLabel("Crestline Bank settings"); heading.setFont(Theme.font(Font.BOLD,20)); heading.setForeground(Theme.NAVY);
        JTextArea info=new JTextArea("PROJECT MODE\nEducational Java Swing + MySQL demonstration\n\nDATABASE\nUses crestline_db. Keep your existing database and run upgrade_family_pay_interest_audit.sql and upgrade_staff_family_controls.sql in that order.\n\nPAYMENTS\nCrestline Pay uses simulated UPI identities and demo transactions only. It does not connect to NPCI, UPI, banks or real payment rails.\n\nACCOUNT ACTIONS\nClosed accounts retain their records. Administrative actions require a reason and are written to the audit log.");
        info.setEditable(false); info.setLineWrap(true); info.setWrapStyleWord(true); info.setFont(Theme.font(Font.PLAIN,14)); info.setBackground(Color.WHITE); info.setBorder(new EmptyBorder(18,18,18,18));
        p.add(heading,BorderLayout.NORTH); p.add(new JScrollPane(info),BorderLayout.CENTER); return p;
    }

    private JPanel customerPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12)); p.setBackground(Theme.BACKGROUND);
        p.add(tableScroll(customers), BorderLayout.CENTER);
        JLabel hint = new JLabel("Select a customer row, then choose an account action.");
        hint.setForeground(Theme.MUTED); hint.setFont(Theme.font(Font.PLAIN, 12));
        JPanel actions = actionBar(hint);
        JButton freeze = actionButton("❄  Freeze account", Theme.NAVY_LIGHT);
        JButton activate = actionButton("✓  Activate account", Theme.GREEN);
        JButton close = actionButton("×  Close account", Theme.RED);
        JButton audit = actionButton("▤  View action history", Theme.GOLD); audit.setForeground(Theme.NAVY);
        audit.addActionListener(e -> viewAccountAudit());
        freeze.addActionListener(e -> changeAccount("FROZEN"));
        activate.addActionListener(e -> changeAccount("ACTIVE"));
        close.addActionListener(e -> changeAccount("CLOSED"));
        actions.add(freeze); actions.add(activate); actions.add(close); actions.add(audit);
        p.add(actions, BorderLayout.SOUTH); return p;
    }

    private JPanel loanPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12)); p.setBackground(Theme.BACKGROUND);
        JTable loanTable = tableFor(loans);
        JTable interestTable = new JTable(interestDetails);
        interestTable.setRowHeight(25);
        JPanel loanTop = new JPanel(new BorderLayout(0,5)); loanTop.add(new JLabel("Loan applications"),BorderLayout.NORTH); loanTop.add(new JScrollPane(loanTable),BorderLayout.CENTER);
        JPanel interestBottom = new JPanel(new BorderLayout(0,5)); interestBottom.add(new JLabel("Interest collection by customer (separate from principal)"),BorderLayout.NORTH); interestBottom.add(new JScrollPane(interestTable),BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,loanTop,interestBottom); split.setResizeWeight(.56); split.setDividerLocation(330);
        p.add(split, BorderLayout.CENTER);
        JLabel hint = new JLabel("Review applications above; track interest collected and remaining below.");
        hint.setForeground(Theme.MUTED); hint.setFont(Theme.font(Font.PLAIN, 12));
        JPanel actions = actionBar(hint);
        JButton review = actionButton("▤  Review application", Theme.NAVY_LIGHT);
        JButton approve = actionButton("✓  Approve application", Theme.GREEN);
        JButton reject = actionButton("✕  Decline application", Theme.RED);
        review.addActionListener(e -> reviewLoan());
        approve.addActionListener(e -> decide("APPROVED"));
        reject.addActionListener(e -> decide("REJECTED"));
        actions.add(review); actions.add(approve); actions.add(reject);
        p.add(actions, BorderLayout.SOUTH); return p;
    }

    private JPanel tablePanel(DefaultTableModel model, String description) {
        JPanel p = new JPanel(new BorderLayout(0, 10)); p.setBackground(Theme.BACKGROUND);
        p.add(tableScroll(model), BorderLayout.CENTER);
        JLabel hint = new JLabel(description); hint.setForeground(Theme.MUTED); hint.setFont(Theme.font(Font.PLAIN, 12));
        JPanel actions = actionBar(hint);
        JButton export = actionButton("⇩  Export CSV report", Theme.GOLD); export.setForeground(Theme.NAVY);
        export.addActionListener(e -> exportCsv(model)); actions.add(export); p.add(actions, BorderLayout.SOUTH); return p;
    }

    private JPanel analyticsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 12)); p.setBackground(Theme.BACKGROUND);
        analyticsSummary.setFont(Theme.font(Font.BOLD, 14)); analyticsSummary.setForeground(Theme.NAVY);
        analyticsSummary.setBorder(new EmptyBorder(12, 14, 12, 14)); analyticsSummary.setOpaque(true); analyticsSummary.setBackground(Theme.CREAM);
        JPanel charts = new JPanel(new GridLayout(1,3,12,12)); charts.setOpaque(false);
        charts.add(new PieSummaryPanel("Loan application status", new String[]{"Approved","Pending","Rejected"}, new int[]{0,0,0}));
        charts.add(new PieSummaryPanel("Repayment progress", new String[]{"Fully repaid","Outstanding"}, new int[]{0,0}));
        charts.add(new PieSummaryPanel("Interest collection", new String[]{"Collected","Uncollected"}, new int[]{0,0}));
        JPanel north = new JPanel(new BorderLayout(0,10)); north.setOpaque(false); north.add(analyticsSummary,BorderLayout.NORTH); north.add(charts,BorderLayout.CENTER);
        p.add(north, BorderLayout.NORTH); p.add(tableScroll(monthly), BorderLayout.CENTER);
        JLabel hint = new JLabel("Monthly activity totals. Export this table for your project report.");
        hint.setForeground(Theme.MUTED); hint.setFont(Theme.font(Font.PLAIN, 12));
        JPanel actions = actionBar(hint);
        JButton export = actionButton("⇩  Export analytics CSV", Theme.GOLD); export.setForeground(Theme.NAVY);
        export.addActionListener(e -> exportCsv(monthly)); actions.add(export); p.add(actions, BorderLayout.SOUTH); return p;
    }

    private JPanel supportPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 10)); p.setBackground(Theme.BACKGROUND); p.add(tableScroll(support), BorderLayout.CENTER);
        JLabel hint = new JLabel("Review customer complaints and service requests, then send a reply to the customer inbox.");
        hint.setForeground(Theme.MUTED); hint.setFont(Theme.font(Font.PLAIN, 12));
        JPanel actions = actionBar(hint);
        JButton reply = actionButton("✉  Reply to customer", Theme.NAVY_LIGHT);
        JButton close = actionButton("✓  Resolve request", Theme.GREEN);
        JButton refresh = actionButton("↻  Refresh requests", Theme.GOLD); refresh.setForeground(Theme.NAVY);
        reply.addActionListener(e -> replySupport()); close.addActionListener(e -> closeSupport()); refresh.addActionListener(e -> loadSupport());
        actions.add(reply); actions.add(close); actions.add(refresh); p.add(actions, BorderLayout.SOUTH); return p;
    }

    private JPanel actionBar(JLabel hint) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        bar.setBackground(Theme.CREAM); bar.setBorder(new LineBorder(Theme.BORDER, 1, true));
        bar.add(hint); return bar;
    }

    private JButton actionButton(String text, Color color) {
        JButton b = Ui.button(text, color); b.setFont(Theme.font(Font.BOLD, 12));
        b.setPreferredSize(new Dimension(190, 44)); b.setMinimumSize(new Dimension(160, 44)); return b;
    }

    private void loadInterestTable() {
        try { interestDetails.setRowCount(0); for(Object[] r:dao.loanInterestDetails()) interestDetails.addRow(new Object[]{r[0],r[1],money.format(r[2]),r[3],money.format(r[5]),money.format(r[6]),money.format(r[7]),r[8],r[9]}); }
        catch(Exception ex) { error(ex); }
    }

    private JTable tableFor(DefaultTableModel m) { JTable t=new JTable(m); t.setRowHeight(27); t.setAutoCreateRowSorter(true); return t; }

    private void loadSupport() {
        try { support.setRowCount(0); for (Object[] r : supportDao.all()) support.addRow(r); }
        catch (Exception e) { error(e); }
    }

    private long selectedSupportId() {
        JTable t = findTable(support); int row = t.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Select a customer request first.");
        return ((Number) support.getValueAt(t.convertRowIndexToModel(row), 0)).longValue();
    }

    private void replySupport() {
        try {
            JTable table = findTable(support); int row = table.getSelectedRow(); long id = selectedSupportId();
            int modelRow = table.convertRowIndexToModel(row);
            String subject = String.valueOf(support.getValueAt(modelRow, 3));
            String current = String.valueOf(support.getValueAt(modelRow, 4));
            JTextArea area = new JTextArea(7, 38); area.setLineWrap(true); area.setWrapStyleWord(true);
            area.setText("Thank you for contacting Crestline Bank. ");
            JPanel form = new JPanel(new BorderLayout(0, 8));
            JLabel subjectLabel = new JLabel("Request: " + subject); subjectLabel.setFont(Theme.font(Font.BOLD, 13));
            form.add(subjectLabel, BorderLayout.NORTH);
            JTextArea message = new JTextArea(current); message.setLineWrap(true); message.setWrapStyleWord(true); message.setEditable(false);
            JPanel center = new JPanel(new GridLayout(2, 1, 0, 7)); center.add(new JScrollPane(message)); center.add(new JScrollPane(area));
            form.add(center, BorderLayout.CENTER);
            if (JOptionPane.showConfirmDialog(this, form, "Reply to customer", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            if (area.getText().isBlank()) throw new IllegalArgumentException("Enter a reply before sending.");
            supportDao.reply(id, area.getText().trim()); loadSupport();
            JOptionPane.showMessageDialog(this, "Reply sent to the customer's Crestline inbox.", "Reply sent", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { error(e); }
    }

    private void closeSupport() {
        try { supportDao.close(selectedSupportId()); loadSupport(); }
        catch (Exception e) { error(e); }
    }

    private void refreshLoanCharts() {
        try {
            int[] a=analyticsDao.loanStatusCounts(); int[] b=analyticsDao.repaymentProgressCounts(); java.math.BigDecimal[] c=analyticsDao.interestTotals();
            updatePie("Loan application status",new String[]{"Approved","Pending","Rejected"},new int[]{a[0],a[1],a[2]});
            updatePie("Repayment progress",new String[]{"Fully repaid","Outstanding"},new int[]{b[0],b[1]});
            updatePie("Interest collection",new String[]{"Collected","Uncollected"},new int[]{c[0].intValue(),c[1].intValue()});
        } catch(Exception ignored) { }
    }
    private void updatePie(String title,String[] labels,int[] values) {
        for(Component c:getContentPane().getComponents()) updatePieDeep(c,title,labels,values);
    }
    private void updatePieDeep(Component c,String title,String[] labels,int[] values) {
        if(c instanceof PieSummaryPanel pie && pie.title.equals(title)){pie.labels=labels;pie.values=values;pie.repaint();}
        if(c instanceof Container co) for(Component child:co.getComponents()) updatePieDeep(child,title,labels,values);
    }
    private static class PieSummaryPanel extends JPanel {
        final String title; String[] labels; int[] values;
        PieSummaryPanel(String title,String[] labels,int[] values){this.title=title;this.labels=labels;this.values=values;setBackground(Color.WHITE);setBorder(new LineBorder(Theme.BORDER,1,true));setPreferredSize(new Dimension(220,185));}
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);x.setColor(Theme.TEXT);x.setFont(Theme.font(Font.BOLD,12));x.drawString(title,12,20);int total=0;for(int v:values)total+=Math.max(0,v);Color[] colors={new Color(48,132,104),new Color(224,169,64),new Color(190,83,83)};int start=90;for(int i=0;i<values.length;i++){int arc=total==0?0:(int)Math.round(360.0*Math.max(0,values[i])/total);x.setColor(colors[i%colors.length]);x.fillArc(12,34,112,112,start,arc);start+=arc;}x.setFont(Theme.font(Font.PLAIN,10));int yy=52;for(int i=0;i<labels.length;i++){x.setColor(colors[i%colors.length]);x.fillRect(137,yy-8,8,8);x.setColor(Theme.TEXT);x.drawString(labels[i]+": "+values[i],150,yy);yy+=19;}if(total==0){x.setColor(Theme.MUTED);x.drawString("No records yet",27,160);}x.dispose();}
    }

    private void loadAnalytics() {
        try {
            refreshLoanCharts();
            Object[] t = analyticsDao.totals();
            analyticsSummary.setText("  Deposits: " + money.format(t[0]) + "     Withdrawals: " + money.format(t[1])
                    + "     Transfers: " + money.format(t[2]) + "     Customers: " + t[3]
                    + "     Approved loans: " + t[4] + "     Loan outstanding: " + money.format(t[5]) + "  ");
            monthly.setRowCount(0);
            for (Object[] r : analyticsDao.monthly()) monthly.addRow(new Object[]{r[0], money.format(r[1]), money.format(r[2]), money.format(r[3]), r[4]});
        } catch (Exception e) { analyticsSummary.setText("Analytics unavailable: " + e.getMessage()); }
    }

    private void exportCsv(DefaultTableModel model) {
        JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new java.io.File("crestline-report.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (java.io.PrintWriter out = new java.io.PrintWriter(java.nio.file.Files.newBufferedWriter(
                chooser.getSelectedFile().toPath(), java.nio.charset.StandardCharsets.UTF_8))) {
            for (int c = 0; c < model.getColumnCount(); c++) { if (c > 0) out.print(','); out.print(csv(model.getColumnName(c))); } out.println();
            for (int r = 0; r < model.getRowCount(); r++) { for (int c = 0; c < model.getColumnCount(); c++) { if (c > 0) out.print(','); out.print(csv(String.valueOf(model.getValueAt(r, c)))); } out.println(); }
            JOptionPane.showMessageDialog(this, "Report exported successfully.", "Crestline reports", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) { error(ex); }
    }

    private String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

    private JScrollPane tableScroll(DefaultTableModel model) {
        JTable table = new JTable(model); table.setRowHeight(32); table.setFont(Theme.font(Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setSelectionBackground(new Color(218, 230, 246));
        table.setSelectionForeground(Theme.NAVY); table.setGridColor(new Color(229, 233, 239));
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 13)); table.getTableHeader().setBackground(new Color(239, 242, 247));
        table.getTableHeader().setForeground(Theme.NAVY); table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(new LineBorder(Theme.BORDER)); return scroll;
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
    }

    private void refreshAll() {
        try {
            Object[] m = dao.metrics();
            summary.setText("Customers: " + m[0] + "     Active accounts: " + m[1] + "     Total account balances: "
                    + money.format(m[2]) + "     Transactions: " + m[3] + "     Pending loans: " + m[4]);
            customers.setRowCount(0); for (Object[] row : dao.customers()) customers.addRow(row);
            transactions.setRowCount(0); for (Object[] row : dao.transactions()) transactions.addRow(row);
            repayments.setRowCount(0); for (Object[] row : dao.repayments()) repayments.addRow(new Object[]{row[0], row[1], row[2], money.format(row[3]), row[4]});
            loans.setRowCount(0); for (Loan l : dao.loans()) loans.addRow(new Object[]{l.id(), l.customerId(), l.customerName(), l.accountNumber(),
                    money.format(l.principal()), money.format(l.outstanding()), l.annualRate(), l.tenureMonths(), l.status(), l.createdAt()});
            loadInterestTable(); loadSupport(); loadAnalytics(); loadStaff();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Could not load admin data:\n" + e.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE); }
    }

    private int selectedCustomerRow() {
        JTable table = findTable(customers); int row = table.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Select a customer account from the table first.");
        return table.convertRowIndexToModel(row);
    }

    private void viewAccountAudit() {
        try { int row=selectedCustomerRow(); String account=String.valueOf(customers.getValueAt(row,3)); if(account.equals("null")||account.isBlank())throw new IllegalArgumentException("Selected customer has no account.");
            DefaultTableModel m=model("Action","Reason","Administrator","Date / time"); for(Object[] r:dao.accountAudit(account))m.addRow(r);
            JOptionPane.showMessageDialog(this,tableScroll(m),"Account action history — "+account,JOptionPane.PLAIN_MESSAGE);
        } catch(Exception ex){error(ex);}
    }

    private void changeAccount(String status) {
        try {
            int row = selectedCustomerRow();
            String account = String.valueOf(customers.getValueAt(row, 3));
            String current = String.valueOf(customers.getValueAt(row, 5));
            if (account.equals("null") || account.isBlank()) throw new IllegalArgumentException("This customer has no account to update.");
            if (current.equalsIgnoreCase("CLOSED") && !status.equals("ACTIVE")) throw new IllegalArgumentException("This account is already closed.");
            JTextArea reasonBox = new JTextArea(3, 28); reasonBox.setLineWrap(true); reasonBox.setWrapStyleWord(true);
            JPanel form = new JPanel(new BorderLayout(0, 8));
            form.add(new JLabel("Reason for this action (required):"), BorderLayout.NORTH); form.add(new JScrollPane(reasonBox), BorderLayout.CENTER);
            if (JOptionPane.showConfirmDialog(this, form, "Confirm " + status.toLowerCase() + " account", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            dao.setAccountStatus(account, status, admin.id(), reasonBox.getText()); refreshAll();
        } catch (Exception e) { error(e); }
    }

    private int selectedLoanRow() {
        JTable table = findTable(loans); int row = table.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Select a loan application first.");
        return table.convertRowIndexToModel(row);
    }

    private BigDecimal parseMoney(Object value) {
        String cleaned = String.valueOf(value).replaceAll("[^0-9.\\-]", "");
        return new BigDecimal(cleaned);
    }

    private BigDecimal emi(BigDecimal principal, BigDecimal annualRate, int months) {
        if (annualRate.signum() == 0) return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        double monthlyRate = annualRate.doubleValue() / 1200.0;
        double power = Math.pow(1 + monthlyRate, months);
        return BigDecimal.valueOf(principal.doubleValue() * monthlyRate * power / (power - 1)).setScale(2, RoundingMode.HALF_UP);
    }

    private void reviewLoan() {
        try {
            int row = selectedLoanRow();
            BigDecimal principal = parseMoney(loans.getValueAt(row, 4));
            BigDecimal rate = new BigDecimal(String.valueOf(loans.getValueAt(row, 6)));
            int months = Integer.parseInt(String.valueOf(loans.getValueAt(row, 7)));
            BigDecimal monthly = emi(principal, rate, months);
            BigDecimal total = monthly.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal interest = total.subtract(principal).max(BigDecimal.ZERO);
            String details = "<html><div style='width:360px;padding:8px'>"
                    + "<h2 style='color:#111f36'>Loan application #" + loans.getValueAt(row, 0) + "</h2>"
                    + "<b>Customer:</b> " + loans.getValueAt(row, 2) + "<br>"
                    + "<b>Account:</b> " + loans.getValueAt(row, 3) + "<br>"
                    + "<b>Requested principal:</b> " + money.format(principal) + "<br>"
                    + "<b>Annual interest:</b> " + rate + "%<br><b>Tenure:</b> " + months + " months<hr>"
                    + "<b>Estimated monthly EMI:</b> " + money.format(monthly) + "<br>"
                    + "<b>Total repayment:</b> " + money.format(total) + "<br>"
                    + "<b>Estimated interest:</b> " + money.format(interest) + "<br>"
                    + "<b>Current status:</b> " + loans.getValueAt(row, 8) + "</div></html>";
            JOptionPane.showMessageDialog(this, details, "Review loan terms", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { error(e); }
    }

    private void decide(String decision) {
        try {
            int row = selectedLoanRow(); int id = ((Number) loans.getValueAt(row, 0)).intValue();
            String status = String.valueOf(loans.getValueAt(row, 8));
            if (!"PENDING".equalsIgnoreCase(status)) throw new IllegalArgumentException("Only pending applications can be approved or declined.");
            String customer = String.valueOf(loans.getValueAt(row, 2));
            int confirm = JOptionPane.showConfirmDialog(this, decision + " loan application #" + id + " for " + customer + "?",
                    "Confirm loan decision", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            dao.decideLoan(id, decision); refreshAll();
            JOptionPane.showMessageDialog(this, "Loan application " + decision.toLowerCase() + " successfully.",
                    "Crestline loan desk", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { error(e); }
    }

    private JTable findTable(DefaultTableModel model) {
        JTable found = findTableDeep(getContentPane(), model);
        if (found == null) throw new IllegalStateException("Table unavailable.");
        return found;
    }

    private JTable findTableDeep(Component component, DefaultTableModel model) {
        if (component instanceof JTable table && table.getModel() == model) return table;
        if (component instanceof Container container) for (Component child : container.getComponents()) {
            JTable found = findTableDeep(child, model); if (found != null) return found;
        }
        return null;
    }

    private void error(Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Action failed", JOptionPane.ERROR_MESSAGE);
    }
}
