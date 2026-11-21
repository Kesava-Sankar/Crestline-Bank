package com.crestlinebank.ui;

import com.crestlinebank.model.Customer;
import com.crestlinebank.service.BankingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

public class RegisterDialog extends JDialog {
    private final BankingService service;
    private final JTextField name = Ui.field(22);
    private final JTextField email = Ui.field(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JPasswordField confirm = new JPasswordField(22);

    public RegisterDialog(JFrame owner, BankingService service) {
        super(owner, "Open a Crestline account", true);
        this.service = service;
        setSize(500, 610);
        setLocationRelativeTo(owner);
        buildUi();
    }

    private void buildUi() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(28, 38, 28, 38));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Create your account");
        title.setFont(Theme.font(Font.BOLD, 23));
        title.setForeground(Theme.TEXT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(7));
        JLabel note = new JLabel("Your new account starts with a zero balance.");
        note.setForeground(Theme.MUTED);
        panel.add(note);
        addField(panel, "FULL NAME", name);
        addField(panel, "EMAIL ADDRESS", email);
        addField(panel, "PASSWORD (MINIMUM 8 CHARACTERS)", password);
        addField(panel, "CONFIRM PASSWORD", confirm);
        panel.add(Box.createVerticalStrut(22));
        JButton create = Ui.button("Create account", Theme.NAVY);
        create.setAlignmentX(Component.LEFT_ALIGNMENT);
        create.addActionListener(e -> createAccount());
        panel.add(create);
        setContentPane(panel);
    }

    private void addField(JPanel panel, String label, JComponent field) {
        panel.add(Box.createVerticalStrut(17));
        panel.add(Ui.label(label));
        panel.add(Box.createVerticalStrut(7));
        field.setFont(Theme.font(Font.PLAIN, 14));
        if (field instanceof JPasswordField) field.setBorder(name.getBorder());
        field.setFont(Theme.font(Font.PLAIN, 16));
        field.setPreferredSize(new Dimension(400, 46));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        field.setForeground(Theme.TEXT);
        field.setBackground(Theme.WHITE);
        panel.add(field);
    }

    private void createAccount() {
        String p = new String(password.getPassword());
        String c = new String(confirm.getPassword());
        if (!p.equals(c)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Check password", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Customer customer = service.register(name.getText(), email.getText(), p);
            JOptionPane.showMessageDialog(this,
                    "Account created successfully!\n\nAccount number: " + customer.accountNumber()
                            + "\nPlease keep this number safe.",
                    "Welcome to Crestline", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Could not create account", JOptionPane.ERROR_MESSAGE);
        }
    }
}
