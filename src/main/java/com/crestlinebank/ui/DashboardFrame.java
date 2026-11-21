package com.crestlinebank.ui;

import com.crestlinebank.model.BankTransaction;
import com.crestlinebank.model.Customer;
import com.crestlinebank.service.BankingService;
import com.crestlinebank.dao.CustomerLoanDao;
import com.crestlinebank.dao.SupportDao;
import com.crestlinebank.model.Loan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class DashboardFrame extends JFrame {
    private final Customer customer;
    private final BankingService service = new BankingService();
    private final CustomerLoanDao loanDao = new CustomerLoanDao();
    private final SupportDao supportDao = new SupportDao();
    private final JLabel balanceValue = new JLabel("₹ 0.00");
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Date & time", "Type", "Amount", "Description"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public DashboardFrame(Customer customer) {
        this.customer = customer;
        setTitle("Crestline Bank | Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1120, 760);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        buildUi();
        refreshData();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.NAVY);
        top.setBorder(new EmptyBorder(17, 28, 17, 28));
        JLabel brand = new JLabel("CRESTLINE BANK");
        brand.setForeground(Theme.GOLD);
        brand.setFont(Theme.font(Font.BOLD, 19));
        JLabel user = new JLabel("Welcome, " + customer.fullName());
        user.setForeground(Color.WHITE);
        user.setFont(Theme.font(Font.PLAIN, 13));
        JButton notifications = Ui.button(NotificationDialog.buttonLabel("CUSTOMER", customer.id()), Theme.NAVY_LIGHT);
        notifications.addActionListener(e -> NotificationDialog.show(this, "CUSTOMER", customer.id()));
        JButton logout = Ui.button("Sign out", Theme.NAVY_LIGHT);
        logout.addActionListener(e -> { new LoginFrame().setVisible(true); dispose(); });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false); right.add(user); right.add(notifications); right.add(logout);
        top.add(brand, BorderLayout.WEST); top.add(right, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(25, 28, 25, 28));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Your financial overview");
        title.setFont(Theme.font(Font.BOLD, 24));
        title.setForeground(Theme.TEXT);
        JLabel account = new JLabel("ACCOUNT  " + customer.accountNumber());
        account.setFont(Theme.font(Font.BOLD, 12));
        account.setForeground(Theme.MUTED);
        heading.add(title, BorderLayout.WEST); heading.add(account, BorderLayout.EAST);

        JPanel upper = new JPanel(new BorderLayout(16, 0));
        upper.setOpaque(false);
        JPanel balanceCard = Ui.card();
        balanceCard.setLayout(new BoxLayout(balanceCard, BoxLayout.Y_AXIS));
        JLabel balanceLabel = Ui.label("AVAILABLE BALANCE");
        balanceValue.setFont(Theme.font(Font.BOLD, 32));
        balanceValue.setForeground(Theme.NAVY);
        JLabel balanceHint = new JLabel("Your current account balance");
        balanceHint.setForeground(Theme.MUTED);
        balanceHint.setFont(Theme.font(Font.PLAIN, 12));
        balanceCard.add(balanceLabel); balanceCard.add(Box.createVerticalStrut(13));
        balanceCard.add(balanceValue); balanceCard.add(Box.createVerticalStrut(8)); balanceCard.add(balanceHint);
        balanceCard.add(Box.createVerticalStrut(18));
        balanceCard.add(parentUpiPanel());

        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        actions.setOpaque(false);
        JButton deposit = Ui.button("＋  Deposit", Theme.GREEN);
        JButton withdraw = Ui.button("−  Withdraw", Theme.NAVY_LIGHT);
        JButton transfer = Ui.button("⇄  Transfer", new Color(75, 91, 123));
        JButton refresh = Ui.button("↻  Refresh", Theme.GOLD);
        JButton loans = Ui.button("▤  My loans", new Color(90, 105, 130));
        JButton support = Ui.button("✉  Contact admin", Theme.GOLD);
        JButton crestlinePay = Ui.button("₹  Crestline Pay", new Color(48, 112, 135));
        crestlinePay.addActionListener(e -> familyPayCenter());
        support.setForeground(Theme.NAVY);
        JButton statement = Ui.button("⇩  Export statement", new Color(75, 91, 123));
        refresh.setForeground(Theme.NAVY);
        deposit.addActionListener(e -> amountAction("Deposit", true));
        withdraw.addActionListener(e -> amountAction("Withdraw", false));
        transfer.addActionListener(e -> transferAction());
        refresh.addActionListener(e -> refreshData());
        loans.addActionListener(e -> loanCenter());
        support.addActionListener(e -> supportCenter());
        statement.addActionListener(e -> exportStatement());
        actions.add(deposit); actions.add(withdraw); actions.add(transfer); actions.add(refresh); actions.add(loans); actions.add(crestlinePay); actions.add(support); actions.add(statement);
        upper.add(balanceCard, BorderLayout.CENTER); upper.add(actions, BorderLayout.EAST);

        JPanel transactions = Ui.card();
        transactions.setLayout(new BorderLayout(0, 12));
        JLabel txTitle = new JLabel("Recent transactions");
        txTitle.setFont(Theme.font(Font.BOLD, 17));
        txTitle.setForeground(Theme.TEXT);
        table.setRowHeight(32);
        table.setFont(Theme.font(Font.PLAIN, 12));
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(242, 245, 249));
        table.setFillsViewportHeight(true);
        transactions.add(txTitle, BorderLayout.NORTH);
        transactions.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.add(heading, BorderLayout.NORTH);
        JPanel middle = new JPanel(new BorderLayout(0, 18));
        middle.setOpaque(false);
        middle.add(upper, BorderLayout.NORTH);
        middle.add(transactions, BorderLayout.CENTER);
        center.add(middle, BorderLayout.CENTER);

        status.setForeground(Theme.MUTED);
        status.setFont(Theme.font(Font.PLAIN, 11));
        content.add(center, BorderLayout.CENTER);
        content.add(status, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH); root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel parentUpiPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        JLabel caption = new JLabel("YOUR PARENT UPI ID");
        caption.setFont(Theme.font(Font.BOLD, 10));
        caption.setForeground(Theme.MUTED);
        JLabel upi = new JLabel("Loading UPI ID...");
        upi.setFont(Theme.font(Font.BOLD, 16));
        upi.setForeground(Theme.NAVY);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(caption);
        labels.add(Box.createVerticalStrut(5));
        labels.add(upi);
        JButton copy = Ui.button("Copy", Theme.NAVY_LIGHT);
        copy.setPreferredSize(new Dimension(88, 38));
        try {
            String parentUpi = new com.crestlinebank.dao.FamilyPayDao().parentUpi(customer.id());
            upi.setText(parentUpi);
            copy.addActionListener(e -> {
                try {
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            new java.awt.datatransfer.StringSelection(parentUpi), null);
                    JOptionPane.showMessageDialog(this, "Parent UPI ID copied: " + parentUpi,
                            "Crestline Bank", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Could not access clipboard: " + ex.getMessage(),
                            "Copy failed", JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (Exception ex) {
            upi.setText("UPI ID unavailable");
            copy.setEnabled(false);
            copy.setToolTipText(ex.getMessage());
        }
        panel.add(labels, BorderLayout.CENTER);
        panel.add(copy, BorderLayout.EAST);
        return panel;
    }

    private void refreshData() {
        try {
            balanceValue.setText(money.format(service.balance(customer.id())));
            tableModel.setRowCount(0);
            List<BankTransaction> transactions = service.history(customer.id());
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            for (BankTransaction tx : transactions) {
                tableModel.addRow(new Object[]{tx.createdAt().format(dtf), tx.type(),
                        money.format(tx.amount()), tx.description()});
            }
            status.setText("Showing up to 100 most recent transactions.");
        } catch (Exception ex) {
            status.setText("Could not refresh: " + ex.getMessage());
        }
    }


    private void exportStatement(){
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("crestline-statement-"+customer.accountNumber()+".csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try(java.io.PrintWriter out=new java.io.PrintWriter(java.nio.file.Files.newBufferedWriter(chooser.getSelectedFile().toPath(),java.nio.charset.StandardCharsets.UTF_8))){
            out.println("Date,Type,Amount,Description");for(BankTransaction tx:service.history(customer.id()))out.println(csv(tx.createdAt().toString())+","+csv(tx.type())+","+csv(tx.amount().toPlainString())+","+csv(tx.description()==null?"":tx.description()));
            JOptionPane.showMessageDialog(this,"Account statement exported successfully.","Crestline Bank",JOptionPane.INFORMATION_MESSAGE);
        }catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Export failed",JOptionPane.ERROR_MESSAGE);}
    }
    private String csv(String v){return "\""+v.replace("\"","\"\"")+"\"";}
    private void supportCenter(){
        try{
            DefaultTableModel model=new DefaultTableModel(new Object[]{"ID","Subject","Message","Admin reply","Status","Created"},0){public boolean isCellEditable(int r,int c){return false;}};
            for(Object[] r:supportDao.forCustomer(customer.id()))model.addRow(new Object[]{r[0],r[1],r[2],r[3],r[4],r[5]});
            JTable requests=new JTable(model);requests.setRowHeight(26);JPanel panel=new JPanel(new BorderLayout(0,10));panel.add(new JScrollPane(requests),BorderLayout.CENTER);
            JButton create=Ui.button("＋ New request",Theme.NAVY_LIGHT),refresh=Ui.button("Refresh",Theme.GOLD);refresh.setForeground(Theme.NAVY);JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));actions.add(refresh);actions.add(create);panel.add(actions,BorderLayout.SOUTH);
            refresh.addActionListener(e->{try{model.setRowCount(0);for(Object[] r:supportDao.forCustomer(customer.id()))model.addRow(new Object[]{r[0],r[1],r[2],r[3],r[4],r[5]});}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage());}});
            create.addActionListener(e->{JTextField subject=new JTextField();JTextArea message=new JTextArea(5,28);message.setLineWrap(true);message.setWrapStyleWord(true);JPanel form=new JPanel(new BorderLayout(0,6));JPanel top=new JPanel(new GridLayout(0,1,4,4));top.add(new JLabel("Subject"));top.add(subject);top.add(new JLabel("Message"));form.add(top,BorderLayout.NORTH);form.add(new JScrollPane(message),BorderLayout.CENTER);if(JOptionPane.showConfirmDialog(panel,form,"Contact Crestline support",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{supportDao.create(customer.id(),subject.getText(),message.getText());refresh.doClick();JOptionPane.showMessageDialog(panel,"Your request has been sent to the Crestline admin team.");}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage(),"Request failed",JOptionPane.ERROR_MESSAGE);}});
            JOptionPane.showMessageDialog(this,panel,"Crestline customer support",JOptionPane.PLAIN_MESSAGE);
        }catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Support unavailable",JOptionPane.ERROR_MESSAGE);}
    }
    private BigDecimal calculateEmi(BigDecimal principal,BigDecimal annualRate,int months){
        if(annualRate.signum()==0)return principal.divide(BigDecimal.valueOf(months),2,java.math.RoundingMode.HALF_UP);
        double r=annualRate.doubleValue()/1200.0;double pow=Math.pow(1+r,months);
        return BigDecimal.valueOf(principal.doubleValue()*r*pow/(pow-1)).setScale(2,java.math.RoundingMode.HALF_UP);
    }
    private void loanCenter() {
        try {
            List<Loan> loans = loanDao.findForCustomer(customer.id());
            DefaultTableModel model = new DefaultTableModel(new Object[]{"Loan ID", "Principal", "Outstanding", "Rate %", "Tenure", "Status"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (Loan loan : loans) model.addRow(new Object[]{loan.id(), money.format(loan.principal()),
                    money.format(loan.outstanding()), loan.annualRate(), loan.tenureMonths()+" months", loan.status()});
            JTable loanTable = new JTable(model); loanTable.setRowHeight(28);
            JPanel panel = new JPanel(new BorderLayout(0, 10)); panel.add(new JScrollPane(loanTable), BorderLayout.CENTER);
            panel.add(new JLabel("Loan requests and repayments are managed securely through this demo account."), BorderLayout.NORTH);
            JButton request = Ui.button("Request a loan", Theme.NAVY_LIGHT);
            JButton repay = Ui.button("Make repayment", Theme.GREEN);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.add(request); actions.add(repay); panel.add(actions, BorderLayout.SOUTH);
            request.addActionListener(e -> {
                JTextField amount = new JTextField(), rate = new JTextField("10.5"), months = new JTextField("12");JLabel emiPreview=new JLabel("Estimated monthly EMI: ₹0.00");JButton calculate=Ui.button("Calculate EMI",Theme.GOLD);calculate.setForeground(Theme.NAVY);
                JPanel form = new JPanel(new GridLayout(0,1,5,5)); form.add(new JLabel("Requested amount (INR)")); form.add(amount); form.add(new JLabel("Annual interest rate (%)")); form.add(rate); form.add(new JLabel("Tenure (months)")); form.add(months);form.add(calculate);form.add(emiPreview);
                calculate.addActionListener(ev->{try{BigDecimal principal=new BigDecimal(amount.getText().trim());BigDecimal annual=new BigDecimal(rate.getText().trim());int tenure=Integer.parseInt(months.getText().trim());if(principal.signum()<=0||annual.signum()<0||tenure<1||tenure>360)throw new IllegalArgumentException();BigDecimal emi=calculateEmi(principal,annual,tenure);BigDecimal total=emi.multiply(BigDecimal.valueOf(tenure)).setScale(2,java.math.RoundingMode.HALF_UP);BigDecimal interest=total.subtract(principal).max(BigDecimal.ZERO);emiPreview.setText("<html><b>Estimated monthly EMI:</b> "+money.format(emi)+"<br><b>Total repayment:</b> "+money.format(total)+"<br><b>Estimated interest:</b> "+money.format(interest)+"</html>");}catch(Exception err){emiPreview.setText("Enter valid loan details (1–360 months) to calculate EMI.");}});
                JScrollPane loanScroll = new JScrollPane(form); loanScroll.setPreferredSize(new Dimension(390, 430));
                loanScroll.getVerticalScrollBar().setUnitIncrement(18);
                if (JOptionPane.showConfirmDialog(panel, loanScroll, "Loan request", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
                try { loanDao.request(customer.id(), new BigDecimal(amount.getText().trim()), new BigDecimal(rate.getText().trim()), Integer.parseInt(months.getText().trim())); JOptionPane.showMessageDialog(panel, "Loan request submitted for admin review. "+emiPreview.getText()); ((Window) SwingUtilities.getWindowAncestor(panel)).dispose(); loanCenter(); }
                catch (Exception ex) { JOptionPane.showMessageDialog(panel, ex.getMessage(), "Loan request failed", JOptionPane.ERROR_MESSAGE); }
            });
            repay.addActionListener(e -> {
                int row = loanTable.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(panel, "Select an approved loan first."); return; }
                int loanId = ((Number) model.getValueAt(loanTable.convertRowIndexToModel(row), 0)).intValue();
                if (!"APPROVED".equals(model.getValueAt(row, 5))) { JOptionPane.showMessageDialog(panel, "Only approved loans can receive repayments."); return; }
                String input = JOptionPane.showInputDialog(panel, "Repayment amount (INR):"); if (input == null) return;
                try { loanDao.repay(customer.id(), loanId, new BigDecimal(input.trim())); JOptionPane.showMessageDialog(panel, "Repayment completed."); refreshData(); ((Window) SwingUtilities.getWindowAncestor(panel)).dispose(); }
                catch (Exception ex) { JOptionPane.showMessageDialog(panel, ex.getMessage(), "Repayment failed", JOptionPane.ERROR_MESSAGE); }
            });
            JOptionPane.showMessageDialog(this, panel, "My loans", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Could not load loans", JOptionPane.ERROR_MESSAGE); }
    }


    private void familyPayCenter() {
        try {
            com.crestlinebank.dao.FamilyPayDao dao = new com.crestlinebank.dao.FamilyPayDao();
            DefaultTableModel model = new DefaultTableModel(new Object[]{"ID","Child","Mobile","UPI ID","Daily limit","Per payment","Status"},0) {
                @Override public boolean isCellEditable(int r,int c){return false;}
            };
            Runnable reload = () -> { try { model.setRowCount(0); for(Object[] r:dao.children(customer.id())) model.addRow(r); } catch(Exception ex){throw new RuntimeException(ex);} };
            reload.run(); JTable table = new JTable(model); table.setRowHeight(27);
            JPanel panel = new JPanel(new BorderLayout(0,10)); panel.setPreferredSize(new Dimension(650,360));
            panel.add(new JLabel("Parent UPI ID: "+dao.parentUpi(customer.id())+"   |   Child identities are delegated profiles — simulated demo only"),BorderLayout.NORTH);
            panel.add(new JScrollPane(table),BorderLayout.CENTER);
            JButton add=Ui.button("＋ Add child UPI",Theme.NAVY_LIGHT), credentials=Ui.button("Set child login",Theme.NAVY_LIGHT), limit=Ui.button("Set spending limits",Theme.GOLD), toggle=Ui.button("Block / reactivate",Theme.RED), pay=Ui.button("Demo payment",Theme.GREEN), emergency=Ui.button("Emergency request",new Color(75,91,123)),history=Ui.button("UPI history",new Color(90,105,130)),blockMerchant=Ui.button("Block game / website",Theme.RED),unblockMerchant=Ui.button("Unblock merchant",new Color(110,125,145));
            limit.setForeground(Theme.NAVY); JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.add(add);actions.add(credentials);actions.add(limit);actions.add(toggle);actions.add(pay);actions.add(emergency);actions.add(history);actions.add(blockMerchant);actions.add(unblockMerchant);panel.add(actions,BorderLayout.SOUTH);
            add.addActionListener(e->{JTextField child=new JTextField(),mobile=new JTextField(),upi=new JTextField(),daily=new JTextField("2000"),per=new JTextField("1000");JPasswordField childPassword=new JPasswordField();JPanel f=new JPanel(new GridLayout(0,1,4,4));for(Object[] pair:new Object[][]{{"Child name",child},{"Child mobile number",mobile},{"Child UPI ID",upi},{"Child login password",childPassword},{"Daily cap (₹)",daily},{"Per-payment cap (₹)",per}}){f.add(new JLabel((String)pair[0]));f.add((JComponent)pair[1]);}if(JOptionPane.showConfirmDialog(panel,f,"Create child UPI identity",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{dao.addChild(customer.id(),child.getText(),mobile.getText(),upi.getText(),com.crestlinebank.util.PasswordUtil.hash(new String(childPassword.getPassword())),new BigDecimal(daily.getText()),new BigDecimal(per.getText()));reload.run();}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage());}});
            credentials.addActionListener(e->{int r=table.getSelectedRow();if(r<0){JOptionPane.showMessageDialog(panel,"Select a child first.");return;}long id=((Number)model.getValueAt(table.convertRowIndexToModel(r),0)).longValue();JPasswordField pw=new JPasswordField();if(JOptionPane.showConfirmDialog(panel,new Object[]{new JLabel("New child login password"),pw},"Set child login",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{dao.setChildPassword(customer.id(),id,com.crestlinebank.util.PasswordUtil.hash(new String(pw.getPassword())));JOptionPane.showMessageDialog(panel,"Child login credentials updated. The child can now sign in using their UPI ID.");}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage());}});
            limit.addActionListener(e->{int r=table.getSelectedRow();if(r<0){JOptionPane.showMessageDialog(panel,"Select a child first.");return;}long id=((Number)model.getValueAt(table.convertRowIndexToModel(r),0)).longValue();String d=JOptionPane.showInputDialog(panel,"New daily cap (₹):",model.getValueAt(table.convertRowIndexToModel(r),4));if(d==null)return;String t=JOptionPane.showInputDialog(panel,"New per-payment cap (₹):",model.getValueAt(table.convertRowIndexToModel(r),5));if(t==null)return;try{dao.setLimits(customer.id(),id,new BigDecimal(d),new BigDecimal(t));reload.run();}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage());}});
            toggle.addActionListener(e->{int r=table.getSelectedRow();if(r<0){JOptionPane.showMessageDialog(panel,"Select a child first.");return;}int mr=table.convertRowIndexToModel(r);long id=((Number)model.getValueAt(mr,0)).longValue();String state=String.valueOf(model.getValueAt(mr,6));try{dao.setStatus(customer.id(),id,"ACTIVE".equals(state)?"BLOCKED":"ACTIVE");reload.run();}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage());}});
            pay.addActionListener(e->{int r=table.getSelectedRow();if(r<0){JOptionPane.showMessageDialog(panel,"Select a child first.");return;}int mr=table.convertRowIndexToModel(r);long id=((Number)model.getValueAt(mr,0)).longValue();JTextField recipient=new JTextField(),amount=new JTextField(),note=new JTextField("Daily expense");JPanel f=new JPanel(new GridLayout(0,1,4,4));f.add(new JLabel("Recipient / UPI ID"));f.add(recipient);f.add(new JLabel("Amount (₹)"));f.add(amount);f.add(new JLabel("Purpose"));f.add(note);if(JOptionPane.showConfirmDialog(panel,f,"Simulate child UPI payment",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{dao.demoPayment(customer.id(),id,recipient.getText(),new BigDecimal(amount.getText()),note.getText());JOptionPane.showMessageDialog(panel,"Demo payment recorded. No real money was moved.");}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage(),"Payment blocked",JOptionPane.WARNING_MESSAGE);}});
            blockMerchant.addActionListener(e->{int r=table.getSelectedRow();if(r<0){JOptionPane.showMessageDialog(panel,"Select the child whose spending you want to control.");return;}long id=((Number)model.getValueAt(table.convertRowIndexToModel(r),0)).longValue();String merchant=JOptionPane.showInputDialog(panel,"Exact game / website / merchant name or UPI ID to block for this child:");if(merchant==null)return;try{dao.setMerchantBlocked(id,merchant,true);JOptionPane.showMessageDialog(panel,"Only this merchant is blocked for the selected child. Other payments remain available within normal limits.");}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage());}});
            unblockMerchant.addActionListener(e->{int r=table.getSelectedRow();if(r<0){JOptionPane.showMessageDialog(panel,"Select the child first.");return;}long id=((Number)model.getValueAt(table.convertRowIndexToModel(r),0)).longValue();String merchant=JOptionPane.showInputDialog(panel,"Exact merchant name / UPI ID to unblock:");if(merchant==null)return;try{dao.setMerchantBlocked(id,merchant,false);JOptionPane.showMessageDialog(panel,"Merchant unblocked for this child.");}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage());}});
            history.addActionListener(e->{try{DefaultTableModel hm=new DefaultTableModel(new Object[]{"Child","Recipient","Amount","Status","Purpose / result","Date"},0){public boolean isCellEditable(int r,int c){return false;}};for(Object[] x:dao.transactions(customer.id()))hm.addRow(x);JTable ht=new JTable(hm);ht.setRowHeight(25);JOptionPane.showMessageDialog(panel,new JScrollPane(ht),"Family UPI transaction history",JOptionPane.PLAIN_MESSAGE);}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage());}});
            emergency.addActionListener(e->{int r=table.getSelectedRow();if(r<0){JOptionPane.showMessageDialog(panel,"Select a child first.");return;}long id=((Number)model.getValueAt(table.convertRowIndexToModel(r),0)).longValue();String a=JOptionPane.showInputDialog(panel,"Emergency amount (₹):");if(a==null)return;String why=JOptionPane.showInputDialog(panel,"Reason:");if(why==null)return;try{dao.requestEmergency(customer.id(),id,new BigDecimal(a),why);int ok=JOptionPane.showConfirmDialog(panel,"Approve this one-time demo exception? The regular daily cap will remain unchanged.","Parent approval",JOptionPane.YES_NO_OPTION);if(ok==JOptionPane.YES_OPTION)dao.approveLatestRequest(customer.id(),id);JOptionPane.showMessageDialog(panel,"Emergency request recorded.");}catch(Exception ex){JOptionPane.showMessageDialog(panel,ex.getMessage());}});
            JOptionPane.showMessageDialog(this,panel,"Crestline Pay | Family controls",JOptionPane.PLAIN_MESSAGE);
        } catch(Exception ex) { JOptionPane.showMessageDialog(this,ex.getMessage(),"Crestline Pay unavailable",JOptionPane.ERROR_MESSAGE); }
    }

    private void amountAction(String title, boolean isDeposit) {
        String input = JOptionPane.showInputDialog(this, "Enter amount in INR:", title, JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;
        try {
            BigDecimal amount = new BigDecimal(input.trim());
            if (isDeposit) service.deposit(customer.id(), amount);
            else service.withdraw(customer.id(), amount);
            JOptionPane.showMessageDialog(this, title + " completed successfully.", "Crestline Bank", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), title + " failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void transferAction() {
        JTextField account = new JTextField();
        JTextField amount = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Recipient account number"));
        panel.add(account);
        panel.add(new JLabel("Amount (INR)"));
        panel.add(amount);
        if (JOptionPane.showConfirmDialog(this, panel, "Transfer money",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            service.transfer(customer.id(), account.getText(), new BigDecimal(amount.getText().trim()));
            JOptionPane.showMessageDialog(this, "Transfer completed successfully.", "Crestline Bank", JOptionPane.INFORMATION_MESSAGE);
            refreshData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Transfer failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
